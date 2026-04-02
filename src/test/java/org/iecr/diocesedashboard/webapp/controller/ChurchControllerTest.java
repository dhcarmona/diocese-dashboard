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
import org.iecr.diocesedashboard.service.ChurchService;
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

@WebMvcTest(ChurchController.class)
@Import(SecurityConfig.class)
class ChurchControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ChurchService churchService;

  @MockBean
  private UserDetailsService userDetailsService;

  private Church buildChurch(String name) {
    Church c = new Church();
    c.setName(name);
    c.setLocation("San José");
    c.setPortraitDataUrl("data:image/svg+xml;base64,church-" + name);
    return c;
  }

  // --- GET /api/churches ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(churchService.findAll()).thenReturn(List.of(buildChurch("ChurchA"), buildChurch("ChurchB")));

    mockMvc.perform(get("/api/churches"))
        .andExpect(status().isOk())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].portraitDataUrl")
            .value("data:image/svg+xml;base64,church-ChurchA"));
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void getAll_asReporter_returns200() throws Exception {
    when(churchService.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/api/churches"))
        .andExpect(status().isOk());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/churches"))
        .andExpect(status().isUnauthorized());
  }

  // --- GET /api/churches/{name} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getByName_exists_returns200() throws Exception {
    Church church = buildChurch("Trinity");
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));

    mockMvc.perform(get("/api/churches/Trinity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Trinity"))
        .andExpect(jsonPath("$.portraitDataUrl")
            .value("data:image/svg+xml;base64,church-Trinity"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getByName_notFound_returns404() throws Exception {
    when(churchService.findById("Unknown")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/churches/Unknown"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void getByName_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/churches/Trinity"))
        .andExpect(status().isForbidden());
  }

  // --- POST /api/churches ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_returns201() throws Exception {
    Church church = buildChurch("NewChurch");
    when(churchService.save(any(Church.class))).thenReturn(church);

    mockMvc.perform(post("/api/churches")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(church)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("NewChurch"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void create_asUser_returns403() throws Exception {
    mockMvc.perform(post("/api/churches")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildChurch("X"))))
        .andExpect(status().isForbidden());
  }

  // --- PUT /api/churches/{name} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    Church church = buildChurch("Trinity");
    when(churchService.existsById("Trinity")).thenReturn(true);
    when(churchService.save(any(Church.class))).thenReturn(church);

    mockMvc.perform(put("/api/churches/Trinity")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(church)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Trinity"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_notFound_returns404() throws Exception {
    when(churchService.existsById(eq("Ghost"))).thenReturn(false);

    mockMvc.perform(put("/api/churches/Ghost")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildChurch("Ghost"))))
        .andExpect(status().isNotFound());
  }

  // --- DELETE /api/churches/{name} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(churchService.existsById("Trinity")).thenReturn(true);

    mockMvc.perform(delete("/api/churches/Trinity").with(csrf()))
        .andExpect(status().isNoContent());

    verify(churchService).deleteById("Trinity");
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(churchService.existsById("Ghost")).thenReturn(false);

    mockMvc.perform(delete("/api/churches/Ghost").with(csrf()))
        .andExpect(status().isNotFound());
  }
}
