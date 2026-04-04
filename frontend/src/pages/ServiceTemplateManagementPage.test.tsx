import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import {
  createServiceTemplate,
  deleteServiceTemplate,
  getServiceTemplateById,
  getServiceTemplates,
  updateServiceTemplate,
} from '../api/serviceTemplates';
import ServiceTemplateManagementPage from './ServiceTemplateManagementPage';

vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplates: vi.fn(),
  getServiceTemplateById: vi.fn(),
  createServiceTemplate: vi.fn(),
  updateServiceTemplate: vi.fn(),
  deleteServiceTemplate: vi.fn(),
}));

vi.mock('../api/serviceInfoItems', () => ({
  createServiceInfoItem: vi.fn(),
  deleteServiceInfoItem: vi.fn(),
}));

describe('ServiceTemplateManagementPage', () => {
  const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);
  const mockedGetServiceTemplateById = vi.mocked(getServiceTemplateById);
  const mockedCreateServiceTemplate = vi.mocked(createServiceTemplate);
  const mockedUpdateServiceTemplate = vi.mocked(updateServiceTemplate);
  const mockedDeleteServiceTemplate = vi.mocked(deleteServiceTemplate);

  const sampleTemplate = {
    id: 1,
    serviceTemplateName: 'Sunday Mass',
    serviceInfoItems: [],
    bannerUrl: undefined,
  };

  beforeEach(async () => {
    mockedGetServiceTemplates.mockReset();
    mockedGetServiceTemplateById.mockReset();
    mockedCreateServiceTemplate.mockReset();
    mockedUpdateServiceTemplate.mockReset();
    mockedDeleteServiceTemplate.mockReset();
    await i18n.changeLanguage('en');
  });

  function renderPage() {
    return render(
      <MemoryRouter>
        <ServiceTemplateManagementPage />
      </MemoryRouter>,
    );
  }

  it('renders loading state then shows template directory', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplate]);

    renderPage();

    expect(screen.getByText('Loading templates...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /sunday mass/i })).toBeInTheDocument();
    });

    expect(screen.getByText('Total service templates: 1')).toBeInTheDocument();
  });

  it('shows empty state when no templates exist', async () => {
    mockedGetServiceTemplates.mockResolvedValueOnce([]);

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('No service templates have been added yet.'),
      ).toBeInTheDocument();
    });
  });

  it('shows error alert when loading fails', async () => {
    mockedGetServiceTemplates.mockRejectedValueOnce(new Error('network error'));

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('We could not load the service templates right now.'),
      ).toBeInTheDocument();
    });
  });

  it('selects a template and shows edit form', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplate]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplate);

    renderPage();

    const templateButton = await screen.findByRole('button', { name: /sunday mass/i });
    await user.click(templateButton);

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: 'Edit Service Template' }),
      ).toBeInTheDocument();
    });

    expect(screen.getByDisplayValue('Sunday Mass')).toBeInTheDocument();
  });

  it('shows validation error when creating without a name', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([]);

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByRole('heading', { name: 'Add Service Template' }),
      ).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /create template/i }));

    expect(
      await screen.findByText('Enter a template name before saving.'),
    ).toBeInTheDocument();
    expect(mockedCreateServiceTemplate).not.toHaveBeenCalled();
  });

  it('filters templates by search term', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([
      sampleTemplate,
      {
        ...sampleTemplate,
        id: 2,
        serviceTemplateName: 'Vespers',
      },
    ]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /sunday mass/i })).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Search templates'), 'Vespers');

    expect(screen.queryByRole('button', { name: /sunday mass/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /vespers/i })).toBeInTheDocument();
  });
});
