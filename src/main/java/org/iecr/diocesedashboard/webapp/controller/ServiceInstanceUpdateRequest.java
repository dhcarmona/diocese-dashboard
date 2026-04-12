package org.iecr.diocesedashboard.webapp.controller;

import java.util.List;

/**
 * Request body for updating an existing ServiceInstance. Supports updating survey responses,
 * the list of celebrants (optional — omit or set to null to leave unchanged), and whether
 * to notify the reporter via WhatsApp.
 */
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
