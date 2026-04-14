import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { getChurches } from '../api/churches';
import {
  createReporterUser,
  deleteReporterUser,
  getReporterUsers,
  updateReporterUser,
} from '../api/users';
import ReporterUserManagementPage from './ReporterUserManagementPage';

vi.mock('../api/users', () => ({
  getReporterUsers: vi.fn(),
  createReporterUser: vi.fn(),
  updateReporterUser: vi.fn(),
  deleteReporterUser: vi.fn(),
}));

vi.mock('../api/churches', () => ({
  getChurches: vi.fn(),
}));

const mockUser1 = {
  id: 1,
  username: 'jsmith',
  fullName: 'John Smith',
  phoneNumber: '+50688881234',
  preferredLanguage: 'en' as const,
  role: 'REPORTER' as const,
  enabled: true,
  assignedChurches: [{ name: 'Grace Church' }],
};

const mockUser2 = {
  id: 2,
  username: 'adoe',
  fullName: 'Anne Doe',
  phoneNumber: '+50688885678',
  preferredLanguage: 'en' as const,
  role: 'REPORTER' as const,
  enabled: true,
  assignedChurches: [{ name: 'St. Paul' }, { name: 'Trinity' }],
};

describe('ReporterUserManagementPage', () => {
  const mockedGetReporterUsers = vi.mocked(getReporterUsers);
  const mockedCreateReporterUser = vi.mocked(createReporterUser);
  const mockedUpdateReporterUser = vi.mocked(updateReporterUser);
  const mockedDeleteReporterUser = vi.mocked(deleteReporterUser);
  const mockedGetChurches = vi.mocked(getChurches);

  beforeEach(async () => {
    mockedGetReporterUsers.mockReset();
    mockedCreateReporterUser.mockReset();
    mockedUpdateReporterUser.mockReset();
    mockedDeleteReporterUser.mockReset();
    mockedGetChurches.mockResolvedValue([
      { name: 'Grace Church', mainCelebrant: null, location: null },
      { name: 'St. Paul', mainCelebrant: null, location: null },
      { name: 'Trinity', mainCelebrant: null, location: null },
    ]);
    await i18n.changeLanguage('en');
  });

  function renderPage() {
    return render(
      <MemoryRouter>
        <ReporterUserManagementPage />
      </MemoryRouter>,
    );
  }

  it('renders the reporter directory after loading', async () => {
    mockedGetReporterUsers.mockResolvedValueOnce([mockUser1, mockUser2]);

    renderPage();

    expect(screen.getByText('Loading reporters...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /john smith/i })).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /anne doe/i })).toBeInTheDocument();
    expect(screen.getByText('Total reporter users: 2')).toBeInTheDocument();
    expect(screen.getByText('Phone: +50688881234')).toBeInTheDocument();
  });

  it('shows empty state when there are no reporters', async () => {
    mockedGetReporterUsers.mockResolvedValueOnce([]);
    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('No reporter users have been added yet.'),
      ).toBeInTheDocument();
    });
  });

  it('creates a reporter and returns the form to create mode', async () => {
    const user = userEvent.setup();
    mockedGetReporterUsers.mockResolvedValueOnce([]);
    mockedCreateReporterUser.mockResolvedValueOnce(mockUser1);

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('No reporter users have been added yet.'),
      ).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Username'), 'jsmith');
    await user.type(screen.getByLabelText('Full name'), 'John Smith');
    await user.type(screen.getByLabelText('WhatsApp phone number'), '88881234');

    // Type in the church autocomplete and select from the dropdown
    const churchInput = screen.getByLabelText('Assigned churches');
    await user.type(churchInput, 'Grace');
    await user.click(await screen.findByRole('option', { name: 'Grace Church' }));

    await user.click(screen.getByRole('button', { name: /create reporter/i }));

    await waitFor(() => {
      expect(mockedCreateReporterUser).toHaveBeenCalledWith({
        username: 'jsmith',
        fullName: 'John Smith',
        phoneNumber: expect.stringContaining('+506'),
        churchNames: ['Grace Church'],
      });
    });

    expect(await screen.findByText('John Smith was created.')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Add Reporter' })).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toHaveValue('');
  });

  it('populates the form when a reporter is selected and allows editing', async () => {
    const user = userEvent.setup();
    mockedGetReporterUsers.mockResolvedValueOnce([mockUser1]);
    mockedUpdateReporterUser.mockResolvedValueOnce({
      ...mockUser1,
      fullName: 'Jonathan Smith',
    });

    renderPage();

    const listButton = await screen.findByRole('button', { name: /john smith/i });
    await user.click(listButton);

    expect(screen.getByLabelText('Username')).toBeDisabled();
    expect(screen.getByLabelText('Full name')).toHaveValue('John Smith');

    const fullNameField = screen.getByLabelText('Full name');
    await user.clear(fullNameField);
    await user.type(fullNameField, 'Jonathan Smith');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(mockedUpdateReporterUser).toHaveBeenCalledWith(1, expect.objectContaining({
        fullName: 'Jonathan Smith',
      }));
    });

    expect(await screen.findByText('Jonathan Smith was updated.')).toBeInTheDocument();
  });

  it('deletes a reporter after selecting them', async () => {
    const user = userEvent.setup();
    mockedGetReporterUsers.mockResolvedValueOnce([mockUser1]);
    mockedDeleteReporterUser.mockResolvedValueOnce(undefined);

    renderPage();

    const listButton = await screen.findByRole('button', { name: /john smith/i });
    await user.click(listButton);

    await user.click(screen.getByRole('button', { name: /delete reporter/i }));

    await waitFor(() => {
      expect(mockedDeleteReporterUser).toHaveBeenCalledWith(1);
    });

    expect(await screen.findByText('John Smith was removed.')).toBeInTheDocument();
    expect(
      screen.queryByRole('button', { name: /john smith/i }),
    ).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Add Reporter' })).toBeInTheDocument();
  });

  it('shows a load error when the API call fails', async () => {
    mockedGetReporterUsers.mockRejectedValueOnce(new Error('network error'));

    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText('We could not load the reporter users right now.'),
      ).toBeInTheDocument();
    });
  });

  it('validates required fields before saving', async () => {
    const user = userEvent.setup();
    mockedGetReporterUsers.mockResolvedValueOnce([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /create reporter/i })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /create reporter/i }));
    expect(screen.getByText('Enter a username before saving.')).toBeInTheDocument();
  });
});
