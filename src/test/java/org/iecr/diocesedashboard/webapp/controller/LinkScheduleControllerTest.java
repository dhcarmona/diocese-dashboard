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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hamcrest.Matchers;
import org.iecr.diocesedashboard.domain.objects.LinkSchedule;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.service.LinkScheduleService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;

@WebMvcTest(LinkScheduleController.class)
@Import(SecurityConfig.class)
class LinkScheduleControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private LinkScheduleService linkScheduleService;

  @MockBean
  private UserDetailsService userDetailsService;

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());
  }

  private LinkSchedule buildSchedule(Long id) {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(10L);
    template.setServiceTemplateName("Sunday Eucharist");

    LinkSchedule schedule = new LinkSchedule();
    schedule.setId(id);
    schedule.setServiceTemplate(template);
    schedule.setChurchNames(Set.of("Church A", "Church B"));
    schedule.setDaysOfWeek(Set.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY));
    schedule.setSendHour(8);
    schedule.setCreatedAt(Instant.EPOCH);
    return schedule;
  }

  private LinkScheduleRequest validRequest() {
    return new LinkScheduleRequest(
        10L,
        List.of("Church A"),
        List.of(DayOfWeek.MONDAY),
        8);
  }

  // --- GET /api/link-schedules ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(linkScheduleService.findAll()).thenReturn(
        List.of(buildSchedule(1L), buildSchedule(2L)));

    mockMvc.perform(get("/api/link-schedules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].serviceTemplateName").value("Sunday Eucharist"))
        .andExpect(jsonPath("$[0].sendHour").value(8))
        .andExpect(jsonPath("$[0].churchNames",
            Matchers.containsInAnyOrder("Church A", "Church B")));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_returnsChurchNamesSorted() throws Exception {
    when(linkScheduleService.findAll()).thenReturn(List.of(buildSchedule(1L)));

    mockMvc.perform(get("/api/link-schedules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].churchNames[0]").value("Church A"))
        .andExpect(jsonPath("$[0].churchNames[1]").value("Church B"));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void getAll_returnsDaysOfWeekSorted() throws Exception {
    when(linkScheduleService.findAll()).thenReturn(List.of(buildSchedule(1L)));

    mockMvc.perform(get("/api/link-schedules"))
        .andExpect(status().isOk())
        // MONDAY (value=1) before FRIDAY (value=5)
        .andExpect(jsonPath("$[0].daysOfWeek[0]").value("MONDAY"))
        .andExpect(jsonPath("$[0].daysOfWeek[1]").value("FRIDAY"));
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void getAll_asReporter_returns403() throws Exception {
    mockMvc.perform(get("/api/link-schedules"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/link-schedules"))
        .andExpect(status().isUnauthorized());
  }

  // --- POST /api/link-schedules ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_asAdmin_returns201() throws Exception {
    LinkSchedule saved = buildSchedule(5L);
    when(linkScheduleService.create(any())).thenReturn(saved);

    mockMvc.perform(post("/api/link-schedules")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(5))
        .andExpect(jsonPath("$.serviceTemplateId").value(10));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_withInvalidPayload_returns400() throws Exception {
    // sendHour = 25 is invalid (@Max(23))
    String badJson = """
        {"serviceTemplateId":1,"churchNames":["Church A"],"daysOfWeek":["MONDAY"],"sendHour":25}
        """;

    mockMvc.perform(post("/api/link-schedules")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(badJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void create_withEmptyChurches_returns400() throws Exception {
    String badJson = """
        {"serviceTemplateId":1,"churchNames":[],"daysOfWeek":["MONDAY"],"sendHour":8}
        """;

    mockMvc.perform(post("/api/link-schedules")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(badJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void create_asReporter_returns403() throws Exception {
    mockMvc.perform(post("/api/link-schedules")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isForbidden());
  }

  // --- PUT /api/link-schedules/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_exists_returns200() throws Exception {
    LinkSchedule updated = buildSchedule(3L);
    when(linkScheduleService.update(eq(3L), any())).thenReturn(updated);

    mockMvc.perform(put("/api/link-schedules/3")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(3));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void update_notFound_returns404() throws Exception {
    when(linkScheduleService.update(eq(99L), any()))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));

    mockMvc.perform(put("/api/link-schedules/99")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void update_asReporter_returns403() throws Exception {
    mockMvc.perform(put("/api/link-schedules/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(validRequest())))
        .andExpect(status().isForbidden());
  }

  // --- DELETE /api/link-schedules/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    mockMvc.perform(delete("/api/link-schedules/7").with(csrf()))
        .andExpect(status().isNoContent());

    verify(linkScheduleService).delete(7L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    org.mockito.Mockito.doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
        .when(linkScheduleService).delete(99L);

    mockMvc.perform(delete("/api/link-schedules/99").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "REPORTER")
  void delete_asReporter_returns403() throws Exception {
    mockMvc.perform(delete("/api/link-schedules/1").with(csrf()))
        .andExpect(status().isForbidden());
  }
}
