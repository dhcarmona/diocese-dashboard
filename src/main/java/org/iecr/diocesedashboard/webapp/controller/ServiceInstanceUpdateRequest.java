package org.iecr.diocesedashboard.webapp.controller;

import java.util.List;

/** Request body for updating the responses of an existing ServiceInstance. */
public record ServiceInstanceUpdateRequest(
List<ResponseEntry> responses,
List<Long> celebrantIds,
boolean notifyReporter) {

  /** A single updated answer for one survey item. */
  public record ResponseEntry(
  Long serviceInfoItemId,
  String responseValue) {
  }
}
