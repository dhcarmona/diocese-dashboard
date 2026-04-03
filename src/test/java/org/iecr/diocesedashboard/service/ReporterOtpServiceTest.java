package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ReporterOtpServiceTest {

  @Mock
  private WhatsAppService whatsAppService;

  @Mock
  private UserService userService;

  @InjectMocks
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
    // fresh service per test via @InjectMocks
  }

  @Test
  void generateAndSendOtp_sendsWhatsAppMessage() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    reporterOtpService.generateAndSendOtp("rep1");

    verify(whatsAppService).sendMessage(eq("+50688888888"), contains("Diocese Dashboard"));
  }

  @Test
  void generateAndSendOtp_unknownUser_throwsIllegalArgument() {
    when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reporterOtpService.generateAndSendOtp("ghost"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("ghost");
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
    verify(whatsAppService).sendMessage(anyString(), codeCaptor.capture());
    String message = codeCaptor.getValue();
    String code = message.replaceAll(".*code is: (\\d{6}).*", "$1");

    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", code)).isTrue();
  }

  @Test
  void verifyAndConsumeOtp_noOtpGenerated_returnsFalse() {
    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", "123456")).isFalse();
  }

  @Test
  void verifyAndConsumeOtp_wrongCode_returnsFalse() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));
    reporterOtpService.generateAndSendOtp("rep1");

    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", "999999")).isFalse();
  }

  @Test
  void verifyAndConsumeOtp_codeConsumedOnSuccess_secondVerifyFails() {
    DashboardUser reporter = buildReporter("rep1");
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    // Use reflection-friendly approach: capture code via ArgumentCaptor
    var codeCaptor = ArgumentCaptor.forClass(String.class);
    reporterOtpService.generateAndSendOtp("rep1");
    verify(whatsAppService).sendMessage(anyString(), codeCaptor.capture());

    // Extract the 6-digit code from the message "...code is: 123456. It expires..."
    String message = codeCaptor.getValue();
    String code = message.replaceAll(".*code is: (\\d{6}).*", "$1");

    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", code)).isTrue();
    assertThat(reporterOtpService.verifyAndConsumeOtp("rep1", code)).isFalse();
  }
}
