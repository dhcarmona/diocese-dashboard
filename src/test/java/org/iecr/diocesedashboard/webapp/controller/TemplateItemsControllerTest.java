package org.iecr.diocesedashboard.webapp.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.service.TemplateItemOrderService;
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

@WebMvcTest(TemplateItemsController.class)
@Import(SecurityConfig.class)
class TemplateItemsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private TemplateItemOrderService templateItemOrderService;

  @MockBean
  private UserDetailsService userDetailsService;

  private TemplateItemReorderRequest validRequest() {
    return new TemplateItemReorderRequest(
        1L,
        List.of(new TemplateItemRef(10L, TemplateItemKind.INFO_ITEM))
    );
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void reorder_asAdmin_returns204() throws Exception {
    mockMvc.perform(put("/api/template-items/reorder")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest()))
        .with(csrf()))
        .andExpect(status().isNoContent());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void reorder_withNullTemplateId_returns400() throws Exception {
    String body = "{\"items\":[{\"id\":10,\"kind\":\"INFO_ITEM\"}]}";

    mockMvc.perform(put("/api/template-items/reorder")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void reorder_withEmptyItemsList_returns400() throws Exception {
    String body = "{\"templateId\":1,\"items\":[]}";

    mockMvc.perform(put("/api/template-items/reorder")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body)
        .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "USER")
  void reorder_asUser_returns403() throws Exception {
    mockMvc.perform(put("/api/template-items/reorder")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest()))
        .with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void reorder_unauthenticated_returns401() throws Exception {
    mockMvc.perform(put("/api/template-items/reorder")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest()))
        .with(csrf()))
        .andExpect(status().isUnauthorized());
  }
}
