package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.domain.objects.SectionHeader;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.SectionHeaderService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

@WebMvcTest(SectionHeaderController.class)
@Import(SecurityConfig.class)
class SectionHeaderControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private SectionHeaderService sectionHeaderService;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private UserDetailsService userDetailsService;

  private SectionHeader buildHeader(Long id, String title) {
    SectionHeader h = new SectionHeader();
    h.setId(id);
    h.setTitle(title);
    return h;
  }

  // --- POST /api/section-headers ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_withValidTemplate_returns201() throws Exception {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(1L);
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));

    SectionHeader created = buildHeader(10L, "Finance");
    when(sectionHeaderService.createHeader(any())).thenReturn(created);

    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(post("/api/section-headers")
        .param("templateId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("Finance"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_withMissingTemplate_returns404() throws Exception {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(post("/api/section-headers")
        .param("templateId", "99")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_withBlankTitle_returns400() throws Exception {
    SectionHeader body = buildHeader(null, "  ");
    mockMvc.perform(post("/api/section-headers")
        .param("templateId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "USER")
  void create_asUser_returns403() throws Exception {
    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(post("/api/section-headers")
        .param("templateId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void create_unauthenticated_returns401() throws Exception {
    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(post("/api/section-headers")
        .param("templateId", "1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  // --- PUT /api/section-headers/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_withExistingId_returns200() throws Exception {
    SectionHeader existing = buildHeader(10L, "Old Title");
    when(sectionHeaderService.findById(10L)).thenReturn(Optional.of(existing));

    SectionHeader updated = buildHeader(10L, "Finance");
    when(sectionHeaderService.save(any())).thenReturn(updated);

    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(put("/api/section-headers/10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("Finance"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_withMissingId_returns404() throws Exception {
    when(sectionHeaderService.findById(99L)).thenReturn(Optional.empty());

    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(put("/api/section-headers/99")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void update_asUser_returns403() throws Exception {
    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(put("/api/section-headers/10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_unauthenticated_returns401() throws Exception {
    SectionHeader body = buildHeader(null, "Finance");
    mockMvc.perform(put("/api/section-headers/10")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(body))
        .with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  // --- DELETE /api/section-headers/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_withExistingId_returns204() throws Exception {
    when(sectionHeaderService.existsById(10L)).thenReturn(true);

    mockMvc.perform(delete("/api/section-headers/10")
        .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_withMissingId_returns404() throws Exception {
    when(sectionHeaderService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/section-headers/99")
        .with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void delete_asUser_returns403() throws Exception {
    mockMvc.perform(delete("/api/section-headers/10")
        .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void delete_unauthenticated_returns401() throws Exception {
    mockMvc.perform(delete("/api/section-headers/10")
        .with(csrf()))
        .andExpect(status().isUnauthorized());
  }
}
