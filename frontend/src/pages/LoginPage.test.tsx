import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import LoginPage from './LoginPage';

function renderLoginPage(overrides: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = {
    user: null,
    status: 'unauthenticated',
    authErrorKey: null,
    signIn: vi.fn(),
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

  beforeEach(async () => {
    mockedSignIn.mockReset();
    await i18n.changeLanguage('en');
  });

  afterEach(async () => {
    await i18n.changeLanguage('en');
  });

  describe('English', () => {
    it('renders the Sign In button', () => {
      renderLoginPage();
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });

    it('renders Username and Password fields', () => {
      renderLoginPage();
      expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    });

    it('renders the app title and subtitle', () => {
      renderLoginPage();
      expect(screen.getByText('Diocese Dashboard')).toBeInTheDocument();
      expect(screen.getByText('Episcopal Church in Costa Rica')).toBeInTheDocument();
    });

    it('updates the login error when the language changes', async () => {
      const user = userEvent.setup();
      const unauthorizedError = { response: { status: 401 }, isAxiosError: true };
      mockedSignIn.mockRejectedValueOnce(unauthorizedError);

      renderLoginPage({ signIn: mockedSignIn });

      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'bad-password');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

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

      await user.type(screen.getByLabelText(/username/i), 'demo');
      await user.type(screen.getByLabelText(/password/i), 'secret');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      expect(
        await screen.findByText(
          'We cannot reach the server right now. If you are running the app locally, start the backend with "mvn package spring-boot:run" and try again.',
        ),
      ).toBeInTheDocument();
    });
  });

  describe('Spanish', () => {
    beforeEach(async () => {
      await i18n.changeLanguage('es');
    });

    it('renders the Iniciar sesión button', () => {
      renderLoginPage();
      expect(screen.getByRole('button', { name: /iniciar sesión/i })).toBeInTheDocument();
    });

    it('renders Usuario and Contraseña fields', () => {
      renderLoginPage();
      expect(screen.getByLabelText(/usuario/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/contraseña/i)).toBeInTheDocument();
    });

    it('renders the Spanish subtitle', () => {
      renderLoginPage();
      expect(screen.getByText('Iglesia Episcopal en Costa Rica')).toBeInTheDocument();
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
