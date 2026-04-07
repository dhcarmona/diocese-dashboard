package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ReporterLink;

import java.time.LocalDate;

/**
 * Minimal API response for a reporter link.
 */
public record ReporterLinkResponse(
Long id,
String token,
Long reporterId,
String reporterUsername,
String reporterFullName,
String churchName,
Long serviceTemplateId,
String serviceTemplateName,
LocalDate activeDate) {

  /**
   * Creates a response DTO from a {@link ReporterLink}.
   *
   * @param reporterLink the persisted reporter link
   * @return the serialized response payload
   */
  public static ReporterLinkResponse from(ReporterLink reporterLink) {
    return new ReporterLinkResponse(
        reporterLink.getId(),
        reporterLink.getToken(),
        reporterLink.getReporter().getId(),
        reporterLink.getReporter().getUsername(),
        reporterLink.getReporter().getFullName(),
        reporterLink.getChurch().getName(),
        reporterLink.getServiceTemplate().getId(),
        reporterLink.getServiceTemplate().getServiceTemplateName(),
        reporterLink.getActiveDate());
  }
}
