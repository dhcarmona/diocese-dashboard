import * as XLSX from 'xlsx';
import type { ConventionReport } from '../api/conventionReport';

export interface ConventionReportExcelLabels {
  churchHeader: string;
  reportCountHeader: string;
  totalsLabel: string;
}

/**
 * Generates and downloads an Excel file for the Convention Report.
 *
 * A single sheet mirrors the on-screen table: one row per church, a report count column,
 * one column per numerical/money item, plus a Totals row summing each column across all
 * churches.
 */
export function downloadConventionReportExcel(
  report: ConventionReport,
  labels: ConventionReportExcelLabels,
): void {
  const workbook = XLSX.utils.book_new();

  const headerRow = [
    labels.churchHeader,
    labels.reportCountHeader,
    ...report.columns.map((c) => c.itemTitle),
  ];
  const dataRows = report.rows.map((row) => [row.churchName, row.reportCount, ...row.values]);

  const totalReportCount = report.rows.reduce((sum, row) => sum + row.reportCount, 0);
  const columnTotals = report.columns.map((_, colIndex) =>
    report.rows.reduce((sum, row) => sum + (row.values[colIndex] ?? 0), 0));
  const totalsRow = [labels.totalsLabel, totalReportCount, ...columnTotals];

  const sheetData = [headerRow, ...dataRows, totalsRow];
  const sheet = XLSX.utils.aoa_to_sheet(sheetData);

  // Bold the header row and totals row
  const range = XLSX.utils.decode_range(sheet['!ref'] ?? 'A1');
  const headerStyle = { font: { bold: true } };
  for (let col = range.s.c; col <= range.e.c; col++) {
    const headerCell = XLSX.utils.encode_cell({ r: 0, c: col });
    if (sheet[headerCell]) sheet[headerCell].s = headerStyle;
    const totalsCell = XLSX.utils.encode_cell({ r: sheetData.length - 1, c: col });
    if (sheet[totalsCell]) sheet[totalsCell].s = headerStyle;
  }

  XLSX.utils.book_append_sheet(workbook, sheet, 'Convention Report');

  const safeName = report.templateName.replace(/[^a-z0-9]/gi, '-').toLowerCase();
  XLSX.writeFile(
    workbook, `convention-report-${safeName}-${report.startDate}-${report.endDate}.xlsx`);
}
