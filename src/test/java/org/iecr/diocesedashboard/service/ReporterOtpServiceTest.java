package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.benmanes.caffeine.cache.Ticker;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class ReporterOtpServiceTest {

  @Mock
  private WhatsAppService whatsAppService;

  @Mock
  private UserService userService;

  private AdjustableClock clock;
  private AdjustableTicker ticker;
  private ReporterOtpService reporterOtpService;

  private DashboardUser buildReporter(String username) {
    DashboardUser user = new DashboardUser();
    user.setId(1L);
    user.setUsername(username);
    user.setRole(UserRole.REPORTER);
    user.setPhoneNumber("+50688888888");
    user.setFullName("Test Reporter");
    user.setEnabled(true);
    return user;
  }

  @BeforeEach
  void setUp() {
    ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
    messageSource.setBasename("messages");
    messageSource.setDefaultEncoding("UTF-8");
    clock = new AdjustableClock(Instant.parse("2026-04-13T12:00:00Z"), ZoneId.of("UTC"));
    ticker = new AdjustableTicker();
    reporterOtpService =
        new ReporterOtpService(whatsAppService, userService, messageSource, clock, ticker);
  }

  @Test
  void generateAndSendOtp_sendsWhatsAppMessage() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    reporterOtpService.generateAndSendOtp("rep1");

    verify(whatsAppService)
        .sendOtpAndLog(eq("+50688888888"), contains("Diocese Dashboard"), any(), any());
  }

  @Test
  void generateAndSendOtp_usesPreferredLanguageTemplate() {
    DashboardUser reporter = buildReporter("rep1");
    reporter.setPreferredLanguage("en");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    reporterOtpService.generateAndSendOtp("rep1");

    verify(whatsAppService)
        .sendOtpAndLog(eq("+50688888888"), contains("login code"), any(), any());
  }

  @Test
  void generateAndSendOtp_immediateRetry_isThrottled() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    reporterOtpService.generateAndSendOtp("rep1");
    reporterOtpService.generateAndSendOtp("rep1");

    verify(userService).findByUsername("rep1");
    verify(whatsAppService)
        .sendOtpAndLog(eq("+50688888888"), contains("Diocese Dashboard"), any(), any());
  }

  @Test
  void generateAndSendOtp_afterTooManyRequests_stopsSending() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    for (int ii = 0; ii < ReporterOtpService.MAX_OTP_REQUEST_ATTEMPTS; ii++) {
      reporterOtpService.generateAndSendOtp("rep1");
      clock.advanceSeconds(ReporterOtpService.REQUEST_THROTTLE_SECONDS);
    }
    reporterOtpService.generateAndSendOtp("rep1");

    verify(userService, times(ReporterOtpService.MAX_OTP_REQUEST_ATTEMPTS))
        .findByUsername("rep1");
    verify(whatsAppService, times(ReporterOtpService.MAX_OTP_REQUEST_ATTEMPTS))
        .sendOtpAndLog(eq("+50688888888"), contains("Diocese Dashboard"), any(), any());
  }

  @Test
  void generateAndSendOtp_unknownUser_isStillThrottledAfterFirstAttempt() {
    when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("ghost"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost");
    reporterOtpService.generateAndSendOtp("ghost");

    verify(userService).findByUsername("ghost");
    verify(whatsAppService, never()).sendOtpAndLog(
        anyString(), anyString(), anyString(), anyString());
  }

  @Test
  void generateAndSendOtp_unknownUser_throwsIllegalArgument() {
    when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("ghost"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost");
  }

  @Test
  void generateAndSendOtp_blankUsername_throwsIllegalArgument() {
    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp(" "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Username");
  }

  @Test
  void generateAndSendOtp_adminUser_throwsIllegalArgument() {
    DashboardUser admin = new DashboardUser();
    admin.setUsername("admin");
    admin.setRole(UserRole.ADMIN);
    admin.setEnabled(true);
    when(userService.findByUsername("admin")).thenReturn(Optional.of(admin));

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("admin"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void generateAndSendOtp_disabledUser_throwsIllegalArgument() {
    DashboardUser reporter = buildReporter("rep1");
    reporter.setEnabled(false);
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("rep1"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void generateAndSendOtp_nullPhone_throwsIllegalArgument() {
    DashboardUser reporter = buildReporter("rep1");
    reporter.setPhoneNumber(null);
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("rep1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("phone number");
  }

  @Test
  void generateAndSendOtp_blankPhone_throwsIllegalArgument() {
    DashboardUser reporter = buildReporter("rep1");
    reporter.setPhoneNumber("  ");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("rep1"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("phone number");
  }

  @Test
  void verifyAndConsumeOtp_correctCode_returnsTrue() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));
    reporterOtpService.generateAndSendOtp("rep1");

    var codeCaptor = ArgumentCaptor.forClass(String.class);
    verify(whatsAppService).sendOtpAndLog(anyString(), codeCaptor.capture(), any(), any());
    String code = codeCaptor.getValue().replaceAll(".*?(\\d{6}).*", "$1");

    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", code).isSuccess()).isTrue();
  }

  @Test
  void verifyAndConsumeOtp_noOtpGenerated_returnsFalse() {
    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", "123456").isSuccess()).isFalse();
  }

  @Test
  void verifyAndConsumeOtp_blankUsername_returnsInvalidWithoutTracking() {
    assertThat(reporterOtpService.verifyAndConsumeOtp(" ", "123456").isSuccess()).isFalse();
    verify(userService, never()).findByUsername(anyString());
  }

  @Test
  void verifyAndConsumeOtp_wrongCode_returnsFalse() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));
    reporterOtpService.generateAndSendOtp("rep1");

    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", "999999").isSuccess()).isFalse();
  }

  @Test
  void verifyAndConsumeOtp_codeConsumedOnSuccess_secondVerifyFails() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    var codeCaptor = ArgumentCaptor.forClass(String.class);
    reporterOtpService.generateAndSendOtp("rep1");
    verify(whatsAppService).sendOtpAndLog(anyString(), codeCaptor.capture(), any(), any());
    String code = codeCaptor.getValue().replaceAll(".*?(\\d{6}).*", "$1");

    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", code).isSuccess()).isTrue();
    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", code).isSuccess()).isFalse();
  }

  @Test
  void verifyAndConsumeOtp_immediateRetry_isThrottled() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));
    reporterOtpService.generateAndSendOtp("rep1");

    ReporterOtpService.OtpVerificationResult firstAttempt =
        reporterOtpService.verifyAndConsumeOtp("rep1", "999999");
    ReporterOtpService.OtpVerificationResult secondAttempt =
        reporterOtpService.verifyAndConsumeOtp("rep1", "999999");

    assertThat(firstAttempt.isSuccess()).isFalse();
    assertThat(firstAttempt.isThrottled()).isFalse();
    assertThat(secondAttempt.isThrottled()).isTrue();
    assertThat(secondAttempt.retryAfterSeconds())
        .isEqualTo(ReporterOtpService.VERIFY_THROTTLE_SECONDS);
  }

  @Test
  void verifyAndConsumeOtp_afterTooManyFailures_returnsAttemptLimitExceeded() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));
    reporterOtpService.generateAndSendOtp("rep1");

    ReporterOtpService.OtpVerificationResult result =
        ReporterOtpService.OtpVerificationResult.invalid();
    for (int ii = 0; ii < ReporterOtpService.MAX_FAILED_VERIFY_ATTEMPTS; ii++) {
      result = reporterOtpService.verifyAndConsumeOtp("rep1", "999999");
      if (ii < ReporterOtpService.MAX_FAILED_VERIFY_ATTEMPTS - 1) {
        advanceSeconds(ReporterOtpService.VERIFY_THROTTLE_SECONDS);
      }
    }

    assertThat(result.isAttemptLimitExceeded()).isTrue();
    assertThat(result.retryAfterSeconds()).isEqualTo(ReporterOtpService.VERIFY_LOCKOUT_SECONDS);
    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", "999999").isAttemptLimitExceeded())
        .isTrue();
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
