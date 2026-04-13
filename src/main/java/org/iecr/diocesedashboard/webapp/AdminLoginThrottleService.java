package org.iecr.diocesedashboard.webapp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/** Tracks failed admin login attempts and enforces throttling plus temporary lockouts. */
class AdminLoginThrottleService {

  static final long LOGIN_THROTTLE_SECONDS = 5;
  static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
  static final long LOGIN_ATTEMPT_WINDOW_SECONDS = 600;
  static final long LOGIN_LOCKOUT_SECONDS = 600;
  private static final long MAX_TRACKED_USERNAMES = 10_000;

  private final Clock clock;
  private final Cache<String, LoginAttemptState> attemptStore;

  AdminLoginThrottleService(Clock clock) {
    this(clock, Ticker.systemTicker());
  }

  AdminLoginThrottleService(Clock clock, Ticker ticker) {
    this.clock = clock;
    this.attemptStore = Caffeine.newBuilder()
        .maximumSize(MAX_TRACKED_USERNAMES)
        .expireAfterWrite(Duration.ofSeconds(LOGIN_LOCKOUT_SECONDS))
        .ticker(ticker)
        .build();
  }

  /**
   * Returns whether a login attempt should be allowed for the given username.
   *
   * @param username the username being authenticated
   * @return the current throttle or lockout state for that username
   */
  LoginAttemptResult checkAttemptAllowed(String username) {
    if (username == null || username.isBlank()) {
      return LoginAttemptResult.allowed();
    }
    evictExpiredAttempts();
    Instant now = Instant.now(clock);
    LoginAttemptState state = attemptStore.getIfPresent(username);
    if (state != null && now.isBefore(state.blockedUntil())) {
      return LoginAttemptResult.attemptLimitExceeded(secondsUntil(now, state.blockedUntil()));
    }
    if (state != null && now.isBefore(state.nextAllowedAttemptAt())) {
      return LoginAttemptResult.throttled(secondsUntil(now, state.nextAllowedAttemptAt()));
    }
    return LoginAttemptResult.allowed();
  }

  /**
   * Records a failed login attempt for the given username.
   *
   * @param username the username that failed authentication
   * @return the updated throttle or lockout state after recording the failure
   */
  LoginAttemptResult recordFailedAttempt(String username) {
    if (username == null || username.isBlank()) {
      return LoginAttemptResult.allowed();
    }
    evictExpiredAttempts();
    Instant now = Instant.now(clock);
    LoginAttemptState updatedState = attemptStore.asMap().compute(
        username,
        (ignored, existing) -> {
          LoginAttemptState currentState = existing;
          if (currentState != null && isExpired(currentState, now)) {
            currentState = null;
          }

          int failedAttempts = currentState == null ? 1 : currentState.failedAttempts() + 1;
          Instant blockedUntil = failedAttempts >= MAX_FAILED_LOGIN_ATTEMPTS
              ? now.plusSeconds(LOGIN_LOCKOUT_SECONDS) : now;
          return new LoginAttemptState(
              failedAttempts,
              now.plusSeconds(LOGIN_THROTTLE_SECONDS),
              blockedUntil,
              now.plusSeconds(LOGIN_ATTEMPT_WINDOW_SECONDS));
        });

    if (updatedState.failedAttempts() >= MAX_FAILED_LOGIN_ATTEMPTS) {
      return LoginAttemptResult.attemptLimitExceeded(
          secondsUntil(now, updatedState.blockedUntil()));
    }
    return LoginAttemptResult.allowed();
  }

  void clearFailedAttempts(String username) {
    if (username == null || username.isBlank()) {
      return;
    }
    attemptStore.invalidate(username);
  }

  void clearAllAttempts() {
    attemptStore.invalidateAll();
  }

  private void evictExpiredAttempts() {
    attemptStore.cleanUp();
  }

  private boolean isExpired(LoginAttemptState state, Instant now) {
    return !now.isBefore(state.attemptWindowEndsAt()) && !now.isBefore(state.blockedUntil());
  }

  private long secondsUntil(Instant now, Instant target) {
    return Math.max(1L, Duration.between(now, target).toSeconds());
  }

  /** Result of checking or recording a login attempt. */
  record LoginAttemptResult(LoginAttemptOutcome outcome, long retryAfterSeconds) {

    boolean isAllowed() {
      return outcome == LoginAttemptOutcome.ALLOWED;
    }

    boolean isThrottled() {
      return outcome == LoginAttemptOutcome.THROTTLED;
    }

    boolean isAttemptLimitExceeded() {
      return outcome == LoginAttemptOutcome.ATTEMPT_LIMIT_EXCEEDED;
    }

    static LoginAttemptResult allowed() {
      return new LoginAttemptResult(LoginAttemptOutcome.ALLOWED, 0);
    }

    static LoginAttemptResult throttled(long retryAfterSeconds) {
      return new LoginAttemptResult(LoginAttemptOutcome.THROTTLED, retryAfterSeconds);
    }

    static LoginAttemptResult attemptLimitExceeded(long retryAfterSeconds) {
      return new LoginAttemptResult(LoginAttemptOutcome.ATTEMPT_LIMIT_EXCEEDED, retryAfterSeconds);
    }
  }

  /** Supported states for an admin login attempt. */
  enum LoginAttemptOutcome {
    ALLOWED,
    THROTTLED,
    ATTEMPT_LIMIT_EXCEEDED
  }

  /** Tracks failed login attempt counts and retry timing for a username. */
  record LoginAttemptState(
      int failedAttempts,
      Instant nextAllowedAttemptAt,
      Instant blockedUntil,
      Instant attemptWindowEndsAt) {
  }
}
