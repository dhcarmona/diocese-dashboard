import { render, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getServiceTemplates } from '../api/serviceTemplates';
import AdminReportTemplateSelectionPage from './AdminReportTemplateSelectionPage';

vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplates: vi.fn(),
}));

describe('AdminReportTemplateSelectionPage', () => {
  const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);

  beforeEach(async () => {
    mockedGetServiceTemplates.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetServiceTemplates.mockReturnValue(new Promise(() => {}));

    render(
      <MemoryRouter>
        <AdminReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading templates...')).toBeInTheDocument();
  });

  it('renders a tile for each template after loading', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([
      { id: 1, serviceTemplateName: 'Sunday Eucharist', linkOnly: false },
      { id: 2, serviceTemplateName: 'Morning Prayer', linkOnly: false },
    ]);

    render(
      <MemoryRouter>
        <AdminReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sunday eucharist/i })).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /morning prayer/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /sunday eucharist/i })).toHaveAttribute(
      'href',
      '/reports/view/individual/1',
    );
  });

  it('shows empty state when there are no templates', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([]);

    render(
      <MemoryRouter>
        <AdminReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(
        screen.getByText('There are no service templates available yet.'),
      ).toBeInTheDocument();
    });
  });

  it('shows an error state when loading fails', async () => {
    mockedGetServiceTemplates.mockRejectedValueOnce(new Error('network error'));

    render(
      <MemoryRouter>
        <AdminReportTemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(
        screen.getByText('We could not load the service templates.'),
      ).toBeInTheDocument();
    });
  });
});
