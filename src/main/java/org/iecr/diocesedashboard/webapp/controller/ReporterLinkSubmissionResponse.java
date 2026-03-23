package org.iecr.diocesedashboard.webapp.controller;

import org.iecr.diocesedashboard.domain.objects.ServiceInstance;

/**
 * Minimal API response for a reporter-link submission.
 */
public record ReporterLinkSubmissionResponse(Long serviceInstanceId) {

  /**
   * Creates a response DTO from a newly created {@link ServiceInstance}.
   *
   * @param serviceInstance the created service instance
   * @return the serialized response payload
   */
  public static ReporterLinkSubmissionResponse from(ServiceInstance serviceInstance) {
    return new ReporterLinkSubmissionResponse(serviceInstance.getId());
  }
}
