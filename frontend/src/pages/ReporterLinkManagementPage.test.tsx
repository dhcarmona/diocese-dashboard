import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getChurches } from '../api/churches';
import {
  createLinkSchedule,
  deleteLinkSchedule,
  getLinkSchedules,
  updateLinkSchedule,
} from '../api/linkSchedules';
import { createReporterLinksBulk, getReporterLinks, revokeReporterLink } from '../api/reporterLinks';
import { getServiceTemplates } from '../api/serviceTemplates';
import ReporterLinkManagementPage from './ReporterLinkManagementPage';

vi.mock('../api/churches', () => ({ getChurches: vi.fn() }));
vi.mock('../api/linkSchedules', () => ({
  ALL_DAYS: ['MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY'],
  getLinkSchedules: vi.fn(),
  createLinkSchedule: vi.fn(),
  updateLinkSchedule: vi.fn(),
  deleteLinkSchedule: vi.fn(),
}));
vi.mock('../api/reporterLinks', () => ({
  getReporterLinks: vi.fn(),
  createReporterLinksBulk: vi.fn(),
  revokeReporterLink: vi.fn(),
}));
vi.mock('../api/serviceTemplates', () => ({ getServiceTemplates: vi.fn() }));

const mockedGetChurches = vi.mocked(getChurches);
const mockedGetLinkSchedules = vi.mocked(getLinkSchedules);
const mockedCreateLinkSchedule = vi.mocked(createLinkSchedule);
const mockedUpdateLinkSchedule = vi.mocked(updateLinkSchedule);
const mockedDeleteLinkSchedule = vi.mocked(deleteLinkSchedule);
const mockedGetReporterLinks = vi.mocked(getReporterLinks);
const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);

const TEMPLATE = { id: 1, serviceTemplateName: 'Sunday Eucharist', linkOnly: false };

const CHURCH_A = { name: 'Church A', location: 'San José', mainCelebrant: null, portraitUrl: '' };
const CHURCH_B = { name: 'Church B', location: 'Heredia', mainCelebrant: null, portraitUrl: '' };

const SCHEDULE = {
  id: 10,
  serviceTemplateId: 1,
  serviceTemplateName: 'Sunday Eucharist',
  churchNames: ['Church A'],
  daysOfWeek: ['MONDAY' as const],
  sendHour: 8,
  lastTriggeredDate: null,
  createdAt: '2024-01-01T00:00:00Z',
};

function renderPage() {
  render(
    <LocalizationProvider dateAdapter={AdapterDayjs}>
      <MemoryRouter>
        <ReporterLinkManagementPage />
      </MemoryRouter>
    </LocalizationProvider>,
  );
}

