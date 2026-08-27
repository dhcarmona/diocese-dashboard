import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { getDrillDown, type ChartDrillDown } from '../api/statistics';
import StatisticsChartDrillDownPage from './StatisticsChartDrillDownPage';

vi.mock('../api/statistics', () => ({
  getDrillDown: vi.fn(),
}));

const mockedGetDrillDown = vi.mocked(getDrillDown);

const sampleDrillDown: ChartDrillDown = {
  itemId: 10,
  itemTitle: 'Attendance',
  itemType: 'NUMERICAL',
  date: '2024-03-10',
  rows: [
    {
      serviceInstanceId: 100,
      churchName: 'Trinity',
      filledByUsername: 'alice',
      filledByFullName: 'Alice Smith',
      value: 30,
    },
    {
      serviceInstanceId: 101,
      churchName: 'Grace',
      filledByUsername: 'bob',
      filledByFullName: null,
      value: 70,
    },
  ],
};

function renderPage(
  search = '?itemId=10&date=2024-03-10&churchName=Trinity&startDate=2024-01-01&endDate=2024-12-31',
) {
  return render(
    <MemoryRouter initialEntries={[`/statistics/1/drill-down${search}`]}>
      <Routes>
        <Route path="/statistics/:templateId/drill-down" element={<StatisticsChartDrillDownPage />} />
        <Route path="/statistics/:templateId/report" element={<div>Report page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('StatisticsChartDrillDownPage', () => {
  beforeEach(async () => {
    mockedGetDrillDown.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows loading indicator initially', () => {
    mockedGetDrillDown.mockReturnValue(new Promise(() => {}));
    renderPage();
    expect(screen.getByText('Loading detail...')).toBeInTheDocument();
  });

  it('shows error alert when fetch fails', async () => {
    mockedGetDrillDown.mockRejectedValueOnce(new Error('network error'));
    renderPage();
    await waitFor(() => {
      expect(
        screen.getByText('Failed to load detail. Please try again.'),
      ).toBeInTheDocument();
    });
  });

  it('renders item title and date after loading', async () => {
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage();
    await waitFor(() => {
      expect(screen.getAllByText('Attendance').length).toBeGreaterThan(0);
    });
    expect(screen.getByText(/10 Mar 2024/)).toBeInTheDocument();
  });

  it('renders a table row for each result', async () => {
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage();
    await waitFor(() => {
      expect(screen.getByText('Alice Smith (alice)')).toBeInTheDocument();
    });
    expect(screen.getByText('bob')).toBeInTheDocument();
    expect(screen.getByText('Trinity')).toBeInTheDocument();
    expect(screen.getByText('Grace')).toBeInTheDocument();
  });

  it('links each row to the report instance detail page', async () => {
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage();
    await waitFor(() => {
      expect(screen.getByRole('link', { name: '#100' })).toBeInTheDocument();
    });
    expect(screen.getByRole('link', { name: '#100' })).toHaveAttribute(
      'href',
      '/reports/view/individual/1/100',
    );
    expect(screen.getByRole('link', { name: '#101' })).toHaveAttribute(
      'href',
      '/reports/view/individual/1/101',
    );
  });

  it('shows no-rows message when rows list is empty', async () => {
    mockedGetDrillDown.mockResolvedValueOnce({ ...sampleDrillDown, rows: [] });
    renderPage();
    await waitFor(() => {
      expect(
        screen.getByText('No contributions found for this item on this date.'),
      ).toBeInTheDocument();
    });
  });

  it('passes correct params to getDrillDown', async () => {
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage('?itemId=10&date=2024-03-10&churchName=Trinity');
    await waitFor(() => expect(mockedGetDrillDown).toHaveBeenCalledTimes(1));
    expect(mockedGetDrillDown).toHaveBeenCalledWith({
      templateId: 1,
      itemId: 10,
      date: '2024-03-10',
      churchName: 'Trinity',
    });
  });

  it('passes no churchName when not in search params (global)', async () => {
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage('?itemId=10&date=2024-03-10');
    await waitFor(() => expect(mockedGetDrillDown).toHaveBeenCalledTimes(1));
    expect(mockedGetDrillDown).toHaveBeenCalledWith({
      templateId: 1,
      itemId: 10,
      date: '2024-03-10',
      churchName: undefined,
    });
  });

  it('sorts rows by clicking column headers', async () => {
    const user = userEvent.setup();
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Alice Smith (alice)')).toBeInTheDocument();
    });

    // Default order is value desc: bob (70) first, alice (30) second
    const rows = screen.getAllByRole('row');
    // rows[0] is header, rows[1..] are data rows
    expect(within(rows[1]).getByText('bob')).toBeInTheDocument();
    expect(within(rows[2]).getByText('Alice Smith (alice)')).toBeInTheDocument();

    // Click Church column to sort ascending by church name
    await user.click(screen.getByText('Church'));
    const sortedRows = screen.getAllByRole('row');
    expect(within(sortedRows[1]).getByText('Grace')).toBeInTheDocument();
    expect(within(sortedRows[2]).getByText('Trinity')).toBeInTheDocument();
  });

  it('back button navigates to the report with original search params', async () => {
    mockedGetDrillDown.mockResolvedValueOnce(sampleDrillDown);
    renderPage(
      '?itemId=10&date=2024-03-10&churchName=Trinity&startDate=2024-01-01&endDate=2024-12-31',
    );
    await waitFor(() => {
      expect(screen.getAllByText('Attendance').length).toBeGreaterThan(0);
    });

    const backButton = screen.getByRole('button', { name: 'Back to Report' });
    expect(backButton).toBeInTheDocument();
  });
});
