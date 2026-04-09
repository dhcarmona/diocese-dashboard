package org.iecr.diocesedashboard.webapp.controller;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated statistics report for a service template, optionally scoped to a single church.
 */
public record StatisticsResponse(
long templateId,
String templateName,
String churchName,
boolean global,
LocalDate startDate,
LocalDate endDate,
int totalServiceCount,
List<CelebrantStat> celebrantStats,
List<AggregatedItem> numericalItems,
List<AggregatedItem> moneyItems,
List<PendingLink> pendingLinks) {

  /** Per-celebrant service count for the pie chart. */
  public record CelebrantStat(long celebrantId, String celebrantName, int serviceCount) {
  }

  /** Aggregated totals and time-series data for a single info item. */
  public record AggregatedItem(
  long itemId,
  String itemTitle,
  String itemType,
  double total,
  List<TimeSeriesPoint> timeSeriesData) {
  }

  /** A single data point in a time-series chart. */
  public record TimeSeriesPoint(LocalDate date, double value) {
  }

  /** A reporter link that has not yet been used (no matching submission exists). */
  public record PendingLink(
  String token,
  String reporterUsername,
  String reporterFullName,
  String churchName,
  LocalDate activeDate) {
  }
}
