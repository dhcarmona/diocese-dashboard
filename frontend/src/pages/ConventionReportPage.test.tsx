import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { getConventionReport, type ConventionReport } from '../api/conventionReport';
import ConventionReportPage from './ConventionReportPage';

vi.mock('../api/conventionReport', () => ({
  getConventionReport: vi.fn(),
}));

vi.mock('../utils/conventionReportExcel', () => ({
  downloadConventionReportExcel: vi.fn(),
}));

const mockedGetConventionReport = vi.mocked(getConventionReport);

const sampleReport: ConventionReport = {
  templateId: 1,
  templateName: 'Sunday Eucharist',
  startDate: '2024-01-01',
  endDate: '2024-01-31',
  columns: [
    { itemId: 1, itemTitle: 'Attendance', itemType: 'NUMERICAL' },
    { itemId: 2, itemTitle: 'Offering', itemType: 'DOLLARS' },
  ],
  rows: [
    { churchName: 'Trinity', reportCount: 2, values: [180, 70] },
    { churchName: 'Grace', reportCount: 0, values: [0, 0] },
  ],
  duplicateNotes: [],
};

function renderPage(path = '/convention-report/1/report?startDate=2024-01-01&endDate=2024-01-31') {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/convention-report/:templateId/report" element={<ConventionReportPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ConventionReportPage', () => {
  beforeEach(async () => {
    mockedGetConventionReport.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetConventionReport.mockReturnValue(new Promise(() => {}));

    renderPage();

    expect(screen.getByText('Generating report...')).toBeInTheDocument();
  });

  it('shows an error state when loading fails', async () => {
    mockedGetConventionReport.mockRejectedValueOnce(new Error('network error'));

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('Failed to generate the report. Please try again.'),
      ).toBeInTheDocument();
    });
  });

  it('renders one row per church and one column per numerical item', async () => {
    mockedGetConventionReport.mockResolvedValueOnce(sampleReport);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Trinity')).toBeInTheDocument();
    });

    expect(screen.getByText('Grace')).toBeInTheDocument();
    expect(screen.getByText('Attendance')).toBeInTheDocument();
    expect(screen.getByText('Offering')).toBeInTheDocument();
    expect(screen.getByText('180')).toBeInTheDocument();
    expect(screen.getByText('$70.00')).toBeInTheDocument();
  });

  it('renders the report count column with the number of reports counted per church', async () => {
    mockedGetConventionReport.mockResolvedValueOnce(sampleReport);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Reports Counted')).toBeInTheDocument();
    });

    const trinityRow = screen.getByText('Trinity').closest('tr');
    expect(trinityRow).not.toBeNull();
    expect(trinityRow).toHaveTextContent('2');
  });

  it('shows a duplicate report note when present', async () => {
    mockedGetConventionReport.mockResolvedValueOnce({
      ...sampleReport,
      duplicateNotes: [{ churchName: 'Trinity', serviceDate: '2024-01-07', reportCount: 2 }],
    });

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('Multiple reports found for the same Sunday'),
      ).toBeInTheDocument();
    });
    expect(
      screen.getByText(/Trinity had 2 reports for/),
    ).toBeInTheDocument();
  });

  it('shows noItems message when the template has no numerical columns', async () => {
    mockedGetConventionReport.mockResolvedValueOnce({ ...sampleReport, columns: [], rows: [] });

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('This template has no numerical or money items to report.'),
      ).toBeInTheDocument();
    });
  });

  it('calls the API with templateId, startDate, and endDate from the URL', async () => {
    mockedGetConventionReport.mockResolvedValueOnce(sampleReport);

    renderPage();

    await waitFor(() => {
      expect(mockedGetConventionReport).toHaveBeenCalledWith({
        templateId: 1,
        startDate: '2024-01-01',
        endDate: '2024-01-31',
      });
    });
  });

  it('shows the Download as Excel button when report has data', async () => {
    mockedGetConventionReport.mockResolvedValueOnce(sampleReport);

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Download as Excel' })).toBeInTheDocument();
    });
  });

  it('does not show the Download as Excel button when there are no columns', async () => {
    mockedGetConventionReport.mockResolvedValueOnce({ ...sampleReport, columns: [], rows: [] });

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('This template has no numerical or money items to report.'),
      ).toBeInTheDocument();
    });
    expect(screen.queryByRole('button', { name: 'Download as Excel' })).not.toBeInTheDocument();
  });

  it('calls downloadConventionReportExcel when the Excel button is clicked', async () => {
    const { downloadConventionReportExcel } = await import('../utils/conventionReportExcel');
    const mockedDownload = vi.mocked(downloadConventionReportExcel);
    mockedGetConventionReport.mockResolvedValueOnce(sampleReport);

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Download as Excel' })).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Download as Excel' }));

    expect(mockedDownload).toHaveBeenCalledWith(
      sampleReport,
      expect.objectContaining({
        churchHeader: 'Church',
        reportCountHeader: 'Reports Counted',
        totalsLabel: 'Total',
      }),
    );
  });
});
