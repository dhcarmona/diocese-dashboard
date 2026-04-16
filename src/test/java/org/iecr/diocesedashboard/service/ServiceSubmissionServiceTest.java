package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.DashboardUser;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.domain.objects.UserRole;
import org.iecr.diocesedashboard.webapp.controller.ServiceInstanceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ServiceSubmissionServiceTest {

  @Mock
  private ServiceTemplateService serviceTemplateService;
  @Mock
  private ServiceInstanceService serviceInstanceService;
  @Mock
  private ChurchService churchService;
  @Mock
  private CelebrantService celebrantService;
  @Mock
  private ServiceInfoItemService serviceInfoItemService;
  @Mock
  private ServiceInfoItemResponseService responseService;
  @Mock
  private WhatsAppService whatsAppService;
  @Mock
  private MessageSource messageSource;

  @InjectMocks
  private ServiceSubmissionService serviceSubmissionService;

  private ServiceTemplate buildTemplate() {
    ServiceTemplate t = new ServiceTemplate();
    t.setId(1L);
    t.setServiceTemplateName("Sunday Mass");
    return t;
  }

  private Church buildChurch() {
    Church c = new Church();
    c.setName("Trinity");
    c.setLocation("San José");
    return c;
  }

  private Celebrant buildCelebrant(Long id) {
    Celebrant c = new Celebrant();
    c.setId(id);
    c.setName("Fr. John");
    return c;
  }

  private ServiceInfoItem buildInfoItem(Long id) {
    ServiceInfoItem item = new ServiceInfoItem();
    item.setId(id);
    item.setTitle("attendance");
    item.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    return item;
  }

  private DashboardUser buildReporter(String phone) {
    DashboardUser user = new DashboardUser();
    user.setId(42L);
    user.setUsername("reporter1");
    user.setRole(UserRole.REPORTER);
    user.setPhoneNumber(phone);
    return user;
  }

  @Test
  void submit_validRequest_createsInstanceAndResponses() {
    ServiceTemplate template = buildTemplate();
    Church church = buildChurch();
    Celebrant celebrant = buildCelebrant(10L);
    ServiceInfoItem infoItem = buildInfoItem(5L);
    infoItem.setServiceTemplate(template);

    ServiceInstance savedInstance = new ServiceInstance();
    savedInstance.setId(99L);

    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(template));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(church));
    when(celebrantService.findById(10L)).thenReturn(Optional.of(celebrant));
    when(serviceInstanceService.save(any(ServiceInstance.class))).thenReturn(savedInstance);
    when(serviceInfoItemService.findById(5L)).thenReturn(Optional.of(infoItem));

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity",
        List.of(10L),
        LocalDate.of(2024, 1, 14),
        List.of(new ServiceInstanceRequest.ResponseEntry(5L, "120"))
    );

    ServiceInstance result = serviceSubmissionService.submit(1L, request, null);

    assertThat(result.getId()).isEqualTo(99L);
    verify(serviceInstanceService).save(any(ServiceInstance.class));
    verify(responseService).save(any());
  }

  @Test
  void submit_withNoCelebrants_createsInstanceWithoutCelebrants() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    ServiceInstance saved = new ServiceInstance();
    saved.setId(1L);
    when(serviceInstanceService.save(any())).thenReturn(saved);

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.of(2024, 1, 14), List.of());

    ServiceInstance result = serviceSubmissionService.submit(1L, request, null);

    assertThat(result).isNotNull();
    verify(celebrantService, times(0)).findById(any());
  }

  @Test
  void submit_templateNotFound_throwsResponseStatusException() {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.now(), List.of());

    assertThatThrownBy(() -> serviceSubmissionService.submit(99L, request, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Template not found");
  }

  @Test
  void submit_churchNotFound_throwsResponseStatusException() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Ghost")).thenReturn(Optional.empty());

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Ghost", List.of(), LocalDate.now(), List.of());

    assertThatThrownBy(() -> serviceSubmissionService.submit(1L, request, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Church not found");
  }

  @Test
  void submit_celebrantNotFound_throwsResponseStatusException() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    when(celebrantService.findById(999L)).thenReturn(Optional.empty());

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(999L), LocalDate.now(), List.of());

    assertThatThrownBy(() -> serviceSubmissionService.submit(1L, request, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Celebrant not found");
  }

  @Test
  void submit_infoItemNotFound_throwsResponseStatusException() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    ServiceInstance saved = new ServiceInstance();
    saved.setId(1L);
    when(serviceInstanceService.save(any())).thenReturn(saved);
    when(serviceInfoItemService.findById(888L)).thenReturn(Optional.empty());

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.now(),
        List.of(new ServiceInstanceRequest.ResponseEntry(888L, "val")));

    assertThatThrownBy(() -> serviceSubmissionService.submit(1L, request, null))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("ServiceInfoItem not found");
  }

  @Test
  void submit_withReporterPhone_sendsWhatsAppNotification() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    ServiceInstance saved = new ServiceInstance();
    saved.setId(1L);
    when(serviceInstanceService.save(any())).thenReturn(saved);
    when(messageSource.getMessage(eq("whatsapp.report.submitted"), any(), any(Locale.class)))
        .thenReturn("Confirmation message");

    DashboardUser reporter = buildReporter("+50688887777");
    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.of(2024, 3, 10), List.of());

    serviceSubmissionService.submit(1L, request, reporter);

    verify(whatsAppService).sendConfiguredMessageAndLog(
        eq("+50688887777"),
        eq("Confirmation message"),
        eq("reporter1"),
        any(),
        eq(WhatsAppService.TemplateType.REPORT_SUBMITTED),
        any());
  }

  @Test
  void submit_usesReporterPreferredLanguageForNotification() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    ServiceInstance saved = new ServiceInstance();
    saved.setId(1L);
    when(serviceInstanceService.save(any())).thenReturn(saved);
    when(messageSource.getMessage(eq("whatsapp.report.submitted"), any(), any(Locale.class)))
        .thenReturn("Confirmation message");

    DashboardUser reporter = buildReporter("+50688887777");
    reporter.setPreferredLanguage("en");
    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.of(2024, 3, 10), List.of());

    serviceSubmissionService.submit(1L, request, reporter);

    verify(messageSource).getMessage(
        eq("whatsapp.report.submitted"), any(), eq(Locale.ENGLISH));
  }

  @Test
  void submit_withNoPhone_doesNotSendWhatsApp() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    ServiceInstance saved = new ServiceInstance();
    saved.setId(1L);
    when(serviceInstanceService.save(any())).thenReturn(saved);

    DashboardUser reporter = buildReporter(null);
    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.of(2024, 3, 10), List.of());

    serviceSubmissionService.submit(1L, request, reporter);

    verify(whatsAppService, never()).sendConfiguredMessageAndLog(
        any(), any(), any(String.class), any(), any(), any());
  }

  @Test
  void submit_withinTransaction_sendsNotificationOnlyAfterCommit() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Trinity")).thenReturn(Optional.of(buildChurch()));
    ServiceInstance saved = new ServiceInstance();
    saved.setId(1L);
    when(serviceInstanceService.save(any())).thenReturn(saved);
    when(messageSource.getMessage(eq("whatsapp.report.submitted"), any(), any(Locale.class)))
        .thenReturn("Confirmation message");

    DashboardUser reporter = buildReporter("+50688887777");
    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.of(2024, 3, 10), List.of());

    TransactionSynchronizationManager.initSynchronization();
    try {
      serviceSubmissionService.submit(1L, request, reporter);

      verify(whatsAppService, never()).sendConfiguredMessageAndLog(
          any(), any(), any(String.class), any(), any(), any());

      TransactionSynchronizationManager.getSynchronizations()
          .forEach(TransactionSynchronization::afterCommit);

      verify(whatsAppService).sendConfiguredMessageAndLog(
          eq("+50688887777"),
          eq("Confirmation message"),
          eq("reporter1"),
          any(),
          eq(WhatsAppService.TemplateType.REPORT_SUBMITTED),
          any());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}
