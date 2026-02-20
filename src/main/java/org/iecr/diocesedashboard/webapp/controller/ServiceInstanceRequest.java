package org.iecr.diocesedashboard.webapp.controller;

import java.time.LocalDate;
import java.util.List;

/**
 * Request body for submitting a new Service Instance against a Service Template.
 */
public record ServiceInstanceRequest(
String churchName,
List<Long> celebrantIds,
LocalDate serviceDate,
List<ResponseEntry> responses
) {

  /**
   * A single answer to one Service Info Item question.
   */
  public record ResponseEntry(Long serviceInfoItemId, String responseValue) {
  }
}
