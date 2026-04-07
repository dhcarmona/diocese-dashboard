import { render, screen } from '@testing-library/react';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import ReportsHubPage from './ReportsHubPage';

describe('ReportsHubPage', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en');
  });

  it('renders the hub title', () => {
    render(
      <MemoryRouter>
        <ReportsHubPage />
      </MemoryRouter>,
    );

    expect(screen.getByRole('heading', { name: 'Reports' })).toBeInTheDocument();
  });

  it('renders a link to the individual reports page', () => {
    render(
      <MemoryRouter>
        <ReportsHubPage />
      </MemoryRouter>,
    );

    const link = screen.getByRole('link', { name: /individual report/i });
    expect(link).toHaveAttribute('href', '/reports/view/individual');
  });
});
