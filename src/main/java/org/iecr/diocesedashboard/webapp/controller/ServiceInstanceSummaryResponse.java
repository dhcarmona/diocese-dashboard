package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** A lightweight projection of a ServiceInstance for list views. */
public record ServiceInstanceSummaryResponse(
Long id,
LocalDate serviceDate,
String churchName,
Long templateId,
String templateName,
String submittedByUsername,
String submittedByFullName,
LocalDateTime submittedAt) {

  /**
   * Builds a summary response from the given entity.
   *
   * @param instance      the service instance
   * @param includeSubmittedAt whether to populate the submittedAt field (admin only)
   * @return the summary response
   */
  public static ServiceInstanceSummaryResponse from(
      ServiceInstance instance, boolean includeSubmittedAt) {
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
        fullName,
        includeSubmittedAt ? instance.getSubmittedAt() : null);
  }
}
