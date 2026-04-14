package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ReporterLink;
import org.iecr.diocesedashboard.domain.objects.ServiceInstance;

import java.time.LocalDate;
import java.util.Optional;

/**
 * API response for a completed report submission, including the next pending reporter link.
 */
public record ReportSubmissionResponse(
Long serviceInstanceId,
String nextReporterLinkToken,
String nextReporterLinkFollowUpToken,
LocalDate nextReporterLinkActiveDate) {

  /**
   * Creates a response DTO for an authenticated flow that may expose the next reporter-link token.
   *
   * @param serviceInstance the created service instance
   * @param nextReporterLink the next pending reporter link, if one exists
   * @return the serialized response payload
   */
  public static ReportSubmissionResponse fromAuthenticated(ServiceInstance serviceInstance,
      Optional<ReporterLink> nextReporterLink) {
    return new ReportSubmissionResponse(
        serviceInstance.getId(),
        nextReporterLink.map(ReporterLink::getToken).orElse(null),
        null,
        nextReporterLink.map(ReporterLink::getActiveDate).orElse(null));
  }

  /**
   * Creates a response DTO for the public flow using a short-lived follow-up token instead of
   * exposing the next reporter-link bearer token directly.
   *
   * @param serviceInstance the created service instance
   * @param nextReporterLink the next pending reporter link, if one exists
   * @param followUpToken the short-lived token for resolving the next pending link
   * @return the serialized response payload
   */
  public static ReportSubmissionResponse fromPublic(ServiceInstance serviceInstance,
      Optional<ReporterLink> nextReporterLink, String followUpToken) {
    return new ReportSubmissionResponse(
        serviceInstance.getId(),
        null,
        followUpToken,
        nextReporterLink.map(ReporterLink::getActiveDate).orElse(null));
  }
}
