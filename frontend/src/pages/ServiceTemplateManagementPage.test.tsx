import { act, render, screen, waitFor } from '@testing-library/react';
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
import {
  createServiceInfoItem,
  reorderServiceInfoItems,
  updateServiceInfoItem,
} from '../api/serviceInfoItems';
import ServiceTemplateManagementPage from './ServiceTemplateManagementPage';

// Capture the DragEnd callback so tests can simulate drag events.
const dndState = vi.hoisted(() => ({
  onDragEnd: null as ((event: { active: { id: number }; over: { id: number } | null }) => void) | null,
}));

vi.mock('@dnd-kit/core', () => ({
  DndContext: ({ children, onDragEnd }: { children: unknown; onDragEnd: (e: unknown) => void }) => {
    dndState.onDragEnd = onDragEnd as typeof dndState.onDragEnd;
    return children;
  },
  closestCenter: {},
  KeyboardSensor: class {},
  PointerSensor: class {},
  useSensor: () => null,
  useSensors: () => [],
}));

vi.mock('@dnd-kit/sortable', () => ({
  SortableContext: ({ children }: { children: unknown }) => children,
  useSortable: () => ({
    attributes: {},
    listeners: {},
    setNodeRef: () => {},
    transform: null,
    transition: undefined,
    isDragging: false,
  }),
  arrayMove: <T,>(arr: T[], from: number, to: number): T[] => {
    const result = [...arr];
    const [removed] = result.splice(from, 1);
    result.splice(to, 0, removed);
    return result;
  },
  sortableKeyboardCoordinates: {},
  verticalListSortingStrategy: {},
}));

vi.mock('@dnd-kit/utilities', () => ({
  CSS: { Transform: { toString: () => '' } },
}));

vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplates: vi.fn(),
  getServiceTemplateById: vi.fn(),
  createServiceTemplate: vi.fn(),
  updateServiceTemplate: vi.fn(),
  deleteServiceTemplate: vi.fn(),
}));

vi.mock('../api/serviceInfoItems', () => ({
  createServiceInfoItem: vi.fn(),
  updateServiceInfoItem: vi.fn(),
  deleteServiceInfoItem: vi.fn(),
  reorderServiceInfoItems: vi.fn(),
}));

