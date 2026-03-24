import { render, screen } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';

function renderLoginPage() {
  return render(
    <MemoryRouter>
      <LoginPage />
    </MemoryRouter>,
  );
}

describe('LoginPage', () => {
  beforeEach(async () => {
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
