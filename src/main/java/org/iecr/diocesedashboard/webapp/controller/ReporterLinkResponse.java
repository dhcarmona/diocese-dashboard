package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ReporterLink;

/**
 * Minimal API response for a reporter link.
 */
public record ReporterLinkResponse(
Long id,
String token,
Long reporterId,
String reporterUsername,
String churchName,
Long serviceTemplateId,
String serviceTemplateName) {

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
        reporterLink.getChurch().getName(),
        reporterLink.getServiceTemplate().getId(),
        reporterLink.getServiceTemplate().getServiceTemplateName());
  }
}
