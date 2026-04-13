import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { requestReporterOtp } from '../api/auth';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import LoginPage from './LoginPage';

vi.mock('../api/auth', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/auth')>();
  return {
    ...actual,
    requestReporterOtp: vi.fn(),
  };
});

function renderLoginPage(overrides: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = {
    user: null,
    status: 'unauthenticated',
    authErrorKey: null,
    signIn: vi.fn(),
    reporterSignIn: vi.fn(),
    signOut: vi.fn(),
    refreshUser: vi.fn(),
    ...overrides,
  };

  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('LoginPage', () => {
  const mockedSignIn = vi.fn();
  const mockedRequestReporterOtp = vi.mocked(requestReporterOtp);

  beforeEach(async () => {
    mockedSignIn.mockReset();
    mockedRequestReporterOtp.mockReset();
    await i18n.changeLanguage('en');
  });

  afterEach(async () => {
    await i18n.changeLanguage('en');
  });

  describe('English', () => {
    it('renders the Send Code button by default (reporter mode)', () => {
      renderLoginPage();
      expect(screen.getByRole('button', { name: /^send code$/i })).toBeInTheDocument();
      expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
    });

    it('renders the Username field and admin escape-hatch link by default', () => {
      renderLoginPage();
      expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /admin\? sign in with password/i })).toBeInTheDocument();
    });

    it('renders the app title and subtitle', () => {
      renderLoginPage();
      expect(screen.getByText('Diocese Dashboard')).toBeInTheDocument();
      expect(screen.getByText('Enter your username to receive a code via WhatsApp.')).toBeInTheDocument();
    });

    it('switches to admin mode and back when the links are clicked', async () => {
      const user = userEvent.setup();
      renderLoginPage();

      await user.click(screen.getByRole('button', { name: /admin\? sign in with password/i }));
      expect(screen.getByRole('button', { name: /^sign in$/i })).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();

      await user.click(screen.getByRole('button', { name: /back to reporter login/i }));
      expect(screen.getByRole('button', { name: /^send code$/i })).toBeInTheDocument();
      expect(screen.queryByLabelText(/password/i)).not.toBeInTheDocument();
    });

    it('always advances to the verify screen after submitting a username', async () => {
      const user = userEvent.setup();
      mockedRequestReporterOtp.mockResolvedValueOnce(undefined);

      renderLoginPage();
      await user.type(screen.getByLabelText(/username/i), 'reporter1');
      await user.click(screen.getByRole('button', { name: /^send code$/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /verify code/i })).toBeInTheDocument();
      });
      expect(
        screen.getByText(/if reporter1 is a registered reporter account/i),
      ).toBeInTheDocument();
    });

    it('advances to the verify screen even if the username does not exist (prevents account enumeration)', async () => {
      const user = userEvent.setup();
      mockedRequestReporterOtp.mockRejectedValueOnce({
        response: { status: 401 },
        isAxiosError: true,
      });

      renderLoginPage();
      await user.type(screen.getByLabelText(/username/i), 'ghost');
      await user.click(screen.getByRole('button', { name: /^send code$/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /verify code/i })).toBeInTheDocument();
      });
      expect(screen.queryByText(/no reporter account/i)).not.toBeInTheDocument();
    });

    it('shows "try a different username" link on verify screen that returns to username form', async () => {
      const user = userEvent.setup();
      mockedRequestReporterOtp.mockResolvedValueOnce(undefined);

      renderLoginPage();
      await user.type(screen.getByLabelText(/username/i), 'reporter1');
      await user.click(screen.getByRole('button', { name: /^send code$/i }));

      await waitFor(() => {
        expect(
          screen.getByRole('button', { name: /try a different username/i }),
        ).toBeInTheDocument();
      });

      await user.click(screen.getByRole('button', { name: /try a different username/i }));
      expect(screen.getByRole('button', { name: /^send code$/i })).toBeInTheDocument();
    });

    it('submits admin credentials and shows an error on bad password', async () => {
      const user = userEvent.setup();
      const unauthorizedError = { response: { status: 401 }, isAxiosError: true };
      mockedSignIn.mockRejectedValueOnce(unauthorizedError);

      renderLoginPage({ signIn: mockedSignIn });

      // Switch to admin mode first
      await user.click(screen.getByRole('button', { name: /admin\? sign in with password/i }));
      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'bad-password');
      await user.click(screen.getByRole('button', { name: /^sign in$/i }));

      expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument();
    });

    it('updates the admin login error when the language changes', async () => {
      const user = userEvent.setup();
      const unauthorizedError = { response: { status: 401 }, isAxiosError: true };
      mockedSignIn.mockRejectedValueOnce(unauthorizedError);

      renderLoginPage({ signIn: mockedSignIn });

      await user.click(screen.getByRole('button', { name: /admin\? sign in with password/i }));
      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'bad-password');
      await user.click(screen.getByRole('button', { name: /^sign in$/i }));

      expect(await screen.findByText('Invalid username or password.')).toBeInTheDocument();

      await act(async () => {
        await i18n.changeLanguage('es');
      });

      await waitFor(() => {
        expect(screen.getByText('Usuario o contraseña incorrectos.')).toBeInTheDocument();
      });
    });

    it('shows a backend unavailable message when the API proxy cannot reach Spring Boot', async () => {
      const user = userEvent.setup();
      mockedSignIn.mockRejectedValueOnce({ response: { status: 502 }, isAxiosError: true });

      renderLoginPage({ signIn: mockedSignIn });

      await user.click(screen.getByRole('button', { name: /admin\? sign in with password/i }));
      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'secret');
      await user.click(screen.getByRole('button', { name: /^sign in$/i }));

      expect(
        await screen.findByText(
          'We cannot reach the server right now. If you are running the app locally, start the backend with "mvn package spring-boot:run" and try again.',
        ),
      ).toBeInTheDocument();
    });

    it('shows admin lockout time when login returns 429', async () => {
      const user = userEvent.setup();
      mockedSignIn.mockRejectedValueOnce({
        response: { status: 429, headers: { 'retry-after': '600' } },
        isAxiosError: true,
      });

      renderLoginPage({ signIn: mockedSignIn });

      await user.click(screen.getByRole('button', { name: /admin\? sign in with password/i }));
      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'secret');
      await user.click(screen.getByRole('button', { name: /^sign in$/i }));

      expect(
        await screen.findByText('Too many attempts. Please wait 10 minutes before trying again.'),
      ).toBeInTheDocument();
    });

    it('shows reporter OTP lockout time when verification returns 429', async () => {
      const user = userEvent.setup();
      const mockedReporterSignIn = vi.fn().mockRejectedValueOnce({
        response: { status: 429, headers: { 'retry-after': '600' } },
        isAxiosError: true,
      });

      renderLoginPage({ reporterSignIn: mockedReporterSignIn });

      await user.type(screen.getByLabelText(/username/i), 'reporter1');
      await user.click(screen.getByRole('button', { name: /^send code$/i }));
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /verify code/i })).toBeInTheDocument();
      });
      await user.type(screen.getByLabelText(/verification code/i), '123456');
      await user.click(screen.getByRole('button', { name: /verify code/i }));

      expect(
        await screen.findByText('Too many attempts. Please wait 10 minutes before trying again.'),
      ).toBeInTheDocument();
    });

    it('updates the lockout message when the language changes', async () => {
      const user = userEvent.setup();
      mockedSignIn.mockRejectedValueOnce({
        response: { status: 429, headers: { 'retry-after': '600' } },
        isAxiosError: true,
      });

      renderLoginPage({ signIn: mockedSignIn });

      await user.click(screen.getByRole('button', { name: /admin\? sign in with password/i }));
      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'secret');
      await user.click(screen.getByRole('button', { name: /^sign in$/i }));

      expect(
        await screen.findByText('Too many attempts. Please wait 10 minutes before trying again.'),
      ).toBeInTheDocument();

      await act(async () => {
        await i18n.changeLanguage('es');
      });

      await waitFor(() => {
        expect(
          screen.getByText(
            'Demasiados intentos. Espere 10 minutos antes de volver a intentarlo.',
          ),
        ).toBeInTheDocument();
      });
    });
  });

  describe('Spanish', () => {
    beforeEach(async () => {
      await i18n.changeLanguage('es');
    });

    it('renders the Enviar código button by default (reporter mode)', () => {
      renderLoginPage();
      expect(screen.getByRole('button', { name: /^enviar código$/i })).toBeInTheDocument();
    });

    it('renders the Spanish reporter subtitle by default', () => {
      renderLoginPage();
      expect(screen.getByText('Ingrese su usuario para recibir un código por WhatsApp.')).toBeInTheDocument();
    });

    it('renders the admin escape-hatch link in Spanish', () => {
      renderLoginPage();
      expect(
        screen.getByRole('button', { name: /¿administrador\? inicie sesión con contraseña/i }),
      ).toBeInTheDocument();
    });

    it('renders the translated session error', () => {
      renderLoginPage({
        status: 'error',
        authErrorKey: 'auth.backendUnavailable',
        signIn: mockedSignIn,
      });
      expect(
        screen.getByText(
          'No se puede establecer conexión con el servidor en este momento. Si está ejecutando la aplicación localmente, inicie el backend con "mvn package spring-boot:run" y vuelva a intentarlo.',
        ),
      ).toBeInTheDocument();
    });
  });
});
