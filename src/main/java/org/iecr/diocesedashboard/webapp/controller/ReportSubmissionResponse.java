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
LocalDate nextReporterLinkActiveDate) {

  /**
   * Creates a response DTO from a newly created {@link ServiceInstance}.
   *
   * @param serviceInstance the created service instance
   * @param nextReporterLink the next pending reporter link, if one exists
   * @return the serialized response payload
   */
  public static ReportSubmissionResponse from(ServiceInstance serviceInstance,
      Optional<ReporterLink> nextReporterLink) {
    return new ReportSubmissionResponse(
        serviceInstance.getId(),
        nextReporterLink.map(ReporterLink::getToken).orElse(null),
        nextReporterLink.map(ReporterLink::getActiveDate).orElse(null));
  }
}
