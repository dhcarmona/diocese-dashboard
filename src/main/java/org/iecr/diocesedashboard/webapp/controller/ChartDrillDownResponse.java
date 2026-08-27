package org.iecr.diocesedashboard.webapp.controller;

import java.time.LocalDate;
import java.util.List;

/**
 * Drill-down detail for a single bar in a statistics chart.
 * Contains the individual service instance contributions for a specific item on a given date.
 */
public record ChartDrillDownResponse(
long itemId,
String itemTitle,
String itemType,
LocalDate date,
List<DrillDownRow> rows) {

  /** One row per service instance that contributed a non-zero value. */
  public record DrillDownRow(
  long serviceInstanceId,
  String churchName,
  String filledByUsername,
  String filledByFullName,
  double value) {
  }
}
