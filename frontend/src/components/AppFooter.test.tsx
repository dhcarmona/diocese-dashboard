import { render, screen } from '@testing-library/react';
import AppFooter from './AppFooter';

describe('AppFooter', () => {
  it('always renders the copyright text', () => {
    render(<AppFooter />);
    expect(screen.getByText(/Misión Santa Clara de Asís/)).toBeInTheDocument();
  });

  it('does not show build info by default', () => {
    render(<AppFooter />);
    expect(screen.queryByText(/^Build /)).not.toBeInTheDocument();
  });

  it('shows build info when showBuildInfo is true', () => {
    render(<AppFooter showBuildInfo />);
    expect(screen.getByText(/^Build /)).toBeInTheDocument();
  });
});
