import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import LanguageSwitcher from './LanguageSwitcher';

describe('LanguageSwitcher', () => {
  beforeEach(async () => {
    await i18n.changeLanguage('en');
  });

  afterEach(async () => {
    await i18n.changeLanguage('en');
  });

  it('renders EN and ES toggle buttons', () => {
    render(<LanguageSwitcher />);
    expect(screen.getByRole('button', { name: 'English' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Spanish' })).toBeInTheDocument();
  });

  it('marks EN as selected by default', () => {
    render(<LanguageSwitcher />);
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'Spanish' })).toHaveAttribute('aria-pressed', 'false');
  });

  it('switches to Spanish when ES is clicked', async () => {
    const user = userEvent.setup();
    render(<LanguageSwitcher />);
    await user.click(screen.getByRole('button', { name: 'Spanish' }));
    expect(i18n.language).toBe('es');
  });

  it('marks ES as selected after switching to Spanish', async () => {
    await i18n.changeLanguage('es');
    render(<LanguageSwitcher />);
    expect(screen.getByRole('button', { name: 'Spanish' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('button', { name: 'English' })).toHaveAttribute('aria-pressed', 'false');
  });
});
