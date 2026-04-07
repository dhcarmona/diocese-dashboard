package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;

import java.time.LocalDate;
import java.util.List;

/** Full detail projection of a ServiceInstance including all survey responses. */
public record ServiceInstanceDetailResponse(
Long id,
LocalDate serviceDate,
String churchName,
Long templateId,
String templateName,
String submittedByUsername,
String submittedByFullName,
List<ResponseDetail> responses) {

  /** Detail for a single survey answer. */
  public record ResponseDetail(
  Long responseId,
  Long serviceInfoItemId,
  String serviceInfoItemTitle,
  String serviceInfoItemDescription,
  ServiceInfoItemType serviceInfoItemType,
  Boolean required,
  String responseValue) {
  }

  /** Builds the detail response from the entity and its loaded responses. */
  public static ServiceInstanceDetailResponse from(ServiceInstance instance,
      List<ServiceInfoItemResponse> responses) {
    String username = instance.getSubmittedBy() != null
        ? instance.getSubmittedBy().getUsername() : null;
    String fullName = instance.getSubmittedBy() != null
        ? instance.getSubmittedBy().getFullName() : null;
    List<ResponseDetail> details = responses.stream()
        .map(r -> new ResponseDetail(
            r.getId(),
            r.getServiceInfoItem().getId(),
            r.getServiceInfoItem().getTitle(),
            r.getServiceInfoItem().getDescription(),
            r.getServiceInfoItem().getServiceInfoItemType(),
            r.getServiceInfoItem().getRequired(),
            r.getResponseValue()))
        .toList();
    return new ServiceInstanceDetailResponse(
        instance.getId(),
        instance.getServiceDate(),
        instance.getChurch() != null ? instance.getChurch().getName() : null,
        instance.getServiceTemplate() != null ? instance.getServiceTemplate().getId() : null,
        instance.getServiceTemplate() != null
            ? instance.getServiceTemplate().getServiceTemplateName() : null,
        username,
        fullName,
        details);
  }
}
