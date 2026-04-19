import { render, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getStatisticsTemplates } from '../api/statistics';
import StatisticsTemplateSelectionPage from './StatisticsTemplateSelectionPage';

vi.mock('../api/statistics', () => ({
  getStatisticsTemplates: vi.fn(),
}));

describe('StatisticsTemplateSelectionPage', () => {
  const mockedGetStatisticsTemplates = vi.mocked(getStatisticsTemplates);

  beforeEach(async () => {
    mockedGetStatisticsTemplates.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetStatisticsTemplates.mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <StatisticsTemplateSelectionPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading templates...')).toBeInTheDocument();
  });

  it('renders a tile for each template after loading, including link-only ones', async () => {
    mockedGetStatisticsTemplates.mockResolvedValueOnce([
      { id: 1, serviceTemplateName: 'Sunday Eucharist', linkOnly: false },
      { id: 2, serviceTemplateName: 'Morning Prayer', linkOnly: true },
    ]);

    render(
      <MemoryRouter>
        <StatisticsTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sunday eucharist/i })).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /morning prayer/i })).toBeInTheDocument();
  });

  it('links each tile to the correct statistics filter route', async () => {
    mockedGetStatisticsTemplates.mockResolvedValueOnce([
      { id: 7, serviceTemplateName: 'Vespers', linkOnly: false },
    ]);

    render(
      <MemoryRouter>
        <StatisticsTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /vespers/i })).toHaveAttribute(
        'href',
        '/statistics/7',
      );
    });
  });

  it('shows empty state when there are no templates', async () => {
    mockedGetStatisticsTemplates.mockResolvedValueOnce([]);

    render(
      <MemoryRouter>
        <StatisticsTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('No service templates found.')).toBeInTheDocument();
    });
  });

  it('shows an error state when loading fails', async () => {
    mockedGetStatisticsTemplates.mockRejectedValueOnce(new Error('network error'));

    render(
      <MemoryRouter>
        <StatisticsTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(
        screen.getByText('Failed to load templates. Please try again.'),
      ).toBeInTheDocument();
    });
  });
});
