package org.iecr.diocesedashboard.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.Church;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItem;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;
import org.iecr.diocesedashboard.domain.objects.ServiceTemplate;
import org.iecr.diocesedashboard.webapp.controller.ServiceInstanceRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
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
    item.setQuestionId("attendance");
    item.setServiceInfoItemType(ServiceInfoItemType.NUMERICAL);
    return item;
  }

  @Test
  void submit_validRequest_createsInstanceAndResponses() {
    ServiceTemplate template = buildTemplate();
    Church church = buildChurch();
    Celebrant celebrant = buildCelebrant(10L);
    ServiceInfoItem infoItem = buildInfoItem(5L);

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

    ServiceInstance result = serviceSubmissionService.submit(1L, request);

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

    ServiceInstance result = serviceSubmissionService.submit(1L, request);

    assertThat(result).isNotNull();
    verify(celebrantService, times(0)).findById(any());
  }

  @Test
  void submit_templateNotFound_throwsResponseStatusException() {
    when(serviceTemplateService.findById(99L)).thenReturn(Optional.empty());

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Trinity", List.of(), LocalDate.now(), List.of());

    assertThatThrownBy(() -> serviceSubmissionService.submit(99L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Template not found");
  }

  @Test
  void submit_churchNotFound_throwsResponseStatusException() {
    when(serviceTemplateService.findById(1L)).thenReturn(Optional.of(buildTemplate()));
    when(churchService.findById("Ghost")).thenReturn(Optional.empty());

    ServiceInstanceRequest request = new ServiceInstanceRequest(
        "Ghost", List.of(), LocalDate.now(), List.of());

    assertThatThrownBy(() -> serviceSubmissionService.submit(1L, request))
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

    assertThatThrownBy(() -> serviceSubmissionService.submit(1L, request))
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

    assertThatThrownBy(() -> serviceSubmissionService.submit(1L, request))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("ServiceInfoItem not found");
  }
}
