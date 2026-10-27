import { render, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getConventionReportTemplates } from '../api/conventionReport';
import ConventionReportTemplateSelectionPage from './ConventionReportTemplateSelectionPage';

vi.mock('../api/conventionReport', () => ({
  getConventionReportTemplates: vi.fn(),
}));

describe('ConventionReportTemplateSelectionPage', () => {
  const mockedGetTemplates = vi.mocked(getConventionReportTemplates);

  beforeEach(async () => {
    mockedGetTemplates.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetTemplates.mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <ConventionReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading templates...')).toBeInTheDocument();
  });

  it('renders a tile for each template after loading', async () => {
    mockedGetTemplates.mockResolvedValueOnce([
      { id: 1, serviceTemplateName: 'Sunday Eucharist', linkOnly: false },
    ]);

    render(
      <MemoryRouter>
        <ConventionReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sunday eucharist/i })).toBeInTheDocument();
    });
  });

  it('links each tile to the correct convention report filter route', async () => {
    mockedGetTemplates.mockResolvedValueOnce([
      { id: 7, serviceTemplateName: 'Vespers', linkOnly: false },
    ]);

    render(
      <MemoryRouter>
        <ConventionReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /vespers/i })).toHaveAttribute(
        'href',
        '/convention-report/7',
      );
    });
  });

  it('shows empty state when there are no templates', async () => {
    mockedGetTemplates.mockResolvedValueOnce([]);

    render(
      <MemoryRouter>
        <ConventionReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('No service templates found.')).toBeInTheDocument();
    });
  });

  it('shows an error state when loading fails', async () => {
    mockedGetTemplates.mockRejectedValueOnce(new Error('network error'));

    render(
      <MemoryRouter>
        <ConventionReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(
        screen.getByText('Failed to load templates. Please try again.'),
      ).toBeInTheDocument();
    });
  });
});
