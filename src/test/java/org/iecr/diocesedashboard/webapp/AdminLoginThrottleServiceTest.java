package org.iecr.diocesedashboard.webapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.time.ZoneId;

class AdminLoginThrottleServiceTest {

  private AdjustableClock clock;
  private AdjustableTicker ticker;
  private AdminLoginThrottleService throttleService;

  @BeforeEach
  void setUp() {
    clock = new AdjustableClock(Instant.parse("2026-04-13T12:00:00Z"), ZoneId.of("UTC"));
    ticker = new AdjustableTicker();
    throttleService = new AdminLoginThrottleService(clock, ticker);
  }

  @Test
  void checkAttemptAllowed_freshUsername_isAllowed() {
    assertThat(throttleService.checkAttemptAllowed("admin").isAllowed()).isTrue();
  }

  @Test
  void checkAttemptAllowed_afterFailure_isThrottled() {
    throttleService.recordFailedAttempt("admin");

    AdminLoginThrottleService.LoginAttemptResult result =
        throttleService.checkAttemptAllowed("admin");

    assertThat(result.isThrottled()).isTrue();
    assertThat(result.retryAfterSeconds())
        .isEqualTo(AdminLoginThrottleService.LOGIN_THROTTLE_SECONDS);
  }

  @Test
  void checkAttemptAllowed_afterTooManyFailures_isLockedOut() {
    for (int ii = 0; ii < AdminLoginThrottleService.MAX_FAILED_LOGIN_ATTEMPTS; ii++) {
      AdminLoginThrottleService.LoginAttemptResult result =
        throttleService.recordFailedAttempt("admin");
      if (ii < AdminLoginThrottleService.MAX_FAILED_LOGIN_ATTEMPTS - 1) {
        assertThat(result.isAllowed()).isTrue();
        advanceSeconds(AdminLoginThrottleService.LOGIN_THROTTLE_SECONDS);
      } else {
        assertThat(result.isAttemptLimitExceeded()).isTrue();
        assertThat(result.retryAfterSeconds())
            .isEqualTo(AdminLoginThrottleService.LOGIN_LOCKOUT_SECONDS);
      }
    }

    AdminLoginThrottleService.LoginAttemptResult result =
        throttleService.checkAttemptAllowed("admin");
    assertThat(result.isAttemptLimitExceeded()).isTrue();
  }

  @Test
  void clearFailedAttempts_afterFailure_allowsLoginAgain() {
    throttleService.recordFailedAttempt("admin");

    throttleService.clearFailedAttempts("admin");

    assertThat(throttleService.checkAttemptAllowed("admin").isAllowed()).isTrue();
  }

  private void advanceSeconds(long seconds) {
    clock.advanceSeconds(seconds);
    ticker.advanceSeconds(seconds);
  }

  private static final class AdjustableClock extends Clock {

    private Instant currentInstant;
    private final ZoneId zoneId;

    private AdjustableClock(Instant currentInstant, ZoneId zoneId) {
      this.currentInstant = currentInstant;
      this.zoneId = zoneId;
    }

    @Override
    public ZoneId getZone() {
      return zoneId;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return new AdjustableClock(currentInstant, zone);
    }

    @Override
    public Instant instant() {
      return currentInstant;
    }

    private void advanceSeconds(long seconds) {
      currentInstant = currentInstant.plusSeconds(seconds);
    }
  }

  private static final class AdjustableTicker implements Ticker {

    private long nanos;

    @Override
    public long read() {
      return nanos;
    }

    private void advanceSeconds(long seconds) {
      nanos += TimeUnit.SECONDS.toNanos(seconds);
    }
  }
}
