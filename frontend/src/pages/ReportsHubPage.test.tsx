import { render, screen } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import ReportsHubPage from './ReportsHubPage';

function renderPage(role: 'ADMIN' | 'REPORTER') {
  const value: AuthContextValue = {
    user: { id: 1, username: 'u', role, preferredLanguage: 'en', assignedChurchNames: [] },
    status: 'authenticated',
    authErrorKey: null,
    signIn: async () => {},
    reporterSignIn: async () => {},
    redeemToken: async () => {},
    signOut: async () => {},
    refreshUser: async () => null,
    updatePreferredLanguage: async () => {},
  };

  return render(
    <AuthContext.Provider value={value}>
      <MemoryRouter>
        <ReportsHubPage />
      </MemoryRouter>
    </AuthContext.Provider>,
  );
}

describe('ReportsHubPage', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en');
  });

  it('renders the hub title', () => {
    renderPage('REPORTER');

    expect(screen.getByRole('heading', { name: 'Reports' })).toBeInTheDocument();
  });

  it('renders a link to the individual reports page', () => {
    renderPage('REPORTER');

    const link = screen.getByRole('link', { name: /individual report/i });
    expect(link).toHaveAttribute('href', '/reports/view/individual');
  });

  it('renders a link to the statistics page', () => {
    renderPage('REPORTER');

    const link = screen.getByRole('link', { name: /statistics/i });
    expect(link).toHaveAttribute('href', '/statistics');
  });

  it('shows the convention report tile to admins', () => {
    renderPage('ADMIN');

    const link = screen.getByRole('link', { name: /convention report/i });
    expect(link).toHaveAttribute('href', '/convention-report');
  });

  it('hides the convention report tile from reporters', () => {
    renderPage('REPORTER');

    expect(screen.queryByRole('link', { name: /convention report/i })).not.toBeInTheDocument();
  });
});
