package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;

import java.time.LocalDate;

/** A lightweight projection of a ServiceInstance for list views. */
public record ServiceInstanceSummaryResponse(
Long id,
LocalDate serviceDate,
String churchName,
Long templateId,
String templateName,
String submittedByUsername,
String submittedByFullName) {

  /** Builds a summary response from the given entity. */
  public static ServiceInstanceSummaryResponse from(ServiceInstance instance) {
    String username = instance.getSubmittedBy() != null
        ? instance.getSubmittedBy().getUsername() : null;
    String fullName = instance.getSubmittedBy() != null
        ? instance.getSubmittedBy().getFullName() : null;
    return new ServiceInstanceSummaryResponse(
        instance.getId(),
        instance.getServiceDate(),
        instance.getChurch() != null ? instance.getChurch().getName() : null,
        instance.getServiceTemplate() != null ? instance.getServiceTemplate().getId() : null,
        instance.getServiceTemplate() != null
            ? instance.getServiceTemplate().getServiceTemplateName() : null,
        username,
        fullName);
  }
}
