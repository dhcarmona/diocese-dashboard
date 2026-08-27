import { describe, expect, it, vi } from 'vitest';
import * as XLSX from 'xlsx';
import type { StatisticsReport } from '../api/statistics';
import { downloadStatisticsExcel } from './statisticsExcel';

vi.mock('xlsx', async (importOriginal) => {
  const actual = await importOriginal<typeof XLSX>();
  return {
    ...actual,
    writeFile: vi.fn(),
  };
});

const baseReport: StatisticsReport = {
  templateId: 1,
  templateName: 'Sunday Eucharist',
  churchName: 'Trinity',
  global: false,
  startDate: '2024-01-01',
  endDate: '2024-12-31',
  totalServiceCount: 3,
  celebrantStats: [],
  numericalItems: [
    {
      itemId: 1,
      itemTitle: 'Attendance',
      itemType: 'NUMERICAL',
      total: 150,
      timeSeriesData: [
        { date: '2024-01-07', value: 60 },
        { date: '2024-01-14', value: 90 },
      ],
      perChurchData: [],
    },
  ],
  moneyItems: [
    {
      itemId: 2,
      itemTitle: 'Offering',
      itemType: 'DOLLARS',
      total: 500,
      timeSeriesData: [
        { date: '2024-01-07', value: 200 },
        { date: '2024-01-14', value: 300 },
      ],
      perChurchData: [],
    },
  ],
  pendingLinks: [],
};

const labels = {
  churchHeader: 'Church',
  dateHeader: 'Date',
  amountHeader: 'Amount',
  totalsLabel: 'Total',
  globalChurchName: 'All Churches',
};

describe('downloadStatisticsExcel', () => {
  it('creates one sheet per numerical and money item', () => {
    const workbook = XLSX.utils.book_new();
    const bookNewSpy = vi.spyOn(XLSX.utils, 'book_new').mockReturnValue(workbook);
    const appendSheetSpy = vi.spyOn(XLSX.utils, 'book_append_sheet');

    downloadStatisticsExcel(baseReport, labels);

    expect(appendSheetSpy).toHaveBeenCalledTimes(2);
    expect(appendSheetSpy).toHaveBeenCalledWith(workbook, expect.anything(), 'Attendance');
    expect(appendSheetSpy).toHaveBeenCalledWith(workbook, expect.anything(), 'Offering');

    bookNewSpy.mockRestore();
    appendSheetSpy.mockRestore();
  });

  it('sheet data has header row, data rows, and totals row', () => {
    const capturedSheets: XLSX.WorkSheet[] = [];
    const appendSheetSpy = vi
      .spyOn(XLSX.utils, 'book_append_sheet')
      .mockImplementation((_wb, sheet) => { capturedSheets.push(sheet); });

    downloadStatisticsExcel(baseReport, labels);

    const sheet = capturedSheets[0];
    const rows = XLSX.utils.sheet_to_json<string[]>(sheet, { header: 1 });

    // Header
    expect(rows[0]).toEqual(['Church', 'Date', 'Amount']);
    // Data rows
    expect(rows[1]).toEqual(['Trinity', '2024-01-07', 60]);
    expect(rows[2]).toEqual(['Trinity', '2024-01-14', 90]);
    // Totals row
    expect(rows[3]).toEqual(['Total', '', 150]);

    appendSheetSpy.mockRestore();
  });

  it('uses globalChurchName when report is global', () => {
    const globalReport: StatisticsReport = { ...baseReport, global: true, churchName: null };
    const capturedSheets: XLSX.WorkSheet[] = [];
    const appendSheetSpy = vi
      .spyOn(XLSX.utils, 'book_append_sheet')
      .mockImplementation((_wb, sheet) => { capturedSheets.push(sheet); });

    downloadStatisticsExcel(globalReport, labels);

    const sheet = capturedSheets[0];
    const rows = XLSX.utils.sheet_to_json<string[]>(sheet, { header: 1 });
    expect(rows[1][0]).toBe('All Churches');

    appendSheetSpy.mockRestore();
  });

  it('truncates sheet name to 31 characters', () => {
    const longTitleReport: StatisticsReport = {
      ...baseReport,
      numericalItems: [
        {
          ...baseReport.numericalItems[0],
          itemTitle: 'A'.repeat(40),
        },
      ],
      moneyItems: [],
    };
    const appendSheetSpy = vi.spyOn(XLSX.utils, 'book_append_sheet');

    downloadStatisticsExcel(longTitleReport, labels);

    const sheetName = appendSheetSpy.mock.calls[0][2];
    expect(sheetName).toHaveLength(31);

    appendSheetSpy.mockRestore();
  });

  it('produces an empty data section when timeSeriesData is empty', () => {
    const emptyReport: StatisticsReport = {
      ...baseReport,
      numericalItems: [
        { ...baseReport.numericalItems[0], total: 0, timeSeriesData: [], perChurchData: [] },
      ],
      moneyItems: [],
    };
    const capturedSheets: XLSX.WorkSheet[] = [];
    const appendSheetSpy = vi
      .spyOn(XLSX.utils, 'book_append_sheet')
      .mockImplementation((_wb, sheet) => { capturedSheets.push(sheet); });

    downloadStatisticsExcel(emptyReport, labels);

    const rows = XLSX.utils.sheet_to_json<string[]>(capturedSheets[0], { header: 1 });
    // Only header + totals, no data rows
    expect(rows).toHaveLength(2);
    expect(rows[0]).toEqual(['Church', 'Date', 'Amount']);
    expect(rows[1][0]).toBe('Total');

    appendSheetSpy.mockRestore();
  });

  it('global report uses perChurchData with individual church rows instead of aggregated', () => {
    const globalReport: StatisticsReport = {
      ...baseReport,
      global: true,
      churchName: null,
      numericalItems: [
        {
          ...baseReport.numericalItems[0],
          total: 150,
          timeSeriesData: [
            { date: '2024-01-07', value: 150 }, // aggregated across all churches
          ],
          perChurchData: [
            { churchName: 'Trinity', date: '2024-01-07', value: 60 },
            { churchName: 'St. Paul', date: '2024-01-07', value: 90 },
          ],
        },
      ],
      moneyItems: [],
    };
    const capturedSheets: XLSX.WorkSheet[] = [];
    const appendSheetSpy = vi
      .spyOn(XLSX.utils, 'book_append_sheet')
      .mockImplementation((_wb, sheet) => { capturedSheets.push(sheet); });

    downloadStatisticsExcel(globalReport, labels);

    const rows = XLSX.utils.sheet_to_json<string[]>(capturedSheets[0], { header: 1 });
    // Header + 2 per-church rows + totals (NOT the single aggregated row)
    expect(rows).toHaveLength(4);
    expect(rows[1]).toEqual(['Trinity', '2024-01-07', 60]);
    expect(rows[2]).toEqual(['St. Paul', '2024-01-07', 90]);
    expect(rows[3]).toEqual(['Total', '', 150]);

    appendSheetSpy.mockRestore();
  });

  it('saves with a filename derived from template name and dates', () => {
    const writeFileSpy = vi.mocked(XLSX.writeFile);

    downloadStatisticsExcel(baseReport, labels);

    expect(writeFileSpy).toHaveBeenCalledWith(
      expect.anything(),
      'statistics-sunday-eucharist-2024-01-01-2024-12-31.xlsx',
    );
  });
});
