package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ChurchService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.service.StatisticsService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.iecr.diocesedashboard.webapp.WithMockDashboardUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@WebMvcTest(StatisticsController.class)
@Import(SecurityConfig.class)
class StatisticsControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private StatisticsService statisticsService;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private ChurchService churchService;

  @MockBean
  private UserDetailsService userDetailsService;

  private ServiceTemplate template;
  private Church church;
  private StatisticsResponse fakeResponse;

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());

    template = new ServiceTemplate();
    template.setId(1L);
    template.setServiceTemplateName("Sunday Mass");

    church = new Church();
    church.setName("Trinity");

    fakeResponse = new StatisticsResponse(
        1L, "Sunday Mass", "Trinity", false,
        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
        5, List.of(), List.of(), List.of(), List.of());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getStatistics_admin_churchScoped_returns200WithData() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));
    when(statisticsService.computeForChurch(eq(template), eq(church), any(), any(), any()))
        .thenReturn(fakeResponse);

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("churchName", "Trinity")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.templateName").value("Sunday Mass"))
        .andExpect(jsonPath("$.churchName").value("Trinity"))
        .andExpect(jsonPath("$.totalServiceCount").value(5));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getStatistics_admin_globalReport_returns200() throws Exception {
    StatisticsResponse globalResponse = new StatisticsResponse(
        1L, "Sunday Mass", null, true,
        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31),
        10, List.of(), List.of(), List.of(), List.of());

    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(statisticsService.computeGlobal(eq(template), any(), any()))
        .thenReturn(globalResponse);

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.global").value(true))
        .andExpect(jsonPath("$.churchName").doesNotExist());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getStatistics_reporter_assignedChurch_returns200() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));
    when(statisticsService.computeForChurch(eq(template), eq(church), any(), any(), any()))
        .thenReturn(fakeResponse);

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("churchName", "Trinity")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isOk());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void getStatistics_reporter_globalRequest_returns403() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void getStatistics_reporter_unassignedChurch_returns403() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("churchName", "Trinity")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getStatistics_unknownTemplate_returns404() throws Exception {
    when(serviceTemplateService.findById(999L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "999")
        .param("churchName", "Trinity")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getStatistics_unknownChurch_returns404() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(churchService.findById("Unknown")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("churchName", "Unknown")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getStatistics_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/statistics")
        .param("templateId", "1")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-12-31"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void getTemplatesForStatistics_reporter_includesLinkOnlyTemplates() throws Exception {
    ServiceTemplate linkOnlyTemplate = new ServiceTemplate();
    linkOnlyTemplate.setId(2L);
    linkOnlyTemplate.setServiceTemplateName("Link Only Service");
    linkOnlyTemplate.setLinkOnly(true);

    when(serviceTemplateService.findAll()).thenReturn(List.of(template, linkOnlyTemplate));

    mockMvc.perform(get("/api/statistics/templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].serviceTemplateName").value("Sunday Mass"))
        .andExpect(jsonPath("$[1].serviceTemplateName").value("Link Only Service"))
        .andExpect(jsonPath("$[1].linkOnly").value(true));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getTemplatesForStatistics_admin_returnsAllTemplates() throws Exception {
    when(serviceTemplateService.findAll()).thenReturn(List.of(template));

    mockMvc.perform(get("/api/statistics/templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1));
  }

  @Test
  void getTemplatesForStatistics_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/statistics/templates"))
        .andExpect(status().isUnauthorized());
  }
}
