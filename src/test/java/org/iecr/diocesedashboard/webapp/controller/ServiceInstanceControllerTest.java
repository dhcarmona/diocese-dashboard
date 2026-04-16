package org.iecr.diocesedashboard.webapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.service.CelebrantService;
import org.iecr.diocesedashboard.service.ServiceInfoItemResponseService;
import org.iecr.diocesedashboard.service.ServiceInfoItemService;
import org.iecr.diocesedashboard.service.ServiceInstanceService;
import org.iecr.diocesedashboard.service.ServiceTemplateService;
import org.iecr.diocesedashboard.service.WhatsAppService;
import org.iecr.diocesedashboard.webapp.SecurityConfig;
import org.iecr.diocesedashboard.webapp.WithMockDashboardUser;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@WebMvcTest(ServiceInstanceController.class)
@Import(SecurityConfig.class)
class ServiceInstanceControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ServiceInstanceService serviceInstanceService;

  @MockBean
  private ServiceInfoItemResponseService responseService;

  @MockBean
  private ServiceInfoItemService serviceInfoItemService;

  @MockBean
  private ServiceTemplateService serviceTemplateService;

  @MockBean
  private CelebrantService celebrantService;

  @MockBean
  private WhatsAppService whatsAppService;

  @MockBean
  private MessageSource messageSource;

  @MockBean
  private UserDetailsService userDetailsService;

  private ServiceInstance buildInstance(Long id) {
    ServiceInstance i = new ServiceInstance();
    i.setId(id);
    return i;
  }

  private ServiceInstance buildInstanceForChurch(Long id, String churchName) {
    Church church = new Church();
    church.setName(churchName);
    ServiceInstance i = new ServiceInstance();
    i.setId(id);
    i.setChurch(church);
    return i;
  }

  private ServiceInstance buildFullInstance(Long id, String churchName,
      String templateName, String reporterPhone) {
    Church church = new Church();
    church.setName(churchName);
    ServiceTemplate template = new ServiceTemplate();
    template.setId(10L);
    template.setServiceTemplateName(templateName);
    DashboardUser reporter = new DashboardUser();
    reporter.setId(99L);
    reporter.setUsername("reporter1");
    reporter.setFullName("Reporter One");
    reporter.setPhoneNumber(reporterPhone);
    reporter.setPreferredLanguage("es");
    reporter.setRole(UserRole.REPORTER);
    ServiceInstance i = new ServiceInstance();
    i.setId(id);
    i.setChurch(church);
    i.setServiceTemplate(template);
    i.setServiceDate(LocalDate.of(2026, 1, 15));
    i.setSubmittedBy(reporter);
    return i;
  }

  private ServiceInfoItem buildItem(Long id, String title) {
    ServiceInfoItem item = new ServiceInfoItem();
    item.setId(id);
    item.setTitle(title);
    item.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    item.setRequired(true);
    return item;
  }

  private ServiceInfoItemResponse buildResponse(Long id, ServiceInfoItem item,
      ServiceInstance instance, String value) {
    ServiceInfoItemResponse response = new ServiceInfoItemResponse();
    response.setId(id);
    response.setServiceInfoItem(item);
    response.setServiceInstance(instance);
    response.setResponseValue(value);
    return response;
  }

  // --- GET /api/service-instances ---

  @Test
  @WithMockDashboardUser
  void getAll_asAdmin_submittedAtIsIncluded() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", null);
    instance.setSubmittedAt(LocalDateTime.of(2026, 1, 15, 10, 30));
    when(serviceInstanceService.findAll()).thenReturn(List.of(instance));

    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submittedAt").value("2026-01-15T10:30:00"));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getAll_asReporter_submittedAtIsNull() throws Exception {
    ServiceInstance instance = buildInstanceForChurch(1L, "Trinity");
    instance.setSubmittedAt(LocalDateTime.of(2026, 1, 15, 10, 30));
    when(serviceInstanceService.findByChurches(any())).thenReturn(List.of(instance));

    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].submittedAt").doesNotExist());
  }

  @Test
  @WithMockDashboardUser
  void getAll_asAdmin_returns200WithList() throws Exception {
    when(serviceInstanceService.findAll()).thenReturn(
        List.of(buildInstance(1L), buildInstance(2L)));

    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockUser(roles = "USER")
  void getAll_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getAll_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getAll_asReporter_returnsOnlyOwnChurchInstances() throws Exception {
    when(serviceInstanceService.findByChurches(any())).thenReturn(
        List.of(buildInstanceForChurch(1L, "Trinity")));

    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].id").value(1));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchNames = {"StPaul", "Trinity"})
  void getAll_asMultiChurchReporter_returnsAssignedChurchInstances() throws Exception {
    when(serviceInstanceService.findByChurches(any())).thenReturn(
        List.of(buildInstanceForChurch(1L, "Trinity"),
            buildInstanceForChurch(2L, "StPaul")));

    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER)
  void getAll_asReporterWithNoAssignedChurches_returns403() throws Exception {
    mockMvc.perform(get("/api/service-instances"))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser
  void getAll_withTemplateId_asAdmin_returnsFilteredList() throws Exception {
    ServiceTemplate template = new ServiceTemplate();
    template.setId(10L);
    when(serviceTemplateService.findById(10L)).thenReturn(Optional.of(template));
    when(serviceInstanceService.findByServiceTemplate(template)).thenReturn(
        List.of(buildInstance(1L), buildInstance(2L)));

    mockMvc.perform(get("/api/service-instances").param("templateId", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2));
  }

  @Test
  @WithMockDashboardUser
  void getAll_withTemplateId_templateNotFound_returns404() throws Exception {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/service-instances").param("templateId", "99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getAll_withTemplateId_asReporter_returns403() throws Exception {
    mockMvc.perform(get("/api/service-instances").param("templateId", "10"))
        .andExpect(status().isForbidden());
  }

  // --- GET /api/service-instances/{id} ---

  @Test
  @WithMockDashboardUser
  void getById_exists_returns200WithAllFields() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse response = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findByIdWithCelebrants(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(response));

    mockMvc.perform(get("/api/service-instances/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.churchName").value("Trinity"))
        .andExpect(jsonPath("$.templateName").value("Sunday Mass"))
        .andExpect(jsonPath("$.submittedByUsername").value("reporter1"))
        .andExpect(jsonPath("$.responses.length()").value(1))
        .andExpect(jsonPath("$.responses[0].serviceInfoItemTitle").value("Attendance"))
        .andExpect(jsonPath("$.responses[0].responseValue").value("42"));
  }

  @Test
  @WithMockDashboardUser
  void getById_notFound_returns404() throws Exception {
    when(serviceInstanceService.findByIdWithCelebrants(99L)).thenReturn(Optional.empty());

    mockMvc.perform(get("/api/service-instances/99"))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "USER")
  void getById_asUser_returns403() throws Exception {
    mockMvc.perform(get("/api/service-instances/1"))
        .andExpect(status().isForbidden());
  }

  @Test
  void getById_unauthenticated_returns401() throws Exception {
    mockMvc.perform(get("/api/service-instances/1"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getById_asReporter_ownChurch_returns200() throws Exception {
    ServiceInstance instance = buildInstanceForChurch(1L, "Trinity");
    when(serviceInstanceService.findByIdWithCelebrants(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of());

    mockMvc.perform(get("/api/service-instances/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(1));
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void getById_asReporter_otherChurch_returns404() throws Exception {
    when(serviceInstanceService.findByIdWithCelebrants(2L))
        .thenReturn(Optional.of(buildInstanceForChurch(2L, "StPaul")));

    mockMvc.perform(get("/api/service-instances/2"))
        .andExpect(status().isNotFound());
  }

  // --- PUT /api/service-instances/{id} ---

  @Test
  @WithMockDashboardUser
  void update_notFound_returns404() throws Exception {
    when(serviceInstanceService.findById(99L)).thenReturn(Optional.empty());
    String body = objectMapper.writeValueAsString(
        new ServiceInstanceUpdateRequest(List.of(), null, false));

    mockMvc.perform(put("/api/service-instances/99")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockDashboardUser
  void update_noChanges_returns200WithoutSendingNotification() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse existing = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(existing));
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(5L, "42")), null, true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(any(), any(), any(), any(),
        any(), any());
  }

  @Test
  @WithMockDashboardUser
  void update_withChanges_savesUpdatedResponse() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse existing = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(existing));
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(5L, "55")), null, false));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(responseService).save(existing);
    verify(whatsAppService, never()).sendConfiguredMessageAndLog(any(), any(), any(), any(),
        any(), any());
  }

  @Test
  @WithMockDashboardUser
  void update_withChangesAndNotifyTrue_sendsWhatsApp() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse existing = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(existing));
    when(messageSource.getMessage(
        eq("whatsapp.report.updated"), any(Object[].class), any(Locale.class)))
        .thenReturn("cambio en reporte");
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(5L, "55")), null, true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(whatsAppService).sendConfiguredMessageAndLog(
        eq("+50612345678"),
        eq("cambio en reporte"),
        any(),
        any(),
        eq(WhatsAppService.TemplateType.REPORT_UPDATED),
        any());
  }

  @Test
  @WithMockDashboardUser
  void update_withChangesAndNotifyTrue_usesReporterPreferredLanguage() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    instance.getSubmittedBy().setPreferredLanguage("en");
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse existing = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(existing));
    when(messageSource.getMessage(
        eq("whatsapp.report.updated"), any(Object[].class), any(Locale.class)))
        .thenReturn("updated report");
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(5L, "55")), null, true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(messageSource).getMessage(
        eq("whatsapp.report.updated"), any(Object[].class), eq(Locale.ENGLISH));
  }

  @Test
  @WithMockDashboardUser
  void update_withNotifyTrue_noPhoneNumber_doesNotSendWhatsApp() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", null);
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse existing = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(existing));
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(5L, "55")), null, true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(any(), any(), any(), any(),
        any(), any());
  }

  @Test
  @WithMockDashboardUser
  void update_celebrantsChanged_withNotifyTrue_sendsWhatsApp() throws Exception {
    Celebrant oldCelebrant = new Celebrant();
    oldCelebrant.setId(10L);
    oldCelebrant.setName("Fr. Old");
    Celebrant newCelebrant = new Celebrant();
    newCelebrant.setId(20L);
    newCelebrant.setName("Fr. New");
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    instance.setCelebrants(new HashSet<>(Set.of(oldCelebrant)));
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of());
    when(celebrantService.findAllById(List.of(20L))).thenReturn(List.of(newCelebrant));
    when(messageSource.getMessage(
        eq("whatsapp.report.updated"), any(Object[].class), any(Locale.class)))
        .thenReturn("cambio en reporte");
    String body = objectMapper.writeValueAsString(
        new ServiceInstanceUpdateRequest(List.of(), List.of(20L), true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(whatsAppService).sendConfiguredMessageAndLog(
        eq("+50612345678"),
        eq("cambio en reporte"),
        any(),
        any(),
        eq(WhatsAppService.TemplateType.REPORT_UPDATED),
        any());
  }

  @Test
  @WithMockDashboardUser
  void update_celebrantsUnchanged_withNotifyTrue_doesNotSendWhatsApp() throws Exception {
    Celebrant celebrant = new Celebrant();
    celebrant.setId(10L);
    celebrant.setName("Fr. Same");
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    instance.setCelebrants(new HashSet<>(Set.of(celebrant)));
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of());
    when(celebrantService.findAllById(List.of(10L))).thenReturn(List.of(celebrant));
    String body = objectMapper.writeValueAsString(
        new ServiceInstanceUpdateRequest(List.of(), List.of(10L), true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(any(), any(), any(), any(),
        any(), any());
  }

  @Test
  @WithMockDashboardUser
  void update_unknownCelebrantId_returns400() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of());
    when(celebrantService.findAllById(List.of(99L))).thenReturn(List.of());
    String body = objectMapper.writeValueAsString(
        new ServiceInstanceUpdateRequest(List.of(), List.of(99L), false));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest());
  }

  @Test
  @WithMockUser(roles = "USER")
  void update_asUser_returns403() throws Exception {
    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void update_unauthenticated_returns401() throws Exception {
    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isUnauthorized());
  }

  // --- DELETE /api/service-instances/{id} ---

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_exists_returns204() throws Exception {
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(buildInstance(1L)));

    mockMvc.perform(delete("/api/service-instances/1").with(csrf()))
        .andExpect(status().isNoContent());

    verify(serviceInstanceService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_deletesResponsesBeforeInstance() throws Exception {
    ServiceInstance instance = buildInstance(1L);
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));

    mockMvc.perform(delete("/api/service-instances/1").with(csrf()))
        .andExpect(status().isNoContent());

    InOrder order = inOrder(responseService, serviceInstanceService);
    order.verify(responseService).deleteByServiceInstance(instance);
    order.verify(serviceInstanceService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_returns404() throws Exception {
    when(serviceInstanceService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(delete("/api/service-instances/99").with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_notFound_doesNotDeleteResponses() throws Exception {
    when(serviceInstanceService.findById(99L)).thenReturn(Optional.empty());

    mockMvc.perform(delete("/api/service-instances/99").with(csrf()))
        .andExpect(status().isNotFound());

    verify(responseService, never()).deleteByServiceInstance(any());
    verify(serviceInstanceService, never()).deleteById(any());
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_withNotify_sendsWhatsAppThenDeletes() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(messageSource.getMessage(
        eq("whatsapp.report.deleted"), any(Object[].class), any(Locale.class)))
        .thenReturn("reporte eliminado");

    mockMvc.perform(delete("/api/service-instances/1").with(csrf())
        .param("notify", "true"))
        .andExpect(status().isNoContent());

    InOrder order = inOrder(whatsAppService, responseService, serviceInstanceService);
    order.verify(whatsAppService).sendConfiguredMessageAndLog(
        eq("+50612345678"),
        eq("reporte eliminado"),
        any(),
        any(),
        eq(WhatsAppService.TemplateType.REPORT_DELETED),
        any());
    order.verify(responseService).deleteByServiceInstance(instance);
    order.verify(serviceInstanceService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_withNotify_usesReporterPreferredLanguage() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    instance.getSubmittedBy().setPreferredLanguage("en");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(messageSource.getMessage(
        eq("whatsapp.report.deleted"), any(Object[].class), any(Locale.class)))
        .thenReturn("deleted report");

    mockMvc.perform(delete("/api/service-instances/1").with(csrf())
        .param("notify", "true"))
        .andExpect(status().isNoContent());

    verify(messageSource).getMessage(
        eq("whatsapp.report.deleted"), any(Object[].class), eq(Locale.ENGLISH));
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_withNotify_noPhoneNumber_doesNotSendWhatsApp() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", null);
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));

    mockMvc.perform(delete("/api/service-instances/1").with(csrf())
        .param("notify", "true"))
        .andExpect(status().isNoContent());

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(any(), any(), any(), any(),
        any(), any());
    verify(responseService).deleteByServiceInstance(instance);
    verify(serviceInstanceService).deleteById(1L);
  }

  @Test
  @WithMockUser(roles = "ADMIN")
  void delete_withNotify_reporterIsNull_doesNotSendWhatsApp() throws Exception {
    ServiceInstance instance = buildInstance(1L);
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));

    mockMvc.perform(delete("/api/service-instances/1").with(csrf())
        .param("notify", "true"))
        .andExpect(status().isNoContent());

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(any(), any(), any(), any(),
        any(), any());
  }

  @Test
  @WithMockUser(roles = "USER")
  void delete_asUser_returns403() throws Exception {
    mockMvc.perform(delete("/api/service-instances/1").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  @WithMockDashboardUser(role = UserRole.REPORTER, churchName = "Trinity")
  void delete_asReporter_returns403() throws Exception {
    mockMvc.perform(delete("/api/service-instances/1").with(csrf()))
        .andExpect(status().isForbidden());
  }

  @Test
  void delete_unauthenticated_returns401() throws Exception {
    mockMvc.perform(delete("/api/service-instances/1").with(csrf()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @WithMockDashboardUser
  void update_newItemFromDifferentTemplate_returns400() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    ServiceTemplate otherTemplate = new ServiceTemplate();
    otherTemplate.setId(99L);
    ServiceInfoItem foreignItem = new ServiceInfoItem();
    foreignItem.setId(77L);
    foreignItem.setTitle("Foreign");
    foreignItem.setServiceTemplate(otherTemplate);
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of());
    when(serviceInfoItemService.findById(77L)).thenReturn(Optional.of(foreignItem));
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(77L, "newValue")), null, false));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isBadRequest());

    verify(responseService, never()).save(any());
  }

  @Test
  @WithMockDashboardUser
  void update_whatsAppFailure_stillReturns200() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    ServiceInfoItem item = buildItem(5L, "Attendance");
    ServiceInfoItemResponse existing = buildResponse(100L, item, instance, "42");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(responseService.findByServiceInstance(instance)).thenReturn(List.of(existing));
    when(messageSource.getMessage(
        eq("whatsapp.report.updated"), any(Object[].class), any(Locale.class)))
        .thenReturn("Updated message");
    doThrow(new RuntimeException("Twilio error"))
        .when(whatsAppService).sendConfiguredMessageAndLog(any(), any(), any(), any(), any(),
        any());
    String body = objectMapper.writeValueAsString(new ServiceInstanceUpdateRequest(
        List.of(new ServiceInstanceUpdateRequest.ResponseEntry(5L, "99")), null, true));

    mockMvc.perform(put("/api/service-instances/1")
        .with(csrf())
        .contentType(MediaType.APPLICATION_JSON)
        .content(body))
        .andExpect(status().isOk());

    verify(responseService).save(existing);
  }

  @Test
  @WithMockDashboardUser
  void delete_whatsAppFailure_stillDeletes() throws Exception {
    ServiceInstance instance = buildFullInstance(1L, "Trinity", "Sunday Mass", "+50612345678");
    when(serviceInstanceService.findById(1L)).thenReturn(Optional.of(instance));
    when(messageSource.getMessage(
        eq("whatsapp.report.deleted"), any(Object[].class), any(Locale.class)))
        .thenReturn("Deleted message");
    doThrow(new RuntimeException("Twilio error"))
        .when(whatsAppService).sendConfiguredMessageAndLog(any(), any(), any(), any(), any(),
        any());

    mockMvc.perform(delete("/api/service-instances/1")
        .with(csrf())
        .param("notify", "true"))
        .andExpect(status().isNoContent());

    verify(responseService).deleteByServiceInstance(instance);
    verify(serviceInstanceService).deleteById(1L);
  }
}