describe('ServiceTemplateManagementPage', () => {
  const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);
  const mockedGetServiceTemplateById = vi.mocked(getServiceTemplateById);
  const mockedCreateServiceTemplate = vi.mocked(createServiceTemplate);
  const mockedUpdateServiceTemplate = vi.mocked(updateServiceTemplate);
  const mockedDeleteServiceTemplate = vi.mocked(deleteServiceTemplate);
  const mockedReorderServiceInfoItems = vi.mocked(reorderServiceInfoItems);
  const mockedUpdateServiceInfoItem = vi.mocked(updateServiceInfoItem);
  const mockedCreateServiceInfoItem = vi.mocked(createServiceInfoItem);

  const sampleTemplate = {
    id: 1,
    serviceTemplateName: 'Sunday Mass',
    serviceInfoItems: [],
    bannerUrl: undefined,
  };

  const sampleTemplateWithItems = {
    ...sampleTemplate,
    serviceInfoItems: [
      { id: 10, title: 'Attendance', required: true, serviceInfoItemType: 'NUMERICAL' as const },
      { id: 20, title: 'Offering', required: false, serviceInfoItemType: 'DOLLARS' as const },
    ],
  };

  beforeEach(async () => {
    mockedGetServiceTemplates.mockReset();
    mockedGetServiceTemplateById.mockReset();
    mockedCreateServiceTemplate.mockReset();
    mockedUpdateServiceTemplate.mockReset();
    mockedDeleteServiceTemplate.mockReset();
    mockedReorderServiceInfoItems.mockReset();
    mockedUpdateServiceInfoItem.mockReset();
    mockedCreateServiceInfoItem.mockReset();
    dndState.onDragEnd = null;
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

  it('calls reorderServiceInfoItems with the new order after a drag', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplateWithItems]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplateWithItems);
    mockedReorderServiceInfoItems.mockResolvedValueOnce(undefined);

    renderPage();
    await user.click(await screen.findByRole('button', { name: /sunday mass/i }));
    await screen.findByText('Attendance');

    await act(async () => {
      dndState.onDragEnd?.({ active: { id: 10 }, over: { id: 20 } });
    });

    await waitFor(() => {
      expect(mockedReorderServiceInfoItems).toHaveBeenCalledWith([20, 10]);
    });
  });

  it('reverts item order and shows error when reorder API call fails', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplateWithItems]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplateWithItems);
    mockedReorderServiceInfoItems.mockRejectedValueOnce(new Error('network error'));

    renderPage();
    await user.click(await screen.findByRole('button', { name: /sunday mass/i }));
    await screen.findByText('Attendance');

    await act(async () => {
      dndState.onDragEnd?.({ active: { id: 10 }, over: { id: 20 } });
    });

    await screen.findByText('We could not save the new item order right now.');
    const attendanceEl = screen.getByText('Attendance');
    const offeringEl = screen.getByText('Offering');
    expect(attendanceEl).toBeInTheDocument();
    expect(offeringEl).toBeInTheDocument();
    // Attendance should appear before Offering in the DOM (original order preserved)
    expect(
      attendanceEl.compareDocumentPosition(offeringEl) & Node.DOCUMENT_POSITION_FOLLOWING,
    ).toBeTruthy();
  });

  it('clicking Edit on an info item populates the form and switches to update mode', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplateWithItems]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplateWithItems);

    renderPage();
    await user.click(await screen.findByRole('button', { name: /sunday mass/i }));
    await screen.findByText('Attendance');

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0]);

    expect(screen.getByDisplayValue('Attendance')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /update item/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeInTheDocument();
  });

  it('saves an edited info item and shows success feedback', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplateWithItems]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplateWithItems);
    mockedUpdateServiceInfoItem.mockResolvedValueOnce({
      id: 10,
      title: 'Attendance Updated',
      required: true,
      serviceInfoItemType: 'NUMERICAL',
    });

    renderPage();
    await user.click(await screen.findByRole('button', { name: /sunday mass/i }));
    await screen.findByText('Attendance');

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0]);

    const titleField = screen.getByDisplayValue('Attendance');
    await user.clear(titleField);
    await user.type(titleField, 'Attendance Updated');

    await user.click(screen.getByRole('button', { name: /update item/i }));

    await screen.findByText('Info item updated.');
    expect(mockedUpdateServiceInfoItem).toHaveBeenCalledWith(
      10,
      sampleTemplateWithItems.id,
      expect.objectContaining({ title: 'Attendance Updated' }),
    );
    expect(screen.getByText('Attendance Updated')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add info item/i })).toBeInTheDocument();
  });

  it('cancelling edit resets the form to add mode', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplateWithItems]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplateWithItems);

    renderPage();
    await user.click(await screen.findByRole('button', { name: /sunday mass/i }));
    await screen.findByText('Attendance');

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0]);
    expect(screen.getByRole('button', { name: /update item/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /cancel/i }));

    expect(screen.queryByRole('button', { name: /update item/i })).not.toBeInTheDocument();
    expect(screen.getByRole('button', { name: /add info item/i })).toBeInTheDocument();
  });

  it('shows error feedback when updating an info item fails', async () => {
    const user = userEvent.setup();
    mockedGetServiceTemplates.mockResolvedValueOnce([sampleTemplateWithItems]);
    mockedGetServiceTemplateById.mockResolvedValueOnce(sampleTemplateWithItems);
    mockedUpdateServiceInfoItem.mockRejectedValueOnce(new Error('network error'));

    renderPage();
    await user.click(await screen.findByRole('button', { name: /sunday mass/i }));
    await screen.findByText('Attendance');

    const editButtons = screen.getAllByRole('button', { name: /^edit$/i });
    await user.click(editButtons[0]);
    await user.click(screen.getByRole('button', { name: /update item/i }));

    await screen.findByText('We could not save the info item right now.');
    expect(mockedUpdateServiceInfoItem).toHaveBeenCalled();
  });
});
