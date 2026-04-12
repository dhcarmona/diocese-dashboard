package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.Celebrant;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemResponse;
import org.iecr.diocesedashboard.domain.objects.ServiceInfoItemType;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Full detail projection of a ServiceInstance including all survey responses. */
public record ServiceInstanceDetailResponse(
Long id,
LocalDate serviceDate,
String churchName,
Long templateId,
String templateName,
String submittedByUsername,
String submittedByFullName,
List<CelebrantInfo> celebrants,
List<ResponseDetail> responses) {

  /** Minimal celebrant projection used inside an instance detail. */
  public record CelebrantInfo(Long id, String name) {
  }

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
    Set<Celebrant> celebrantSet = instance.getCelebrants();
    List<CelebrantInfo> celebrantInfos = celebrantSet == null ? Collections.emptyList()
        : celebrantSet.stream()
            .map(c -> new CelebrantInfo(c.getId(), c.getName()))
            .sorted(Comparator.comparing(CelebrantInfo::name))
            .toList();
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
        celebrantInfos,
        details);
  }
}
