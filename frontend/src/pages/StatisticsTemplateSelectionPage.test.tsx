import { render, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getServiceTemplates } from '../api/serviceTemplates';
import StatisticsTemplateSelectionPage from './StatisticsTemplateSelectionPage';

vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplates: vi.fn(),
}));

describe('StatisticsTemplateSelectionPage', () => {
  const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);

  beforeEach(async () => {
    mockedGetServiceTemplates.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetServiceTemplates.mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <StatisticsTemplateSelectionPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading templates...')).toBeInTheDocument();
  });

  it('renders a tile for each template after loading', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([
      { id: 1, serviceTemplateName: 'Sunday Eucharist' },
      { id: 2, serviceTemplateName: 'Morning Prayer' },
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
    mockedGetServiceTemplates.mockResolvedValueOnce([
      { id: 7, serviceTemplateName: 'Vespers' },
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
    mockedGetServiceTemplates.mockResolvedValueOnce([]);

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
    mockedGetServiceTemplates.mockRejectedValueOnce(new Error('network error'));

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
