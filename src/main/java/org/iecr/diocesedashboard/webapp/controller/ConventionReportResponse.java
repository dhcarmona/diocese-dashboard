package org.iecr.diocesedashboard.webapp.controller;

import java.time.LocalDate;
import java.util.List;

/**
 * Aggregated Convention Report: for every church, the sum of each numerical/money item's
 * responses across all Sundays in the given date range, for a single service template.
 */
public record ConventionReportResponse(
long templateId,
String templateName,
LocalDate startDate,
LocalDate endDate,
List<ItemColumn> columns,
List<ChurchRow> rows,
List<DuplicateReportNote> duplicateNotes) {

  /** A numerical/money item that becomes a column in the report table. */
  public record ItemColumn(long itemId, String itemTitle, String itemType) {
  }

  /**
   * One row of the report: a church, the number of (deduplicated) reports counted for it,
   * and its accumulated totals, aligned with {@code columns}.
   */
  public record ChurchRow(String churchName, int reportCount, List<Double> values) {
  }

  /** Notes that a church had more than one report for a given Sunday; only the first was used. */
  public record DuplicateReportNote(String churchName, LocalDate serviceDate, int reportCount) {
  }
}
