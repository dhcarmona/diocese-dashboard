package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ConventionReportService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
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

@WebMvcTest(ConventionReportController.class)
@Import(SecurityConfig.class)
class ConventionReportControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ConventionReportService conventionReportService;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private UserDetailsService userDetailsService;

  private ServiceTemplate template;
  private ConventionReportResponse fakeResponse;

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());

    template = new ServiceTemplate();
    template.setId(1L);
    template.setServiceTemplateName("Sunday Mass");

    fakeResponse = new ConventionReportResponse(
        1L, "Sunday Mass",
        LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31),
        List.of(), List.of(), List.of());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getConventionReport_admin_returns200WithData() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(conventionReportService.compute(eq(template), eq(LocalDate.of(2024, 1, 1)),
        eq(LocalDate.of(2024, 1, 31)))).thenReturn(fakeResponse);

    mockMvc.perform(get("/api/convention-report")
        .param("templateId", "1")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-01-31"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.templateName").value("Sunday Mass"));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void getConventionReport_reporter_returns403() throws Exception {
    mockMvc.perform(get("/api/convention-report")
        .param("templateId", "1")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-01-31"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getConventionReport_endDateBeforeStartDate_returns400() throws Exception {
    mockMvc.perform(get("/api/convention-report")
        .param("templateId", "1")
        .param("startDate", "2024-01-31")
        .param("endDate", "2024-01-01"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getConventionReport_templateNotFound_returns404() throws Exception {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/convention-report")
        .param("templateId", "99")
        .param("startDate", "2024-01-01")
        .param("endDate", "2024-01-31"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getTemplates_admin_returns200() throws Exception {
    when(serviceTemplateService.findAll()).thenReturn(List.of(template));

    mockMvc.perform(get("/api/convention-report/templates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].serviceTemplateName").value("Sunday Mass"));
  }
}
