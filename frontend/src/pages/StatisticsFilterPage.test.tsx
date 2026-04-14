import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import dayjs from 'dayjs';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { getChurches } from '../api/churches';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import StatisticsFilterPage from './StatisticsFilterPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('../api/churches', () => ({
  getChurches: vi.fn(),
}));

vi.mock('@mui/x-date-pickers/DatePicker', () => ({
  DatePicker: ({ label, onChange }: { label: string; onChange?: (v: unknown) => void }) => (
    <input
      aria-label={label}
      onChange={(e) => onChange?.(e.target.value ? dayjs(e.target.value) : null)}
    />
  ),
}));

const mockedGetChurches = vi.mocked(getChurches);

function renderPage(role: 'ADMIN' | 'REPORTER') {
  const value: AuthContextValue = {
    user: { id: 1, username: 'u', role, preferredLanguage: 'en', assignedChurchNames: ['Trinity'] },
    status: 'authenticated',
    authErrorKey: null,
    signIn: async () => {},
    reporterSignIn: async () => {},
    signOut: async () => {},
    refreshUser: async () => null,
    updatePreferredLanguage: async () => {},
  };

  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter initialEntries={['/statistics/1']}>
        <Routes>
          <Route path="/statistics/:templateId" element={<StatisticsFilterPage />} />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('StatisticsFilterPage', () => {
  beforeEach(async () => {
    mockedGetChurches.mockReset();
    mockNavigate.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows loading indicator while churches load', () => {
    mockedGetChurches.mockReturnValue(new Promise(() => {}));
    renderPage('ADMIN');

    expect(screen.getByText('Loading...')).toBeInTheDocument();
  });

  it('shows error alert when church loading fails', async () => {
    mockedGetChurches.mockRejectedValueOnce(new Error('fail'));
    renderPage('ADMIN');

    await waitFor(() => {
      expect(screen.getByText('Failed to load churches.')).toBeInTheDocument();
    });
  });

  it('admin sees Global option in the church dropdown', async () => {
    mockedGetChurches.mockResolvedValueOnce([
      { name: 'Trinity', location: null },
      { name: 'Grace', location: null },
    ]);
    renderPage('ADMIN');

    const user = userEvent.setup();
    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /church/i })).toBeInTheDocument();
    });
    await user.click(screen.getByRole('combobox', { name: /church/i }));

    await waitFor(() => {
      expect(
        screen.getByRole('option', { name: 'All Churches (Global Report)' }),
      ).toBeInTheDocument();
    });
    expect(screen.getByRole('option', { name: 'Trinity' })).toBeInTheDocument();
  });

  it('reporter does not see Global option', async () => {
    mockedGetChurches.mockResolvedValueOnce([{ name: 'Trinity', location: null }]);
    renderPage('REPORTER');

    await waitFor(() => {
      expect(screen.queryByText('All Churches (Global Report)')).not.toBeInTheDocument();
    });
  });

  it('shows a Generate Report button', async () => {
    mockedGetChurches.mockResolvedValueOnce([{ name: 'Trinity', location: null }]);
    renderPage('REPORTER');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /generate report/i })).toBeInTheDocument();
    });
  });

  it('admin defaults to Global option selected after loading', async () => {
    mockedGetChurches.mockResolvedValueOnce([{ name: 'Trinity', location: null }]);
    renderPage('ADMIN');

    await waitFor(() => {
      expect(screen.getByRole('combobox', { name: /church/i })).toBeInTheDocument();
    });
    expect(screen.getByRole('combobox', { name: /church/i })).toHaveTextContent(
      'All Churches (Global Report)',
    );
  });

  it('generate navigates without churchName when global is selected', async () => {
    mockedGetChurches.mockResolvedValueOnce([{ name: 'Trinity', location: null }]);
    renderPage('ADMIN');

    const user = userEvent.setup();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /generate report/i })).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: /generate report/i }));

    expect(mockNavigate).toHaveBeenCalledWith(
      expect.stringMatching(/^\/statistics\/1\/report\?/),
    );
    expect(mockNavigate.mock.calls[0][0] as string).not.toContain('churchName');
  });

  it('generate navigates with churchName when a specific church is selected', async () => {
    mockedGetChurches.mockResolvedValueOnce([{ name: 'Trinity', location: null }]);
    renderPage('REPORTER');

    const user = userEvent.setup();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /generate report/i })).toBeInTheDocument();
    });
    await user.click(screen.getByRole('button', { name: /generate report/i }));

    expect(mockNavigate).toHaveBeenCalledWith(expect.stringContaining('churchName=Trinity'));
  });

  it('shows date validation error when end date is before start date', async () => {
    mockedGetChurches.mockResolvedValueOnce([{ name: 'Trinity', location: null }]);
    renderPage('REPORTER');

    const user = userEvent.setup();
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /generate report/i })).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText('Start Date'), { target: { value: '2024-12-01' } });
    fireEvent.change(screen.getByLabelText('End Date'), { target: { value: '2024-01-01' } });
    await user.click(screen.getByRole('button', { name: /generate report/i }));

    expect(screen.getByText('End date must be after start date.')).toBeInTheDocument();
  });
});
