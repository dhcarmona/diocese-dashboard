import { render, screen, waitFor } from '@testing-library/react';
import i18n from 'i18next';
import { vi } from 'vitest';
import App from './App';
import { fetchAuthenticatedUser, logout } from './api/auth';
import { getServiceTemplates } from './api/serviceTemplates';

vi.mock('./api/auth', async () => {
  const actual = await vi.importActual<typeof import('./api/auth')>('./api/auth');
  return {
    ...actual,
    fetchAuthenticatedUser: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
  };
});

vi.mock('./api/serviceTemplates', () => ({
  getServiceTemplates: vi.fn(),
}));

describe('App routing', () => {
  const mockedFetchAuthenticatedUser = vi.mocked(fetchAuthenticatedUser);
  const mockedGetServiceTemplates = vi.mocked(getServiceTemplates);
  const mockedLogout = vi.mocked(logout);

  beforeEach(async () => {
    mockedFetchAuthenticatedUser.mockReset();
    mockedGetServiceTemplates.mockReset();
    mockedLogout.mockReset();
    window.history.pushState({}, '', '/');
    await i18n.changeLanguage('en');
  });

  it('redirects unauthenticated users to the login page', async () => {
    mockedFetchAuthenticatedUser.mockResolvedValueOnce(null);

    render(<App />);

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /^send login link$/i })).toBeInTheDocument();
    });
  });

  it('shows a backend unavailable message when the startup session check gets a 502', async () => {
    mockedFetchAuthenticatedUser.mockRejectedValueOnce({ response: { status: 502 }, isAxiosError: true });

    render(<App />);

    await waitFor(() => {
      expect(
        screen.getByText(
          'We cannot reach the server right now. If you are running the app locally, start the backend with "mvn package spring-boot:run" and try again.',
        ),
      ).toBeInTheDocument();
    });
  });

  it('renders the admin home page for authenticated admins', async () => {
    mockedFetchAuthenticatedUser.mockResolvedValueOnce({
      id: 1,
      username: 'admin',
      role: 'ADMIN',
      preferredLanguage: 'en',
      assignedChurchNames: [],
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /create reporter users/i })).toBeInTheDocument();
    });
  });

  it('redirects reporters away from admin-only routes', async () => {
    window.history.pushState({}, '', '/users/manage');
    mockedFetchAuthenticatedUser.mockResolvedValueOnce({
      id: 2,
      username: 'reporter',
      role: 'REPORTER',
      preferredLanguage: 'en',
      assignedChurchNames: ['Trinity'],
    });

    render(<App />);

    await waitFor(() => {
      expect(screen.getByRole('link', { name: /submit a service report/i })).toBeInTheDocument();
    });

    expect(screen.queryByText('Reporter Users')).not.toBeInTheDocument();
  });
});
