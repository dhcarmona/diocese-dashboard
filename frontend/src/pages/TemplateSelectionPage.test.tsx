import { render, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getServiceTemplates, type ServiceTemplate } from '../api/serviceTemplates';
import TemplateSelectionPage from './TemplateSelectionPage';

vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplates: vi.fn(),
}));

describe('TemplateSelectionPage', () => {
  const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);

  beforeEach(async () => {
    mockedGetServiceTemplates.mockReset();
    await i18n.changeLanguage('en');
  });

  it('renders service template tiles after loading', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([
      { id: 1, serviceTemplateName: 'Sunday Eucharist', linkOnly: false } as ServiceTemplate,
      { id: 2, serviceTemplateName: 'Morning Prayer', linkOnly: false } as ServiceTemplate,
    ]);

    render(
      <MemoryRouter>
        <TemplateSelectionPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading templates...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /sunday eucharist/i })).toBeInTheDocument();
    });

    expect(screen.getByRole('link', { name: /morning prayer/i })).toBeInTheDocument();
  });

  it('renders the empty state when there are no templates', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([]);

    render(
      <MemoryRouter>
        <TemplateSelectionPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(
        screen.getByText('There are no service templates available yet.'),
      ).toBeInTheDocument();
    });
  });
});
