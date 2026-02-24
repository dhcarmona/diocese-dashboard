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
import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.service.CelebrantService;
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

@WebMvcTest(CelebrantController.class)
@Import(SecurityConfig.class)
class CelebrantControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private CelebrantService celebrantService;

  private Celebrant buildCelebrant(Long id, String name) {
    Celebrant c = new Celebrant();
    c.setId(id);
    c.setName(name);
    return c;
  }

  // --- GET /api/celebrants ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(celebrantService.findAll()).thenReturn(
        List.of(buildCelebrant(1L, "Fr. John"), buildCelebrant(2L, "Fr. Paul")));

    mockMvc.perform(get("/api/celebrants"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getAll_asUser_returns200() throws Exception {
    when(celebrantService.findAll()).thenReturn(List.of());

    mockMvc.perform(get("/api/celebrants"))
        .andExpect(status().isOk());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/celebrants"))
        .andExpect(status().isUnauthorized());
  }

  // --- GET /api/celebrants/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_exists_returns200() throws Exception {
    when(celebrantService.findById(1L)).thenReturn(Optional.of(buildCelebrant(1L, "Fr. John")));

    mockMvc.perform(get("/api/celebrants/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Fr. John"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getById_notFound_returns404() throws Exception {
    when(celebrantService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/celebrants/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void getById_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/celebrants/1"))
        .andExpect(status().isForbidden());
  }

  // --- POST /api/celebrants ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_returns201() throws Exception {
    Celebrant celebrant = buildCelebrant(1L, "Fr. John");
    when(celebrantService.save(any(Celebrant.class))).thenReturn(celebrant);

    mockMvc.perform(post("/api/celebrants")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(celebrant)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Fr. John"));
  }

  @Test
  @WithMockUser(roles = "USER")
  void create_asUser_returns403() throws Exception {
    mockMvc.perform(post("/api/celebrants")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildCelebrant(1L, "X"))))
        .andExpect(status().isForbidden());
  }

  // --- PUT /api/celebrants/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    Celebrant celebrant = buildCelebrant(1L, "Fr. John");
    when(celebrantService.existsById(1L)).thenReturn(true);
    when(celebrantService.save(any(Celebrant.class))).thenReturn(celebrant);

    mockMvc.perform(put("/api/celebrants/1")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(celebrant)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_notFound_returns404() throws Exception {
    when(celebrantService.existsById(99L)).thenReturn(false);

    mockMvc.perform(put("/api/celebrants/99")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(buildCelebrant(99L, "Ghost"))))
        .andExpect(status().isNotFound());
  }

  // --- DELETE /api/celebrants/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(celebrantService.existsById(1L)).thenReturn(true);

    mockMvc.perform(delete("/api/celebrants/1"))
        .andExpect(status().isNoContent());

    verify(celebrantService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(celebrantService.existsById(99L)).thenReturn(false);

    mockMvc.perform(delete("/api/celebrants/99"))
        .andExpect(status().isNotFound());
  }
}
