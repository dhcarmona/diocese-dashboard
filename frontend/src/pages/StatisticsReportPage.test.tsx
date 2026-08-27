import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { getStatistics, type StatisticsReport } from '../api/statistics';
import StatisticsReportPage from './StatisticsReportPage';

const { mockUseAuth } = vi.hoisted(() => ({
  mockUseAuth: vi.fn(() => ({ user: { role: 'REPORTER' as 'ADMIN' | 'REPORTER' } })),
}));

vi.mock('../api/statistics', () => ({
  getStatistics: vi.fn(),
}));

vi.mock('../auth/auth-context', () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock('../utils/statisticsPdf', () => ({
  downloadStatisticsPdf: vi.fn().mockResolvedValue(undefined),
}));

vi.mock('../utils/statisticsExcel', () => ({
  downloadStatisticsExcel: vi.fn(),
}));

vi.mock('recharts', () => ({
  PieChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Pie: () => <div data-testid="pie" />,
  Cell: () => null,
  Legend: () => null,
  BarChart: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
  Bar: ({ onClick }: { onClick?: (data: object) => void }) => (
    <button
      data-testid="bar"
      onClick={() => onClick?.({ payload: { rawDate: '2024-03-10' } })}
    />
  ),
  XAxis: () => null,
  YAxis: () => null,
  CartesianGrid: () => null,
  Tooltip: () => null,
  ResponsiveContainer: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

const mockedGetStatistics = vi.mocked(getStatistics);

const sampleReport: StatisticsReport = {
  templateId: 1,
  templateName: 'Sunday Eucharist',
  churchName: 'Trinity',
  global: false,
  startDate: '2024-01-01',
  endDate: '2024-12-31',
  totalServiceCount: 12,
  celebrantStats: [
    { celebrantId: 1, celebrantName: 'Rev. Smith', serviceCount: 8 },
    { celebrantId: 2, celebrantName: 'Rev. Jones', serviceCount: 4 },
  ],
  numericalItems: [
    {
      itemId: 1,
      itemTitle: 'Attendance',
      itemType: 'NUMERICAL',
      total: 350,
      timeSeriesData: [],
      perChurchData: [],
    },
  ],
  moneyItems: [
    {
      itemId: 2,
      itemTitle: 'Offering',
      itemType: 'DOLLARS',
      total: 5000,
      timeSeriesData: [],
      perChurchData: [],
    },
  ],
  pendingLinks: [
    {
      token: 'abc123',
      reporterUsername: 'mlopez',
      reporterFullName: 'Maria Lopez',
      activeDate: '2025-06-01',
      churchName: 'Trinity',
    },
  ],
};

function renderPage(search = '?churchName=Trinity&startDate=2024-01-01&endDate=2024-12-31') {
  return render(
    <MemoryRouter initialEntries={[`/statistics/1/report${search}`]}>
      <Routes>
        <Route path="/statistics/:templateId/report" element={<StatisticsReportPage />} />
        <Route path="/statistics/:templateId" element={<div>Filter page</div>} />
        <Route path="/statistics/:templateId/drill-down" element={<div>Drill-down page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

const sampleReportWithTimeSeries: StatisticsReport = {
  ...sampleReport,
  numericalItems: [
    {
      itemId: 1,
      itemTitle: 'Attendance',
      itemType: 'NUMERICAL',
      total: 80,
      timeSeriesData: [{ date: '2024-03-10', value: 80 }],
      perChurchData: [],
    },
  ],
};

describe('StatisticsReportPage', () => {
  beforeEach(async () => {
    mockedGetStatistics.mockReset();
    mockUseAuth.mockReturnValue({ user: { role: 'REPORTER' as const } });
    await i18n.changeLanguage('en');
  });

  it('shows loading indicator initially', () => {
    mockedGetStatistics.mockReturnValue(new Promise(() => {}));
    renderPage();

    expect(screen.getByText('Generating report...')).toBeInTheDocument();
  });

  it('shows error alert when the report fails to load', async () => {
    mockedGetStatistics.mockRejectedValueOnce(new Error('fail'));
    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('Failed to generate the report. Please try again.'),
      ).toBeInTheDocument();
    });
  });

  it('renders report with template name and church', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });
    expect(screen.getByText('Trinity')).toBeInTheDocument();
    expect(screen.getByText('01 Jan 2024 – 31 Dec 2024')).toBeInTheDocument();
  });

  it('renders celebrant names in the celebrant section', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Rev. Smith')).toBeInTheDocument();
    });
    expect(screen.getByText('Rev. Jones')).toBeInTheDocument();
  });

  it('renders numerical item titles and totals', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Attendance')).toBeInTheDocument();
    });
    expect(screen.getByText(/350/)).toBeInTheDocument();
  });

  it('renders money item titles and totals', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Offering')).toBeInTheDocument();
    });
    expect(screen.getByText(/5,000/)).toBeInTheDocument();
  });

  it('renders pending links section with reporter name', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Reports to Fill')).toBeInTheDocument();
    });
    expect(screen.getByText('Maria Lopez (mlopez)')).toBeInTheDocument();
    expect(screen.getByText('01 Jun 2025')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /\/r\/abc123/ })).toHaveAttribute(
      'href',
      '/r/abc123',
    );
  });

  it('shows total service count', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('12')).toBeInTheDocument();
    });
  });

  it('renders "All Churches (Global)" when report is global', async () => {
    mockedGetStatistics.mockResolvedValueOnce({ ...sampleReport, global: true, churchName: null });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('All Churches (Global)')).toBeInTheDocument();
    });
  });

  it('shows no celebrant message when celebrant stats are empty', async () => {
    mockedGetStatistics.mockResolvedValueOnce({ ...sampleReport, celebrantStats: [] });
    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('No celebrant data available for this period.'),
      ).toBeInTheDocument();
    });
  });

  it('shows no pending links message when list is empty', async () => {
    mockedGetStatistics.mockResolvedValueOnce({ ...sampleReport, pendingLinks: [] });
    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('No pending reporter links for this church and template.'),
      ).toBeInTheDocument();
    });
  });

  it('shows church column in pending links table for global report', async () => {
    mockedGetStatistics.mockResolvedValueOnce({ ...sampleReport, global: true, churchName: null });
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Reports to Fill')).toBeInTheDocument();
    });
    expect(screen.getByRole('columnheader', { name: 'Church' })).toBeInTheDocument();
  });

  it('does not show church column in pending links table for church-scoped report', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Reports to Fill')).toBeInTheDocument();
    });
    expect(screen.queryByRole('columnheader', { name: 'Church' })).not.toBeInTheDocument();
  });

  it('shows the Download as PDF button when report is loaded', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Download as PDF' })).toBeInTheDocument();
    });
  });

  it('calls downloadStatisticsPdf when the PDF button is clicked', async () => {
    const { downloadStatisticsPdf } = await import('../utils/statisticsPdf');
    const mockedDownload = vi.mocked(downloadStatisticsPdf);
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Download as PDF' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Download as PDF' }));

    await waitFor(() => {
      expect(mockedDownload).toHaveBeenCalledOnce();
    });
  });

  it('admin clicking a bar navigates to the drill-down page', async () => {
    const user = userEvent.setup();
    mockUseAuth.mockReturnValue({ user: { role: 'ADMIN' as const } });
    mockedGetStatistics.mockResolvedValueOnce(sampleReportWithTimeSeries);
    renderPage('?churchName=Trinity&startDate=2024-01-01&endDate=2024-12-31');

    await waitFor(() => {
      expect(screen.getByText('Attendance')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('bar'));

    await waitFor(() => {
      expect(screen.getByText('Drill-down page')).toBeInTheDocument();
    });
  });

  it('reporter clicking a bar does not navigate', async () => {
    const user = userEvent.setup();
    // mockUseAuth already returns REPORTER from beforeEach
    mockedGetStatistics.mockResolvedValueOnce(sampleReportWithTimeSeries);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Attendance')).toBeInTheDocument();
    });

    await user.click(screen.getByTestId('bar'));

    // Should remain on the report page, not navigate to drill-down
    expect(screen.queryByText('Drill-down page')).not.toBeInTheDocument();
    expect(screen.getByText('Attendance')).toBeInTheDocument();
  });

  it('shows the Download as Excel button when report is loaded', async () => {
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Download as Excel' })).toBeInTheDocument();
    });
  });

  it('calls downloadStatisticsExcel when the Excel button is clicked', async () => {
    const { downloadStatisticsExcel } = await import('../utils/statisticsExcel');
    const mockedDownload = vi.mocked(downloadStatisticsExcel);
    mockedGetStatistics.mockResolvedValueOnce(sampleReport);
    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Download as Excel' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Download as Excel' }));

    expect(mockedDownload).toHaveBeenCalledOnce();
    expect(mockedDownload).toHaveBeenCalledWith(sampleReport, expect.objectContaining({
      churchHeader: 'Church',
      dateHeader: 'Date',
      amountHeader: 'Amount',
      totalsLabel: 'Total',
      globalChurchName: 'All Churches',
    }));
  });
});
