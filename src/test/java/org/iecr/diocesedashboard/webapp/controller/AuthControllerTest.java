package org.iecr.diocesedashboard.webapp.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.iecr.diocesedashboard.webapp.WithMockDashboardUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private UserDetailsService userDetailsService;

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
        .andExpect(jsonPath("$.assignedChurchNames").isArray())
        .andExpect(jsonPath("$.passwordHash").doesNotExist());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchNames = {"StPaul", "Trinity"})
  void getAuthenticatedUser_asReporter_returnsChurchAssignments() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.role").value("REPORTER"))
        .andExpect(jsonPath("$.assignedChurchNames[0]").value("StPaul"))
        .andExpect(jsonPath("$.assignedChurchNames[1]").value("Trinity"));
  }

  @Test
  void getAuthenticatedUser_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/auth/me"))
        .andExpect(status().isUnauthorized());
  }
}
