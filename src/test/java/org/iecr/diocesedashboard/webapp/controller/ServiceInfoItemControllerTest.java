package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
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
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
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

import java.util.List;
import java.util.Optional;

@WebMvcTest(ServiceInfoItemController.class)
@Import(SecurityConfig.class)
class ServiceInfoItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ServiceInfoItemService serviceInfoItemService;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private UserDetailsService userDetailsService;

  private ServiceInfoItem buildItem(Long id, String title) {
    ServiceInfoItem item = new ServiceInfoItem();
    item.setId(id);
    item.setTitle(title);
    item.setRequired(true);
    item.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    return item;
  }

  // --- GET /api/service-info-items ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(serviceInfoItemService.findAll()).thenReturn(
        List.of(buildItem(1L, "attendance"), buildItem(2L, "offering")));

    mockMvc.perform(get("/api/service-info-items"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getAll_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/service-info-items"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/service-info-items"))
        .andExpect(status().isUnauthorized());
  }

  // --- GET /api/service-info-items/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_exists_returns200() throws Exception {
    when(serviceInfoItemService.findById(1L)).thenReturn(Optional.of(buildItem(1L, "attendance")));

    mockMvc.perform(get("/api/service-info-items/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value("attendance"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_notFound_returns404() throws Exception {
    when(serviceInfoItemService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/service-info-items/99"))
        .andExpect(status().isNotFound());
  }

  // --- POST /api/service-info-items ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_returns201() throws Exception {
    ServiceInfoItem item = buildItem(1L, "attendance");
    ServiceTemplate template = new ServiceTemplate();
    template.setId(1L);
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.createItem(any(ServiceInfoItem.class))).thenReturn(item);

    mockMvc.perform(post("/api/service-info-items")
        .param("templateId", "1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(item)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value("attendance"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void create_asUser_returns403() throws Exception {
    mockMvc.perform(post("/api/service-info-items")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildItem(1L, "x"))))
        .andExpect(status().isForbidden());
  }

  // --- PUT /api/service-info-items/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    ServiceInfoItem item = buildItem(1L, "attendance");
    ServiceTemplate template = new ServiceTemplate();
    template.setId(1L);
    when(serviceInfoItemService.existsById(1L)).thenReturn(true);
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.save(any(ServiceInfoItem.class))).thenReturn(item);

    mockMvc.perform(put("/api/service-info-items/1")
        .param("templateId", "1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(item)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_notFound_returns404() throws Exception {
    when(serviceInfoItemService.existsById(99L)).thenReturn(false);

    mockMvc.perform(put("/api/service-info-items/99")
        .param("templateId", "1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildItem(99L, "x"))))
        .andExpect(status().isNotFound());
  }

  // --- DELETE /api/service-info-items/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(serviceInfoItemService.existsById(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/service-info-items/1").with(csrf()))
        .andExpect(status().isNoContent());

    verify(serviceInfoItemService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(serviceInfoItemService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/service-info-items/99").with(csrf()))
        .andExpect(status().isNotFound());
  }

  // --- PUT /api/service-info-items/reorder ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void reorder_asAdmin_returns204() throws Exception {
    mockMvc.perform(put("/api/service-info-items/reorder")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"orderedIds\":[3,1,2]}"))
        .andExpect(status().isNoContent());

    verify(serviceInfoItemService).reorder(List.of(3L, 1L, 2L));
  }

  @Test
  @WithMockUser(roles = "USER")
  void reorder_asUser_returns403() throws Exception {
    mockMvc.perform(put("/api/service-info-items/reorder")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"orderedIds\":[1,2]}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void reorder_unauthenticated_returns401() throws Exception {
    mockMvc.perform(put("/api/service-info-items/reorder")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{\"orderedIds\":[1,2]}"))
        .andExpect(status().isUnauthorized());
  }
}
