package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ReporterLink;

import java.time.LocalDate;

/**
 * API response for resolving the next pending public reporter link.
 */
public record ReporterLinkFollowUpResponse(
String nextReporterLinkToken,
LocalDate nextReporterLinkActiveDate) {

  /**
   * Creates a response DTO from the next pending {@link ReporterLink}.
   *
   * @param reporterLink the next pending reporter link
   * @return the serialized response payload
   */
  public static ReporterLinkFollowUpResponse from(ReporterLink reporterLink) {
    return new ReporterLinkFollowUpResponse(
        reporterLink.getToken(),
        reporterLink.getActiveDate());
  }
}
