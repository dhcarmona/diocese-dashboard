package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ReporterOtpService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.iecr.diocesedashboard.webapp.WithMockDashboardUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserService userService;

  @MockBean
  private ReporterOtpService reporterOtpService;

  @Test
  void getCsrfToken_returnsTokenPayload() throws Exception {
    mockMvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.headerName").value("X-CSRF-TOKEN"))
        .andExpect(jsonPath("$.token").isString());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getAuthenticatedUser_asAdmin_returnsSafePayload() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("testuser"))
        .andExpect(jsonPath("$.role").value("ADMIN"))
        .andExpect(jsonPath("$.preferredLanguage").value("es"))
        .andExpect(jsonPath("$.assignedChurchNames").isArray())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchNames = {"StPaul", "Trinity"})
  void getAuthenticatedUser_asReporter_returnsChurchAssignments() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("REPORTER"))
        .andExpect(jsonPath("$.preferredLanguage").value("es"))
        .andExpect(jsonPath("$.assignedChurchNames[0]").value("StPaul"))
        .andExpect(jsonPath("$.assignedChurchNames[1]").value("Trinity"));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void updatePreferredLanguage_updatesUserAndReturnsPayload() throws Exception {
    DashboardUser reporter = buildReporter("testuser");
    reporter.setPreferredLanguage("en");
    when(userService.updatePreferredLanguage(1L, "en")).thenReturn(reporter);

    mockMvc.perform(put("/api/auth/me/language")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"language\":\"en\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.preferredLanguage").value("en"));
  }

  @Test
  void getAuthenticatedUser_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  // --- POST /api/auth/reporter/request-otp ---

  @Test
  void requestReporterOtp_validUser_returns200() throws Exception {
    mockMvc.perform(post("/api/auth/reporter/request-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"rep1\"}"))
        .andExpect(status().isOk());

    verify(reporterOtpService).generateAndSendOtp("rep1");
  }

  @Test
  void requestReporterOtp_unknownUser_returns200() throws Exception {
    doThrow(new IllegalArgumentException("No active reporter found"))
        .when(reporterOtpService).generateAndSendOtp("ghost");

    mockMvc.perform(post("/api/auth/reporter/request-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"ghost\"}"))
        .andExpect(status().isOk());

    verify(reporterOtpService).generateAndSendOtp("ghost");
  }

  @Test
  void requestReporterOtp_blankUsername_returns400() throws Exception {
    mockMvc.perform(post("/api/auth/reporter/request-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"\"}"))
        .andExpect(status().isBadRequest());
  }

  // --- POST /api/auth/reporter/verify-otp ---

  @Test
  void verifyReporterOtp_validCode_returns200() throws Exception {
    DashboardUser reporter = buildReporter("rep1");
    when(reporterOtpService.verifyAndConsumeOtp("rep1", "123456"))
        .thenReturn(ReporterOtpService.OtpVerificationResult.success());
    when(userService.findByUsername("rep1")).thenReturn(Optional.of(reporter));

    mockMvc.perform(post("/api/auth/reporter/verify-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"rep1\",\"code\":\"123456\"}"))
        .andExpect(status().isOk());
  }

  @Test
  void verifyReporterOtp_wrongCode_returns401() throws Exception {
    when(reporterOtpService.verifyAndConsumeOtp("rep1", "000000"))
        .thenReturn(ReporterOtpService.OtpVerificationResult.invalid());

    mockMvc.perform(post("/api/auth/reporter/verify-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"rep1\",\"code\":\"000000\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void verifyReporterOtp_unknownUser_returns401() throws Exception {
    when(reporterOtpService.verifyAndConsumeOtp("ghost", "123456"))
        .thenReturn(ReporterOtpService.OtpVerificationResult.success());
    when(userService.findByUsername("ghost")).thenReturn(Optional.empty());

    mockMvc.perform(post("/api/auth/reporter/verify-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"ghost\",\"code\":\"123456\"}"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void verifyReporterOtp_throttled_returns429() throws Exception {
    when(reporterOtpService.verifyAndConsumeOtp("rep1", "123456"))
        .thenReturn(ReporterOtpService.OtpVerificationResult.throttled(5));

    mockMvc.perform(post("/api/auth/reporter/verify-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"rep1\",\"code\":\"123456\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "5"))
        .andExpect(jsonPath("$.message")
            .value("Please wait before trying another login code."))
        .andExpect(jsonPath("$.status").value(429));
  }

  @Test
  void verifyReporterOtp_attemptLimitExceeded_returns429WithRetryAfter() throws Exception {
    when(reporterOtpService.verifyAndConsumeOtp("rep1", "123456"))
        .thenReturn(ReporterOtpService.OtpVerificationResult.attemptLimitExceeded(600));

    mockMvc.perform(post("/api/auth/reporter/verify-otp")
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"username\":\"rep1\",\"code\":\"123456\"}"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "600"))
        .andExpect(jsonPath("$.message")
            .value("Too many login code attempts. Request a new code and try again later."))
        .andExpect(jsonPath("$.status").value(429));
  }

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
}
