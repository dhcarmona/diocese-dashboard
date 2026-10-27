import { describe, expect, it, vi } from 'vitest';
import * as XLSX from 'xlsx';
import type { ConventionReport } from '../api/conventionReport';
import { downloadConventionReportExcel } from './conventionReportExcel';

vi.mock('xlsx', async (importOriginal) => {
  const actual = await importOriginal<typeof XLSX>();
  return {
    ...actual,
    writeFile: vi.fn(),
  };
});

const baseReport: ConventionReport = {
  templateId: 1,
  templateName: 'Sunday Eucharist',
  startDate: '2024-01-01',
  endDate: '2024-01-31',
  columns: [
    { itemId: 1, itemTitle: 'Attendance', itemType: 'NUMERICAL' },
    { itemId: 2, itemTitle: 'Offering', itemType: 'DOLLARS' },
  ],
  rows: [
    { churchName: 'Trinity', reportCount: 3, values: [180, 70] },
    { churchName: 'Grace', reportCount: 0, values: [0, 0] },
  ],
  duplicateNotes: [],
};

const labels = {
  churchHeader: 'Church',
  reportCountHeader: 'Reports Counted',
  totalsLabel: 'Total',
};

describe('downloadConventionReportExcel', () => {
  it('creates a single sheet named "Convention Report"', () => {
    const workbook = XLSX.utils.book_new();
    const bookNewSpy = vi.spyOn(XLSX.utils, 'book_new').mockReturnValue(workbook);
    const appendSheetSpy = vi.spyOn(XLSX.utils, 'book_append_sheet');

    downloadConventionReportExcel(baseReport, labels);

    expect(appendSheetSpy).toHaveBeenCalledTimes(1);
    expect(appendSheetSpy).toHaveBeenCalledWith(workbook, expect.anything(), 'Convention Report');

    bookNewSpy.mockRestore();
    appendSheetSpy.mockRestore();
  });

  it('sheet data has header row, one row per church, and a totals row', () => {
    const capturedSheets: XLSX.WorkSheet[] = [];
    const appendSheetSpy = vi
      .spyOn(XLSX.utils, 'book_append_sheet')
      .mockImplementation((_wb, sheet) => { capturedSheets.push(sheet); });

    downloadConventionReportExcel(baseReport, labels);

    const rows = XLSX.utils.sheet_to_json<(string | number)[]>(capturedSheets[0], { header: 1 });

    expect(rows[0]).toEqual(['Church', 'Reports Counted', 'Attendance', 'Offering']);
    expect(rows[1]).toEqual(['Trinity', 3, 180, 70]);
    expect(rows[2]).toEqual(['Grace', 0, 0, 0]);
    expect(rows[3]).toEqual(['Total', 3, 180, 70]);

    appendSheetSpy.mockRestore();
  });

  it('produces header and totals only when there are no churches', () => {
    const emptyReport: ConventionReport = { ...baseReport, rows: [] };
    const capturedSheets: XLSX.WorkSheet[] = [];
    const appendSheetSpy = vi
      .spyOn(XLSX.utils, 'book_append_sheet')
      .mockImplementation((_wb, sheet) => { capturedSheets.push(sheet); });

    downloadConventionReportExcel(emptyReport, labels);

    const rows = XLSX.utils.sheet_to_json<(string | number)[]>(capturedSheets[0], { header: 1 });
    expect(rows).toHaveLength(2);
    expect(rows[1]).toEqual(['Total', 0, 0, 0]);

    appendSheetSpy.mockRestore();
  });

  it('saves with a filename derived from template name and dates', () => {
    const writeFileSpy = vi.mocked(XLSX.writeFile);

    downloadConventionReportExcel(baseReport, labels);

    expect(writeFileSpy).toHaveBeenCalledWith(
      expect.anything(),
      'convention-report-sunday-eucharist-2024-01-01-2024-01-31.xlsx',
    );
  });
});
