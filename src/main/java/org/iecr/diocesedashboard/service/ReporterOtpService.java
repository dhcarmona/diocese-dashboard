package org.iecr.diocesedashboard.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * Manages one-time passcodes (OTPs) for reporter user authentication.
 *
 * <p>OTPs are 6-digit codes valid for {@value #OTP_TTL_SECONDS} seconds.
 * They are stored in memory and consumed on first successful verification.
 */
@Service
public class ReporterOtpService {

  static final long OTP_TTL_SECONDS = 600;
  static final long REQUEST_THROTTLE_SECONDS = 60;
  static final int MAX_OTP_REQUEST_ATTEMPTS = 3;
  static final long REQUEST_ATTEMPT_WINDOW_SECONDS = OTP_TTL_SECONDS;
  static final long REQUEST_LOCKOUT_SECONDS = OTP_TTL_SECONDS;
  static final long VERIFY_THROTTLE_SECONDS = 5;
  static final int MAX_FAILED_VERIFY_ATTEMPTS = 5;
  static final long VERIFY_ATTEMPT_WINDOW_SECONDS = OTP_TTL_SECONDS;
  static final long VERIFY_LOCKOUT_SECONDS = OTP_TTL_SECONDS;
  private static final int OTP_DIGITS = 6;
  private static final int OTP_MODULUS = 1_000_000;
  private static final long MAX_TRACKED_USERNAMES = 10_000;

  private final WhatsAppService whatsAppService;
  private final UserService userService;
  private final MessageSource messageSource;
  private final Clock clock;
  private final Ticker ticker;
  private final SecureRandom secureRandom = new SecureRandom();
  private final Cache<String, OtpEntry> otpStore;
  private final Cache<String, RequestAttemptState> requestAttemptStore;
  private final Cache<String, VerifyAttemptState> verifyAttemptStore;

  @Autowired
  public ReporterOtpService(
      WhatsAppService whatsAppService, UserService userService, MessageSource messageSource) {
    this(
        whatsAppService,
        userService,
        messageSource,
        Clock.systemUTC(),
        Ticker.systemTicker());
  }

  ReporterOtpService(
      WhatsAppService whatsAppService,
      UserService userService,
      MessageSource messageSource,
      Clock clock) {
    this(whatsAppService, userService, messageSource, clock, Ticker.systemTicker());
  }

  ReporterOtpService(
      WhatsAppService whatsAppService,
      UserService userService,
      MessageSource messageSource,
      Clock clock,
      Ticker ticker) {
    this.whatsAppService = whatsAppService;
    this.userService = userService;
    this.messageSource = messageSource;
    this.clock = clock;
    this.ticker = ticker;
    this.otpStore = buildCache(Duration.ofSeconds(OTP_TTL_SECONDS));
    this.requestAttemptStore = buildCache(Duration.ofSeconds(REQUEST_LOCKOUT_SECONDS));
    this.verifyAttemptStore = buildCache(Duration.ofSeconds(VERIFY_LOCKOUT_SECONDS));
  }

  /**
   * Generates an OTP for the given reporter username and sends it via WhatsApp.
   * Any previously stored (possibly expired) OTP for this username is replaced.
   * Expired entries for other users are evicted opportunistically on each call.
   *
   * @param username the reporter's username
   * @throws IllegalArgumentException if no enabled reporter with that username exists,
   *                                  or if the reporter has no phone number registered
   */
  public void generateAndSendOtp(String username) {
    if (!hasText(username)) {
      throw new IllegalArgumentException("Username must not be blank.");
    }
    evictExpiredEntries();
    evictExpiredRequestAttempts();
    Instant now = Instant.now(clock);
    RequestAttemptState existingRequestState = requestAttemptStore.getIfPresent(username);
    if (existingRequestState != null && now.isBefore(existingRequestState.blockedUntil())) {
      return;
    }
    if (existingRequestState != null
        && now.isBefore(existingRequestState.nextAllowedAttemptAt())) {
      return;
    }
    RequestAttemptState updatedRequestState = recordOtpRequestAttempt(username, now);
    if (updatedRequestState.requestAttempts() > MAX_OTP_REQUEST_ATTEMPTS) {
      return;
    }

    DashboardUser user = userService.findByUsername(username)
        .filter(DashboardUser::isEnabled)
        .filter(uu -> uu.getRole() == UserRole.REPORTER)
        .orElseThrow(() -> new IllegalArgumentException(
            "No active reporter found for username: " + username));

    String phoneNumber = user.getPhoneNumber();
    if (phoneNumber == null || phoneNumber.isBlank()) {
      throw new IllegalArgumentException(
          "Reporter does not have a valid phone number for username: " + username);
    }

    String code = generateCode();
    otpStore.put(username, new OtpEntry(code, Instant.now(clock).plusSeconds(OTP_TTL_SECONDS)));
    String body = messageSource.getMessage(
        "otp.whatsapp.message",
        new Object[]{code},
        "Tu código de verificación es " + code + ".",
        user.getPreferredLocale());
    whatsAppService.sendOtpAndLog(phoneNumber, body, code, username, user.getPreferredLocale());
  }

  /**
   * Verifies the OTP for the given username and consumes it if valid.
   *
   * @param username the reporter's username
   * @param code     the OTP provided by the user
   * @return the verification result, including rate-limit information when applicable
   */
  public OtpVerificationResult verifyAndConsumeOtp(String username, String code) {
    if (!hasText(username) || !hasText(code)) {
      return OtpVerificationResult.invalid();
    }
    evictExpiredVerifyAttempts();
    Instant now = Instant.now(clock);
    VerifyAttemptState attemptState = verifyAttemptStore.getIfPresent(username);
    if (attemptState != null && now.isBefore(attemptState.blockedUntil())) {
      return OtpVerificationResult.attemptLimitExceeded(
          secondsUntil(now, attemptState.blockedUntil()));
    }
    if (attemptState != null && now.isBefore(attemptState.nextAllowedAttemptAt())) {
      return OtpVerificationResult.throttled(
          secondsUntil(now, attemptState.nextAllowedAttemptAt()));
    }

    OtpEntry entry = otpStore.getIfPresent(username);
    if (entry == null) {
      return recordFailedVerifyAttempt(username, now);
    }
    if (now.isAfter(entry.expiry())) {
      otpStore.invalidate(username);
      return recordFailedVerifyAttempt(username, now);
    }
    if (!entry.code().equals(code)) {
      return recordFailedVerifyAttempt(username, now);
    }
    otpStore.invalidate(username);
    verifyAttemptStore.invalidate(username);
    return OtpVerificationResult.success();
  }

  private void evictExpiredEntries() {
    otpStore.cleanUp();
  }

  private void evictExpiredVerifyAttempts() {
    verifyAttemptStore.cleanUp();
  }

  private void evictExpiredRequestAttempts() {
    requestAttemptStore.cleanUp();
  }

  private RequestAttemptState recordOtpRequestAttempt(String username, Instant now) {
    return requestAttemptStore.asMap().compute(
        username,
        (ignored, existing) -> {
          RequestAttemptState currentState = existing;
          if (currentState != null && isExpired(currentState, now)) {
            currentState = null;
          }

          int requestAttempts = currentState == null ? 1 : currentState.requestAttempts() + 1;
          Instant blockedUntil = requestAttempts > MAX_OTP_REQUEST_ATTEMPTS
              ? now.plusSeconds(REQUEST_LOCKOUT_SECONDS) : now;
          return new RequestAttemptState(
              requestAttempts,
              now.plusSeconds(REQUEST_THROTTLE_SECONDS),
              blockedUntil,
              now.plusSeconds(REQUEST_ATTEMPT_WINDOW_SECONDS));
        });
  }

  private OtpVerificationResult recordFailedVerifyAttempt(String username, Instant now) {
    VerifyAttemptState updatedState = verifyAttemptStore.asMap().compute(
        username,
        (ignored, existing) -> {
          VerifyAttemptState currentState = existing;
          if (currentState != null && isExpired(currentState, now)) {
            currentState = null;
          }

          int failedAttempts = currentState == null ? 1 : currentState.failedAttempts() + 1;
          Instant blockedUntil = failedAttempts >= MAX_FAILED_VERIFY_ATTEMPTS
              ? now.plusSeconds(VERIFY_LOCKOUT_SECONDS) : now;
          return new VerifyAttemptState(
              failedAttempts,
              now.plusSeconds(VERIFY_THROTTLE_SECONDS),
              blockedUntil,
              now.plusSeconds(VERIFY_ATTEMPT_WINDOW_SECONDS));
        });

    if (updatedState.failedAttempts() >= MAX_FAILED_VERIFY_ATTEMPTS) {
      otpStore.invalidate(username);
      return OtpVerificationResult.attemptLimitExceeded(
          secondsUntil(now, updatedState.blockedUntil()));
    }
    return OtpVerificationResult.invalid();
  }

  private boolean isExpired(VerifyAttemptState state, Instant now) {
    return !now.isBefore(state.attemptWindowEndsAt()) && !now.isBefore(state.blockedUntil());
  }

  private boolean isExpired(RequestAttemptState state, Instant now) {
    return !now.isBefore(state.attemptWindowEndsAt()) && !now.isBefore(state.blockedUntil());
  }

  private long secondsUntil(Instant now, Instant target) {
    if (!target.isAfter(now)) {
      return 1L;
    }
    Duration duration = Duration.between(now, target);
    long seconds = duration.getSeconds();
    if (duration.getNano() > 0) {
      seconds++;
    }
    return Math.max(1L, seconds);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private <T> Cache<String, T> buildCache(Duration expiryDuration) {
    return Caffeine.newBuilder()
        .maximumSize(MAX_TRACKED_USERNAMES)
        .expireAfterWrite(expiryDuration)
        .ticker(ticker)
        .build();
  }

  private String generateCode() {
    int code = secureRandom.nextInt(OTP_MODULUS);
    return String.format("%0" + OTP_DIGITS + "d", code);
  }

  /** Result of an OTP verification attempt. */
  public record OtpVerificationResult(OtpVerificationOutcome outcome, long retryAfterSeconds) {

    public boolean isSuccess() {
      return outcome == OtpVerificationOutcome.SUCCESS;
    }

    public boolean isThrottled() {
      return outcome == OtpVerificationOutcome.THROTTLED;
    }

    public boolean isAttemptLimitExceeded() {
      return outcome == OtpVerificationOutcome.ATTEMPT_LIMIT_EXCEEDED;
    }

    public static OtpVerificationResult success() {
      return new OtpVerificationResult(OtpVerificationOutcome.SUCCESS, 0);
    }

    public static OtpVerificationResult invalid() {
      return new OtpVerificationResult(OtpVerificationOutcome.INVALID, 0);
    }

    public static OtpVerificationResult throttled(long retryAfterSeconds) {
      return new OtpVerificationResult(OtpVerificationOutcome.THROTTLED, retryAfterSeconds);
    }

    public static OtpVerificationResult attemptLimitExceeded(long retryAfterSeconds) {
      return new OtpVerificationResult(
          OtpVerificationOutcome.ATTEMPT_LIMIT_EXCEEDED, retryAfterSeconds);
    }
  }

  /** Supported OTP verification outcomes. */
  public enum OtpVerificationOutcome {
    SUCCESS,
    INVALID,
    THROTTLED,
    ATTEMPT_LIMIT_EXCEEDED
  }

  /** Immutable OTP entry holding the code and its expiry instant. */
  record OtpEntry(String code, Instant expiry) {
  }

  /** Tracks failed verification attempts and retry timing for a username. */
  record VerifyAttemptState(
  int failedAttempts,
  Instant nextAllowedAttemptAt,
  Instant blockedUntil,
  Instant attemptWindowEndsAt) {
  }

  /** Tracks OTP request timing for a username. */
  record RequestAttemptState(
  int requestAttempts,
  Instant nextAllowedAttemptAt,
  Instant blockedUntil,
  Instant attemptWindowEndsAt) {
  }
}
