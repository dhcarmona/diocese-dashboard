package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ChurchService;
import org.iecr.diocesedashboard.service.UserService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private UserService userService;

  @MockBean
  private ChurchService churchService;

  private DashboardUser buildUser(Long id, String username, UserRole role) {
    DashboardUser user = new DashboardUser();
    user.setId(id);
    user.setUsername(username);
    user.setRole(role);
    user.setEnabled(true);
    return user;
  }

  private UserRequest adminRequest() {
    return new UserRequest("admin2", "secret123", UserRole.ADMIN, null);
  }

  private UserRequest reporterRequest() {
    return new UserRequest("reporter1", "secret123", UserRole.REPORTER, "Trinity");
  }

  // --- GET /api/users ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(userService.findAll()).thenReturn(
        List.of(buildUser(1L, "admin", UserRole.ADMIN),
            buildUser(2L, "reporter", UserRole.REPORTER)));

    mockMvc.perform(get("/api/users"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void getAll_asReporter_returns403() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/users"))
        .andExpect(status().isUnauthorized());
  }

  // --- GET /api/users/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_exists_returns200() throws Exception {
    when(userService.findById(1L)).thenReturn(
        Optional.of(buildUser(1L, "admin", UserRole.ADMIN)));

    mockMvc.perform(get("/api/users/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin"))
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_notFound_returns404() throws Exception {
    when(userService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/users/99"))
        .andExpect(status().isNotFound());
  }

  // --- POST /api/users ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_adminUser_returns201() throws Exception {
    DashboardUser created = buildUser(1L, "admin2", UserRole.ADMIN);
    when(userService.createUser(eq("admin2"), eq("secret123"), eq(UserRole.ADMIN), eq(null)))
        .thenReturn(created);

    mockMvc.perform(post("/api/users")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(adminRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("admin2"))
        .andExpect(jsonPath("$.role").value("ADMIN"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_reporterWithChurch_returns201() throws Exception {
    Church church = new Church();
    church.setName("Trinity");
    DashboardUser created = buildUser(2L, "reporter1", UserRole.REPORTER);
    created.setChurch(church);
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));
    when(userService.createUser(eq("reporter1"), eq("secret123"), eq(UserRole.REPORTER),
        any(Church.class))).thenReturn(created);

    mockMvc.perform(post("/api/users")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(reporterRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.username").value("reporter1"))
        .andExpect(jsonPath("$.role").value("REPORTER"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_missingPassword_returns400() throws Exception {
    UserRequest noPassword = new UserRequest("newuser", null, UserRole.ADMIN, null);

    mockMvc.perform(post("/api/users")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(noPassword)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_reporterWithoutChurch_returns400() throws Exception {
    UserRequest noChurch = new UserRequest("reporter2", "secret", UserRole.REPORTER, null);

    mockMvc.perform(post("/api/users")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(noChurch)))
        .andExpect(status().isBadRequest());
  }

  // --- PUT /api/users/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    DashboardUser updated = buildUser(1L, "admin2", UserRole.ADMIN);
    when(userService.existsById(1L)).thenReturn(true);
    when(userService.updateUser(eq(1L), eq("admin2"), eq("secret123"),
        eq(UserRole.ADMIN), eq(null))).thenReturn(updated);

    mockMvc.perform(put("/api/users/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(adminRequest())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.username").value("admin2"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_notFound_returns404() throws Exception {
    when(userService.existsById(99L)).thenReturn(false);

    mockMvc.perform(put("/api/users/99")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(adminRequest())))
        .andExpect(status().isNotFound());
  }

  // --- DELETE /api/users/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(userService.existsById(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/users/1").with(csrf()))
        .andExpect(status().isNoContent());

    verify(userService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(userService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/users/99").with(csrf()))
        .andExpect(status().isNotFound());
  }
}
