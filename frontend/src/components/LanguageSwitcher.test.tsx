import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { vi } from 'vitest';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import LanguageSwitcher from './LanguageSwitcher';

function renderLanguageSwitcher(overrides: Partial<AuthContextValue> = {}) {
  const value: AuthContextValue = {
    user: null,
    status: 'unauthenticated',
    authErrorKey: null,
    signIn: vi.fn(),
    reporterSignIn: vi.fn(),
    signOut: vi.fn(),
    refreshUser: vi.fn(),
    updatePreferredLanguage: vi.fn(),
    ...overrides,
  };

  return render(
    <AuthContext.Provider value={value}>
      <LanguageSwitcher />
    </AuthContext.Provider>,
  );
}

describe('LanguageSwitcher', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en');
  });

  afterEach(async () => {
    await i18n.changeLanguage('en');
  });

  it('renders EN and ES toggle buttons', () => {
    renderLanguageSwitcher();
    expect(screen.getByRole('button', { name: 'English' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Spanish' })).toBeInTheDocument();
  });

  it('marks EN as selected by default', () => {
    renderLanguageSwitcher();
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Spanish' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('switches to Spanish when ES is clicked', async () => {
    const user = userEvent.setup();
    renderLanguageSwitcher();
    await user.click(screen.getByRole('button', { name: 'Spanish' }));
    expect(i18n.language).toBe('es');
  });

  it('marks ES as selected after switching to Spanish', async () => {
    await i18n.changeLanguage('es');
    renderLanguageSwitcher();
    expect(screen.getByRole('button', { name: 'Español' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Inglés' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('saves the language preference for authenticated users', async () => {
    const user = userEvent.setup();
    const updatePreferredLanguage = vi.fn().mockResolvedValue(undefined);

    renderLanguageSwitcher({
      user: {
        id: 1,
        username: 'reporter',
        role: 'REPORTER',
        preferredLanguage: 'en',
        assignedChurchNames: ['Trinity'],
      },
      status: 'authenticated',
      updatePreferredLanguage,
    });

    await user.click(screen.getByRole('button', { name: 'Spanish' }));

    expect(updatePreferredLanguage).toHaveBeenCalledWith('es');
  });
});
