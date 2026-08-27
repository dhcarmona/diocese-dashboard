import * as XLSX from 'xlsx';
import type { StatisticsReport } from '../api/statistics';

export interface ExcelLabels {
  churchHeader: string;
  dateHeader: string;
  amountHeader: string;
  totalsLabel: string;
  globalChurchName: string;
}

/**
 * Generates and downloads an Excel file for the statistics report.
 *
 * Each numerical/money item gets its own sheet. Each sheet has columns:
 * Church Name, Date, and the item value. A Totals row is appended at the bottom.
 */
export function downloadStatisticsExcel(
  report: StatisticsReport,
  labels: ExcelLabels,
): void {
  const workbook = XLSX.utils.book_new();

  const allItems = [...report.numericalItems, ...report.moneyItems];

  for (const item of allItems) {
    const churchName = report.global ? labels.globalChurchName : (report.churchName ?? '');

    const headerRow = [labels.churchHeader, labels.dateHeader, labels.amountHeader];

    const dataRows = report.global && item.perChurchData.length > 0
      ? item.perChurchData.map((pt) => [pt.churchName, pt.date, pt.value])
      : item.timeSeriesData.map((pt) => [churchName, pt.date, pt.value]);

    const total = dataRows.reduce((sum, row) => sum + (row[2] as number), 0);
    const totalRow = [labels.totalsLabel, '', total];

    const sheetData = [headerRow, ...dataRows, totalRow];

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

    // Truncate sheet name to 31 chars (Excel limit)
    const sheetName = item.itemTitle.slice(0, 31);
    XLSX.utils.book_append_sheet(workbook, sheet, sheetName);
  }

  const safeName = report.templateName.replace(/[^a-z0-9]/gi, '-').toLowerCase();
  XLSX.writeFile(workbook, `statistics-${safeName}-${report.startDate}-${report.endDate}.xlsx`);
}