describe('ReporterLinkManagementPage – schedule section', () => {
  beforeEach(async () => {
    mockedGetChurches.mockReset();
    mockedGetLinkSchedules.mockReset();
    mockedCreateLinkSchedule.mockReset();
    mockedUpdateLinkSchedule.mockReset();
    mockedDeleteLinkSchedule.mockReset();
    mockedGetReporterLinks.mockReset();
    mockedGetServiceTemplates.mockReset();
    vi.mocked(createReporterLinksBulk).mockReset();
    vi.mocked(revokeReporterLink).mockReset();

    mockedGetServiceTemplates.mockResolvedValue([TEMPLATE]);
    mockedGetChurches.mockResolvedValue([CHURCH_A, CHURCH_B]);
    mockedGetReporterLinks.mockResolvedValue([]);
    await i18n.changeLanguage('en');
  });

  it('shows empty state when there are no schedules', async () => {
    mockedGetLinkSchedules.mockResolvedValue([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Scheduled Links')).toBeInTheDocument();
    });

    expect(screen.getByText('No link schedules configured.')).toBeInTheDocument();
  });

  it('renders existing schedules with their details', async () => {
    mockedGetLinkSchedules.mockResolvedValue([SCHEDULE]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    expect(screen.getByText(/Days: Monday/)).toBeInTheDocument();
    expect(screen.getByText('Time: 8:00 (Costa Rica)')).toBeInTheDocument();
    expect(screen.getByText('Never sent yet')).toBeInTheDocument();
  });

  it('deletes a schedule and removes it from the list', async () => {
    mockedGetLinkSchedules.mockResolvedValue([SCHEDULE]);
    mockedDeleteLinkSchedule.mockResolvedValue();

    renderPage();

    const deleteBtn = await screen.findByRole('button', { name: /delete schedule/i });
    await userEvent.click(deleteBtn);

    await waitFor(() => {
      expect(mockedDeleteLinkSchedule).toHaveBeenCalledWith(10);
    });

    expect(screen.queryByText('Sunday Eucharist')).not.toBeInTheDocument();
    expect(screen.getByText('Schedule removed.')).toBeInTheDocument();
  });

  it('creates a new schedule via the Schedule button and dialog', async () => {
    mockedGetLinkSchedules.mockResolvedValue([]);
    mockedCreateLinkSchedule.mockResolvedValue({ ...SCHEDULE, id: 20 });

    renderPage();

    await screen.findByText('Scheduled Links');

    // Select template
    const templateSelect = screen.getByRole('combobox', { name: /service template/i });
    await userEvent.click(templateSelect);
    await userEvent.click(await screen.findByRole('option', { name: 'Sunday Eucharist' }));

    // Select a church chip — use getAllByRole and pick by name to avoid ambiguity
    const churchChips = await screen.findAllByRole('button', { name: 'Church A' });
    await userEvent.click(churchChips[0]);

    // Click Schedule button
    await userEvent.click(screen.getByRole('button', { name: /^schedule$/i }));

    // Dialog should open — pick Monday
    const dialog = await screen.findByRole('dialog');
    await userEvent.click(within(dialog).getByRole('checkbox', { name: /monday/i }));

    // Confirm
    await userEvent.click(within(dialog).getByRole('button', { name: /save schedule/i }));

    await waitFor(() => {
      expect(mockedCreateLinkSchedule).toHaveBeenCalledWith(
        expect.objectContaining({
          serviceTemplateId: 1,
          churchNames: ['Church A'],
          daysOfWeek: expect.arrayContaining(['MONDAY']),
          sendHour: 8,
        }),
      );
    });

    expect(screen.getByText('Schedule saved successfully.')).toBeInTheDocument();
  });

  it('edits a schedule, including changing its churches', async () => {
    mockedGetLinkSchedules.mockResolvedValue([SCHEDULE]);
    mockedUpdateLinkSchedule.mockResolvedValue({
      ...SCHEDULE,
      churchNames: ['Church A', 'Church B'],
      daysOfWeek: ['MONDAY', 'WEDNESDAY'],
    });

    renderPage();

    const editBtn = await screen.findByRole('button', { name: /edit schedule/i });
    await userEvent.click(editBtn);

    const dialog = await screen.findByRole('dialog');
    expect(dialog).toBeInTheDocument();

    // Church B chip should be visible inside the dialog and currently unselected
    const churchBChip = within(dialog).getByRole('button', { name: 'Church B' });
    await userEvent.click(churchBChip);

    // Also add Wednesday
    await userEvent.click(within(dialog).getByRole('checkbox', { name: /wednesday/i }));

    await userEvent.click(within(dialog).getByRole('button', { name: /save schedule/i }));

    await waitFor(() => {
      expect(mockedUpdateLinkSchedule).toHaveBeenCalledWith(
        10,
        expect.objectContaining({
          churchNames: expect.arrayContaining(['Church A', 'Church B']),
          daysOfWeek: expect.arrayContaining(['MONDAY', 'WEDNESDAY']),
        }),
      );
    });

    expect(screen.getByText('Schedule updated successfully.')).toBeInTheDocument();
  });

  it('shows an error when deleting a schedule fails', async () => {
    mockedGetLinkSchedules.mockResolvedValue([SCHEDULE]);
    mockedDeleteLinkSchedule.mockRejectedValue(new Error('Server error'));

    renderPage();

    const deleteBtn = await screen.findByRole('button', { name: /delete schedule/i });
    await userEvent.click(deleteBtn);

    expect(await screen.findByText('We could not remove the schedule right now.')).toBeInTheDocument();
    // Schedule still present
    expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
  });
});
