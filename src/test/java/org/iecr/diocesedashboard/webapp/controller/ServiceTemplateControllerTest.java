package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.ServiceSubmissionService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@WebMvcTest(ServiceTemplateController.class)
@Import(SecurityConfig.class)
class ServiceTemplateControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private ServiceSubmissionService serviceSubmissionService;

  private ServiceTemplate buildTemplate(Long id, String name) {
    ServiceTemplate t = new ServiceTemplate();
    t.setId(id);
    t.setServiceTemplateName(name);
    return t;
  }

  private ServiceInstanceRequest buildRequest() {
    return new ServiceInstanceRequest("Trinity", List.of(1L), LocalDate.of(2024, 1, 14),
        List.of(new ServiceInstanceRequest.ResponseEntry(1L, "120")));
  }

  // --- GET /api/service-templates ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(serviceTemplateService.findAll()).thenReturn(
        List.of(buildTemplate(1L, "Sunday Mass"), buildTemplate(2L, "Vespers")));

    mockMvc.perform(get("/api/service-templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getAll_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/service-templates"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/service-templates"))
        .andExpect(status().isUnauthorized());
  }

  // --- GET /api/service-templates/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_asAdmin_exists_returns200() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate(1L, "Sunday Mass")));

    mockMvc.perform(get("/api/service-templates/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.serviceTemplateName").value("Sunday Mass"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getById_asUser_exists_returns200() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate(1L, "Sunday Mass")));

    mockMvc.perform(get("/api/service-templates/1"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_notFound_returns404() throws Exception {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/service-templates/99"))
        .andExpect(status().isNotFound());
  }

  // --- POST /api/service-templates ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_returns201() throws Exception {
    ServiceTemplate template = buildTemplate(1L, "Sunday Mass");
    when(serviceTemplateService.save(any(ServiceTemplate.class))).thenReturn(template);

    mockMvc.perform(post("/api/service-templates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(template)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.serviceTemplateName").value("Sunday Mass"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void create_asUser_returns403() throws Exception {
    mockMvc.perform(post("/api/service-templates")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildTemplate(1L, "X"))))
        .andExpect(status().isForbidden());
  }

  // --- PUT /api/service-templates/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    ServiceTemplate template = buildTemplate(1L, "Updated");
    when(serviceTemplateService.existsById(1L)).thenReturn(true);
    when(serviceTemplateService.save(any(ServiceTemplate.class))).thenReturn(template);

    mockMvc.perform(put("/api/service-templates/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(template)))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_notFound_returns404() throws Exception {
    when(serviceTemplateService.existsById(99L)).thenReturn(false);

    mockMvc.perform(put("/api/service-templates/99")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildTemplate(99L, "X"))))
        .andExpect(status().isNotFound());
  }

  // --- DELETE /api/service-templates/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(serviceTemplateService.existsById(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/service-templates/1"))
        .andExpect(status().isNoContent());

    verify(serviceTemplateService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(serviceTemplateService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/service-templates/99"))
        .andExpect(status().isNotFound());
  }

  // --- POST /api/service-templates/{id}/submit ---

  @Test
  @WithMockUser(roles = "USER")
  void submit_asUser_validRequest_returns201() throws Exception {
    ServiceInstance instance = new ServiceInstance();
    instance.setId(42L);
    when(serviceSubmissionService.submit(eq(1L), any(ServiceInstanceRequest.class)))
        .thenReturn(instance);

    objectMapper.registerModule(new JavaTimeModule());

    mockMvc.perform(post("/api/service-templates/1/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(42));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void submit_asAdmin_validRequest_returns201() throws Exception {
    ServiceInstance instance = new ServiceInstance();
    instance.setId(7L);
    when(serviceSubmissionService.submit(eq(1L), any(ServiceInstanceRequest.class)))
        .thenReturn(instance);

    objectMapper.registerModule(new JavaTimeModule());

    mockMvc.perform(post("/api/service-templates/1/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isCreated());
  }

  @Test
  @WithMockUser(roles = "USER")
  void submit_templateNotFound_returns404() throws Exception {
    when(serviceSubmissionService.submit(eq(99L), any(ServiceInstanceRequest.class)))
        .thenThrow(new ResponseStatusException(NOT_FOUND, "Template not found"));

    objectMapper.registerModule(new JavaTimeModule());

    mockMvc.perform(post("/api/service-templates/99/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isNotFound());
  }

  @Test
  void submit_unauthenticated_returns401() throws Exception {
    objectMapper.registerModule(new JavaTimeModule());

    mockMvc.perform(post("/api/service-templates/1/submit")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildRequest())))
        .andExpect(status().isUnauthorized());
  }
}
