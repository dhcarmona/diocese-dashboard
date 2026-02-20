package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
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

@WebMvcTest(ServiceInfoItemController.class)
@Import(SecurityConfig.class)
class ServiceInfoItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ServiceInfoItemService serviceInfoItemService;

  private ServiceInfoItem buildItem(Long id, String questionId) {
    ServiceInfoItem item = new ServiceInfoItem();
    item.setId(id);
    item.setQuestionId(questionId);
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
        .andExpect(jsonPath("$.questionId").value("attendance"));
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
    when(serviceInfoItemService.save(any(ServiceInfoItem.class))).thenReturn(item);

    mockMvc.perform(post("/api/service-info-items")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(item)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.questionId").value("attendance"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void create_asUser_returns403() throws Exception {
    mockMvc.perform(post("/api/service-info-items")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildItem(1L, "x"))))
        .andExpect(status().isForbidden());
  }

  // --- PUT /api/service-info-items/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    ServiceInfoItem item = buildItem(1L, "attendance");
    when(serviceInfoItemService.existsById(1L)).thenReturn(true);
    when(serviceInfoItemService.save(any(ServiceInfoItem.class))).thenReturn(item);

    mockMvc.perform(put("/api/service-info-items/1")
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
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildItem(99L, "x"))))
        .andExpect(status().isNotFound());
  }

  // --- DELETE /api/service-info-items/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(serviceInfoItemService.existsById(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/service-info-items/1"))
        .andExpect(status().isNoContent());

    verify(serviceInfoItemService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(serviceInfoItemService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/service-info-items/99"))
        .andExpect(status().isNotFound());
  }
}
