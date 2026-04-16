package org.iecr.diocesedashboard.webapp;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.PortraitService;
import org.iecr.diocesedashboard.service.ReporterMagicLinkService;
import org.iecr.diocesedashboard.service.ReporterOtpService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.controller.AuthController;
import org.iecr.diocesedashboard.webapp.controller.FrontendController;
import org.iecr.diocesedashboard.webapp.controller.PortraitController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Integration tests for {@link SecurityConfig} authorization rules, login/logout
 * behavior, static asset access, and CSRF enforcement.
 */
@WebMvcTest({FrontendController.class, AuthController.class, PortraitController.class})
@Import(SecurityConfig.class)
class SecurityConfigTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @Autowired
  private AdminLoginThrottleService adminLoginThrottleService;

  @MockBean
  private UserService userDetailsService;

  @MockBean
  private ReporterMagicLinkService reporterMagicLinkService;

  @MockBean
  private ReporterOtpService reporterOtpService;

  @MockBean
  private PortraitService portraitService;

  @BeforeEach
  void resetLoginThrottle() {
    adminLoginThrottleService.clearAllAttempts();
  }

  // ---------------------------------------------------------------------------
  // Login endpoint
  // ---------------------------------------------------------------------------

  @Test
  void login_validCredentials_returns200() throws Exception {
    String raw = "secret";
    DashboardUser user = buildUser("admin", raw, UserRole.ADMIN);
    DashboardUserDetails details = new DashboardUserDetails(user);
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);

    mockMvc.perform(post("/api/auth/login")
        .param("username", "admin")
        .param("password", raw))
        .andExpect(status().isOk());
  }

  @Test
  void login_invalidPassword_returns401() throws Exception {
    DashboardUser user = buildUser("admin", "correct", UserRole.ADMIN);
    DashboardUserDetails details = new DashboardUserDetails(user);
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);

    mockMvc.perform(post("/api/auth/login")
        .param("username", "admin")
        .param("password", "wrong"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_immediateRetryAfterFailure_returns429() throws Exception {
    DashboardUser user = buildUser("admin", "correct", UserRole.ADMIN);
    DashboardUserDetails details = new DashboardUserDetails(user);
    when(userDetailsService.loadUserByUsername("admin")).thenReturn(details);

    mockMvc.perform(post("/api/auth/login")
        .param("username", "admin")
        .param("password", "wrong"))
        .andExpect(status().isUnauthorized());

    mockMvc.perform(post("/api/auth/login")
        .param("username", "admin")
        .param("password", "wrong"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void login_lockedUsername_returns429() throws Exception {
    for (int ii = 0; ii < AdminLoginThrottleService.MAX_FAILED_LOGIN_ATTEMPTS; ii++) {
      adminLoginThrottleService.recordFailedAttempt("admin");
    }

    mockMvc.perform(post("/api/auth/login")
        .param("username", "admin")
        .param("password", "wrong"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"));
  }

  @Test
  void login_unknownUser_returns401() throws Exception {
    when(userDetailsService.loadUserByUsername("nobody"))
        .thenThrow(new UsernameNotFoundException("not found"));

    mockMvc.perform(post("/api/auth/login")
        .param("username", "nobody")
        .param("password", "anything"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void login_noCredentials_returns401() throws Exception {
    mockMvc.perform(post("/api/auth/login"))
        .andExpect(status().isUnauthorized());
  }

  // ---------------------------------------------------------------------------
  // Logout endpoint
  // ---------------------------------------------------------------------------

  @Test
  @WithMockDashboardUser
  void logout_authenticated_returns200() throws Exception {
    mockMvc.perform(post("/api/auth/logout").with(csrf()))
        .andExpect(status().isOk());
  }

  // ---------------------------------------------------------------------------
  // Unauthenticated access returns 401 (not a redirect), suitable for SPA
  // ---------------------------------------------------------------------------

  @Test
  void unauthenticatedRequest_protectedEndpoint_returns401NotRedirect() throws Exception {
    mockMvc.perform(get("/api/churches"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void unauthenticatedCurrentUserRequest_returns401NotRedirect() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
  }

  // ---------------------------------------------------------------------------
  // Static assets and public pages are accessible without authentication
  // ---------------------------------------------------------------------------

  @Test
  void staticRoot_noAuth_returns200() throws Exception {
    mockMvc.perform(get("/")).andExpect(status().isOk());
  }

  @Test
  void staticIndexHtml_noAuth_returns200() throws Exception {
    mockMvc.perform(get("/index.html")).andExpect(status().isOk());
  }

  @Test
  void loginPage_noAuth_returns200() throws Exception {
    mockMvc.perform(get("/login")).andExpect(status().isOk());
  }

  @Test
  void nestedSpaRoute_noAuth_returns200() throws Exception {
    mockMvc.perform(get("/users/manage")).andExpect(status().isOk());
  }

  @Test
  void missingAsset_noAuth_returns404InsteadOfSpaHtml() throws Exception {
    mockMvc.perform(get("/assets/does-not-exist.js"))
        .andExpect(status().isNotFound());
  }

  @Test
  void portraitAsset_noAuth_returns401() throws Exception {
    mockMvc.perform(get("/portraits/celebrants/placeholder.svg"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void portraitApi_noAuth_returns401() throws Exception {
    mockMvc.perform(get("/api/portraits/celebrants").param("name", "Ana Perez"))
        .andExpect(status().isUnauthorized());
  }

  // ---------------------------------------------------------------------------
  // anyRequest() catch-all requires ADMIN
  // ---------------------------------------------------------------------------

  @Test
  void unknownEndpoint_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/v1.0/some-endpoint"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void unknownEndpoint_reporter_returns403() throws Exception {
    mockMvc.perform(get("/v1.0/some-endpoint"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void currentUser_reporter_passesSecurityLayer() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void updatePreferredLanguage_reporter_passesSecurityLayer() throws Exception {
    DashboardUser user = new DashboardUser();
    user.setId(1L);
    user.setUsername("testuser");
    user.setRole(UserRole.REPORTER);
    user.setPreferredLanguage("en");
    when(userDetailsService.updatePreferredLanguage(1L, "en")).thenReturn(user);

    mockMvc.perform(put("/api/auth/me/language")
        .with(csrf())
        .contentType("application/json")
        .content("{\"language\":\"en\"}"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void unknownEndpoint_admin_returns404NotForbidden() throws Exception {
    // ADMIN passes security. Use a path whose first segment contains a dot so
    // FrontendController's catch-all (no-dot first segment) does not match it, giving a clean 404.
    mockMvc.perform(get("/v1.0/some-endpoint"))
        .andExpect(status().isNotFound());
  }

  // ---------------------------------------------------------------------------
  // CSRF enforcement
  // ---------------------------------------------------------------------------

  @Test
  @WithMockDashboardUser
  void postWithoutCsrfToken_returns403() throws Exception {
    mockMvc.perform(post("/api/churches"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser
  void postWithCsrfToken_passesSecurityLayer() throws Exception {
    // 404 means security allowed it through — this slice test does not load
    // ChurchController, so Spring reports that no handler exists.
    mockMvc.perform(post("/api/churches").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void loginPost_withoutCsrfToken_isPermitted() throws Exception {
    // /api/auth/login is explicitly excluded from CSRF checks
    when(userDetailsService.loadUserByUsername("nobody"))
        .thenThrow(new UsernameNotFoundException("not found"));

    mockMvc.perform(post("/api/auth/login")
        .param("username", "nobody")
        .param("password", "anything"))
        .andExpect(status().isUnauthorized()); // 401, not 403 — CSRF did not block it
  }

  @Test
  @WithMockDashboardUser
  void logout_withBootstrapCsrfToken_returns200() throws Exception {
    MvcResult csrfResult = mockMvc.perform(get("/api/auth/csrf"))
        .andExpect(status().isOk())
        .andReturn();
    MockHttpSession session = (MockHttpSession) csrfResult.getRequest().getSession(false);
    JsonNode csrfPayload = objectMapper.readTree(csrfResult.getResponse().getContentAsString());

    mockMvc.perform(post("/api/auth/logout")
        .session(session)
        .header(csrfPayload.get("headerName").asText(), csrfPayload.get("token").asText()))
        .andExpect(status().isOk());
  }

  // ---------------------------------------------------------------------------
  // Statistics endpoint security
  // ---------------------------------------------------------------------------

  @Test
  void apiStatistics_anonymous_returns401() throws Exception {
    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void statisticsFrontendRoute_anonymous_returns200() throws Exception {
    mockMvc.perform(get("/statistics")).andExpect(status().isOk());
  }

  @Test
  void statisticsFrontendRouteWithId_anonymous_returns200() throws Exception {
    mockMvc.perform(get("/statistics/1")).andExpect(status().isOk());
  }

  @Test
  void statisticsReportFrontendRoute_anonymous_returns200() throws Exception {
    mockMvc.perform(get("/statistics/1/report")).andExpect(status().isOk());
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private DashboardUser buildUser(String username, String rawPassword, UserRole role) {
    DashboardUser user = new DashboardUser();
    user.setId(1L);
    user.setUsername(username);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setRole(role);
    user.setEnabled(true);
    return user;
  }
}
