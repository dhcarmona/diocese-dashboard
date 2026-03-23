package org.iecr.diocesedashboard.webapp;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.webapp.controller.FrontendController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests for {@link SecurityConfig} authorization rules, login/logout
 * behavior, static asset access, and CSRF enforcement.
 */
@WebMvcTest(FrontendController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private PasswordEncoder passwordEncoder;

  @MockBean
  private UserDetailsService userDetailsService;

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
    // 405 means security allowed it through — FrontendController only handles GET,
    // so Spring rejects the POST method rather than returning 403/401
    mockMvc.perform(post("/api/churches").with(csrf()))
        .andExpect(status().isMethodNotAllowed());
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
