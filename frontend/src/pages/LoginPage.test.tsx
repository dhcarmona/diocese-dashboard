import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { vi } from 'vitest';
import { login } from '../api/auth';
import LoginPage from './LoginPage';

vi.mock('../api/auth', () => ({
  login: vi.fn(),
}));

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  const mockedLogin = vi.mocked(login);

  beforeEach(async () => {
    mockedLogin.mockReset();
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
      mockedLogin.mockRejectedValueOnce(new Error('Invalid credentials'));

      renderLoginPage();

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
  });
});
