import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import {
  type ServiceInstanceSummary,
  getInstancesByTemplate,
} from '../api/serviceInstances';
import ReportInstancesListPage from './ReportInstancesListPage';

vi.mock('../api/serviceInstances', () => ({
  getInstancesByTemplate: vi.fn(),
}));

function renderWithTemplateId(templateId = '10') {
  render(
    <MemoryRouter initialEntries={[`/reports/view/individual/${templateId}`]}>
      <Routes>
        <Route
          path="/reports/view/individual/:templateId"
          element={<ReportInstancesListPage />}
        />
      </Routes>
    </MemoryRouter>,
  );
}

const BASE_INSTANCE: ServiceInstanceSummary = {
  id: 1,
  serviceDate: '2026-01-15',
  churchName: 'Trinity Church',
  templateId: 10,
  templateName: 'Sunday Eucharist',
  submittedByUsername: 'jsmith',
  submittedByFullName: 'Jonathan Smith',
  submittedAt: null,
};

describe('ReportInstancesListPage', () => {
  const mockedGetInstancesByTemplate = vi.mocked(getInstancesByTemplate);

  beforeEach(async () => {
    mockedGetInstancesByTemplate.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetInstancesByTemplate.mockReturnValue(new Promise(() => {}));

    renderWithTemplateId();

    expect(screen.getByText('Loading reports...')).toBeInTheDocument();
  });

  it('renders a table row for each instance after loading', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      BASE_INSTANCE,
      { ...BASE_INSTANCE, id: 2, churchName: 'St. Paul', submittedByFullName: 'Ana Perez' },
    ]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByText('Trinity Church')).toBeInTheDocument();
    });

    expect(screen.getByText('St. Paul')).toBeInTheDocument();
    expect(screen.getByText('Jonathan Smith')).toBeInTheDocument();
    expect(screen.getByText('Ana Perez')).toBeInTheDocument();
    expect(screen.getAllByText('15 Jan 2026')).toHaveLength(2);
    expect(screen.getAllByRole('link', { name: /view details/i })).toHaveLength(2);
  });

  it('the view details link points to the correct detail page', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([BASE_INSTANCE]);

    renderWithTemplateId();

    const link = await screen.findByRole('link', { name: /view details/i });
    expect(link).toHaveAttribute('href', '/reports/view/individual/10/1');
  });

  it('shows reporter username when full name is absent', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      { ...BASE_INSTANCE, submittedByFullName: null },
    ]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByText('jsmith')).toBeInTheDocument();
    });
  });

  it('shows "Unknown" when reporter is absent', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      { ...BASE_INSTANCE, submittedByUsername: null, submittedByFullName: null },
    ]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByText('Unknown')).toBeInTheDocument();
    });
  });

  it('shows the Submitted At column when any instance has a value', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      { ...BASE_INSTANCE, submittedAt: '2026-01-15T10:30:00' },
    ]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByText('Submitted At')).toBeInTheDocument();
    });
    expect(screen.getByText('15 Jan 2026, 10:30')).toBeInTheDocument();
  });

  it('hides the Submitted At column when all values are null', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([BASE_INSTANCE]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByText('Trinity Church')).toBeInTheDocument();
    });

    expect(screen.queryByText('Submitted At')).not.toBeInTheDocument();
  });

  it('shows empty state when there are no instances', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(
        screen.getByText('No reports have been submitted for this template yet.'),
      ).toBeInTheDocument();
    });
  });

  it('shows an error state when loading fails', async () => {
    mockedGetInstancesByTemplate.mockRejectedValueOnce(new Error('network error'));

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByText('We could not load the reports.')).toBeInTheDocument();
    });
  });

  it('renders a search input when instances are loaded', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([BASE_INSTANCE]);

    renderWithTemplateId();

    await waitFor(() => {
      expect(screen.getByLabelText('Search reports…')).toBeInTheDocument();
    });
  });

  it('filters rows by church name', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      BASE_INSTANCE,
      { ...BASE_INSTANCE, id: 2, churchName: 'St. Paul', submittedByFullName: 'Ana Perez' },
    ]);

    renderWithTemplateId();

    const input = await screen.findByLabelText('Search reports…');
    await userEvent.type(input, 'Trinity');

    expect(screen.getByText('Trinity Church')).toBeInTheDocument();
    expect(screen.queryByText('St. Paul')).not.toBeInTheDocument();
  });

  it('filters rows by reporter name', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      BASE_INSTANCE,
      { ...BASE_INSTANCE, id: 2, churchName: 'St. Paul', submittedByFullName: 'Ana Perez' },
    ]);

    renderWithTemplateId();

    const input = await screen.findByLabelText('Search reports…');
    await userEvent.type(input, 'Ana');

    expect(screen.getByText('St. Paul')).toBeInTheDocument();
    expect(screen.queryByText('Trinity Church')).not.toBeInTheDocument();
  });

  it('filters rows by service date', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      BASE_INSTANCE,
      { ...BASE_INSTANCE, id: 2, serviceDate: '2026-03-20', churchName: 'St. Paul' },
    ]);

    renderWithTemplateId();

    const input = await screen.findByLabelText('Search reports…');
    await userEvent.type(input, '2026-01');

    expect(screen.getByText('Trinity Church')).toBeInTheDocument();
    expect(screen.queryByText('St. Paul')).not.toBeInTheDocument();
  });

  it('shows no-search-results message when filter matches nothing', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([BASE_INSTANCE]);

    renderWithTemplateId();

    const input = await screen.findByLabelText('Search reports…');
    await userEvent.type(input, 'zzznomatch');

    expect(screen.getByText('No reports match your search.')).toBeInTheDocument();
    expect(screen.queryByRole('table')).not.toBeInTheDocument();
  });

  it('clears search when the clear button is clicked', async () => {
    mockedGetInstancesByTemplate.mockResolvedValueOnce([
      BASE_INSTANCE,
      { ...BASE_INSTANCE, id: 2, churchName: 'St. Paul', submittedByFullName: 'Ana Perez' },
    ]);

    renderWithTemplateId();

    const input = await screen.findByLabelText('Search reports…');
    await userEvent.type(input, 'Trinity');

    expect(screen.queryByText('St. Paul')).not.toBeInTheDocument();

    const clearBtn = screen.getByRole('button', { name: /clear search/i });
    await userEvent.click(clearBtn);

    expect(screen.getByText('Trinity Church')).toBeInTheDocument();
    expect(screen.getByText('St. Paul')).toBeInTheDocument();
  });
});
