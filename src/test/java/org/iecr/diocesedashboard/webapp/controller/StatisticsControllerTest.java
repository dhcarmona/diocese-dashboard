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
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.ChurchService;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
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
  private ServiceInfoItemService serviceInfoItemService;

  @MockBean
  private UserDetailsService userDetailsService;

  private ServiceTemplate template;
  private Church church;
  private StatisticsResponse fakeResponse;
  private ServiceInfoItem numericalItem;

  @BeforeEach
  void setUp() {
    objectMapper.registerModule(new JavaTimeModule());

    template = new ServiceTemplate();
    template.setId(1L);
    template.setServiceTemplateName("Sunday Mass");

    church = new Church();
    church.setName("Trinity");

    numericalItem = new ServiceInfoItem();
    numericalItem.setId(10L);
    numericalItem.setTitle("Attendance");
    numericalItem.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    numericalItem.setServiceTemplate(template);

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
        .andExpect(jsonPath("$[1].linkOnly").value(true))
        .andExpect(jsonPath("$[0].serviceInfoItems").doesNotExist())
        .andExpect(jsonPath("$[0].sectionHeaders").doesNotExist());
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

  // --- GET /api/statistics/drill-down ---

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getDrillDown_admin_churchScoped_returns200WithRows() throws Exception {
    ChartDrillDownResponse drillDown = new ChartDrillDownResponse(
        10L, "Attendance", "NUMERICAL", LocalDate.of(2024, 3, 10),
        List.of(new ChartDrillDownResponse.DrillDownRow(42L, "Trinity", "alice", "Alice", 30.0)));

    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.findById(10L)).thenReturn(Optional.of(numericalItem));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));
    when(statisticsService.computeDrillDown(eq(template), eq(numericalItem),
        eq(LocalDate.of(2024, 3, 10)), eq(church))).thenReturn(drillDown);

    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "10")
        .param("date", "2024-03-10")
        .param("churchName", "Trinity"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.itemId").value(10))
        .andExpect(jsonPath("$.itemTitle").value("Attendance"))
        .andExpect(jsonPath("$.itemType").value("NUMERICAL"))
        .andExpect(jsonPath("$.date").value("2024-03-10"))
        .andExpect(jsonPath("$.rows.length()").value(1))
        .andExpect(jsonPath("$.rows[0].serviceInstanceId").value(42))
        .andExpect(jsonPath("$.rows[0].churchName").value("Trinity"))
        .andExpect(jsonPath("$.rows[0].filledByUsername").value("alice"))
        .andExpect(jsonPath("$.rows[0].value").value(30.0));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getDrillDown_admin_global_returns200() throws Exception {
    ChartDrillDownResponse drillDown = new ChartDrillDownResponse(
        10L, "Attendance", "NUMERICAL", LocalDate.of(2024, 3, 10), List.of());

    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.findById(10L)).thenReturn(Optional.of(numericalItem));
    when(statisticsService.computeDrillDown(eq(template), eq(numericalItem),
        eq(LocalDate.of(2024, 3, 10)), eq(null))).thenReturn(drillDown);

    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "10")
        .param("date", "2024-03-10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.rows.length()").value(0));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void getDrillDown_reporter_returns403() throws Exception {
    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "10")
        .param("date", "2024-03-10"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getDrillDown_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "10")
        .param("date", "2024-03-10"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getDrillDown_unknownTemplate_returns404() throws Exception {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "99")
        .param("itemId", "10")
        .param("date", "2024-03-10"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getDrillDown_unknownItem_returns404() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "99")
        .param("date", "2024-03-10"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getDrillDown_itemFromDifferentTemplate_returns400() throws Exception {
    ServiceTemplate otherTemplate = new ServiceTemplate();
    otherTemplate.setId(99L);

    ServiceInfoItem foreignItem = new ServiceInfoItem();
    foreignItem.setId(10L);
    foreignItem.setServiceTemplate(otherTemplate);

    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.findById(10L)).thenReturn(Optional.of(foreignItem));

    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "10")
        .param("date", "2024-03-10"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.ADMIN)
  void getDrillDown_unknownChurch_returns404() throws Exception {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(serviceInfoItemService.findById(10L)).thenReturn(Optional.of(numericalItem));
    when(churchService.findById("Unknown")).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/statistics/drill-down")
        .param("templateId", "1")
        .param("itemId", "10")
        .param("date", "2024-03-10")
        .param("churchName", "Unknown"))
        .andExpect(status().isNotFound());
  }
}
