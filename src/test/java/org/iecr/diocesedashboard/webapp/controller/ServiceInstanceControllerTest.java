package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.service.ServiceInstanceService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

@WebMvcTest(ServiceInstanceController.class)
@Import(SecurityConfig.class)
class ServiceInstanceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private ServiceInstanceService serviceInstanceService;

  private ServiceInstance buildInstance(Long id) {
    ServiceInstance i = new ServiceInstance();
    i.setId(id);
    return i;
  }

  // --- GET /api/service-instances ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(serviceInstanceService.findAll()).thenReturn(
        List.of(buildInstance(1L), buildInstance(2L)));

    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getAll_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isUnauthorized());
  }

  // --- GET /api/service-instances/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_exists_returns200() throws Exception {
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(buildInstance(1L)));

    mockMvc.perform(get("/api/service-instances/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_notFound_returns404() throws Exception {
    when(serviceInstanceService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/service-instances/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void getById_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/service-instances/1"))
        .andExpect(status().isForbidden());
  }

  // --- DELETE /api/service-instances/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(serviceInstanceService.existsById(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/service-instances/1"))
        .andExpect(status().isNoContent());

    verify(serviceInstanceService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(serviceInstanceService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/service-instances/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void delete_asUser_returns403() throws Exception {
    mockMvc.perform(delete("/api/service-instances/1"))
        .andExpect(status().isForbidden());
  }
}
