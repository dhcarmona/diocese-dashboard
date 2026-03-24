import { render, screen } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import HomePage from './HomePage';

function renderHomePage(overrides: Partial<AuthContextValue>) {
  const value: AuthContextValue = {
    user: {
      id: 1,
      username: 'demo',
      role: 'REPORTER',
      assignedChurchNames: ['Trinity'],
    },
    status: 'authenticated',
    authErrorKey: null,
    signIn: async () => {},
    signOut: async () => {},
    refreshUser: async () => null,
    ...overrides,
  };

  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('HomePage', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en');
  });

  it('shows only the reporting tile for reporters', () => {
    renderHomePage({});

    expect(screen.getByRole('link', { name: /submit a service report/i })).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: /create service templates/i }),
    ).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /manage churches/i })).not.toBeInTheDocument();
  });

  it('shows reporter and admin tiles for admins', () => {
    renderHomePage({
      user: {
        id: 2,
        username: 'admin',
        role: 'ADMIN',
        assignedChurchNames: [],
      },
    });

    expect(screen.getByRole('link', { name: /submit a service report/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create service templates/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create reporter users/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /manage celebrants/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /manage churches/i })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /create reporter links/i })).toBeInTheDocument();
  });
});
