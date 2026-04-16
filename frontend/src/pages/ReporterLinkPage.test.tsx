import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import type { ReactNode } from 'react';
import {
  getNextPublicReporterLink,
  getReporterLinkByToken,
  getReporterLinkPublic,
  submitViaReporterLink,
  submitViaReporterLinkPublic,
} from '../api/reporterLinks';
import { getServiceTemplateById } from '../api/serviceTemplates';
import { getCelebrants } from '../api/celebrants';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import ReporterLinkPage from './ReporterLinkPage';

vi.mock('../api/reporterLinks', () => ({
  getReporterLinkByToken: vi.fn(),
  getReporterLinkPublic: vi.fn(),
  getNextPublicReporterLink: vi.fn(),
  submitViaReporterLink: vi.fn(),
  submitViaReporterLinkPublic: vi.fn(),
}));
vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplateById: vi.fn(),
}));
vi.mock('../api/celebrants', () => ({
  getCelebrants: vi.fn(),
}));

const mockedGetReporterLinkByToken = vi.mocked(getReporterLinkByToken);
const mockedGetReporterLinkPublic = vi.mocked(getReporterLinkPublic);
const mockedGetNextPublicReporterLink = vi.mocked(getNextPublicReporterLink);
const mockedSubmitViaReporterLink = vi.mocked(submitViaReporterLink);
const mockedSubmitViaReporterLinkPublic = vi.mocked(submitViaReporterLinkPublic);
const mockedGetServiceTemplateById = vi.mocked(getServiceTemplateById);
const mockedGetCelebrants = vi.mocked(getCelebrants);

function makeAuthValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    user: null,
    status: 'unauthenticated',
    authErrorKey: null,
    signIn: async () => {},
    reporterSignIn: async () => {},
    redeemToken: async () => {},
    signOut: async () => {},
    refreshUser: async () => null,
    updatePreferredLanguage: async () => {},
    ...overrides,
  };
}

function renderPage(authOverrides: Partial<AuthContextValue> = {}) {
  function Wrapper({ children }: Readonly<{ children: ReactNode }>) {
    return (
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <AuthContext.Provider value={makeAuthValue(authOverrides)}>
          <MemoryRouter initialEntries={['/r/test-token']}>
            <Routes>
              <Route path="/r/:token" element={children} />
            </Routes>
          </MemoryRouter>
        </AuthContext.Provider>
      </LocalizationProvider>
    );
  }

  return render(<ReporterLinkPage />, { wrapper: Wrapper });
}

describe('ReporterLinkPage', () => {
  beforeEach(async () => {
    mockedGetReporterLinkByToken.mockReset();
    mockedGetReporterLinkPublic.mockReset();
    mockedGetNextPublicReporterLink.mockReset();
    mockedSubmitViaReporterLink.mockReset();
    mockedSubmitViaReporterLinkPublic.mockReset();
    mockedGetServiceTemplateById.mockReset();
    mockedGetCelebrants.mockReset();
    await i18n.changeLanguage('en');
  });

  it('offers the next pending reporter link after a public submission', async () => {
    mockedGetReporterLinkPublic.mockResolvedValue({
      id: 1,
      token: 'test-token',
      churchName: 'Trinity Church',
      serviceTemplateId: 10,
      serviceTemplateName: 'Sunday Eucharist',
      activeDate: '2024-04-14',
      serviceInfoItems: [],
      celebrants: [],
    });
    mockedSubmitViaReporterLinkPublic.mockResolvedValue({
      serviceInstanceId: 5,
      nextReporterLinkToken: null,
      nextReporterLinkFollowUpToken: 'follow-up-token',
      nextReporterLinkActiveDate: '2026-04-15',
    });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Open Next Pending Link' })).toBeInTheDocument();
    });
    expect(screen.queryByRole('link', { name: 'Back to Home' })).not.toBeInTheDocument();
  });

  it('shows the up-to-date message after a public submission with no pending links', async () => {
    mockedGetReporterLinkPublic.mockResolvedValue({
      id: 1,
      token: 'test-token',
      churchName: 'Trinity Church',
      serviceTemplateId: 10,
      serviceTemplateName: 'Sunday Eucharist',
      activeDate: '2024-04-14',
      serviceInfoItems: [],
      celebrants: [],
    });
    mockedSubmitViaReporterLinkPublic.mockResolvedValue({
      serviceInstanceId: 6,
      nextReporterLinkToken: null,
      nextReporterLinkFollowUpToken: null,
      nextReporterLinkActiveDate: null,
    });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(screen.getByText('You are up to date')).toBeInTheDocument();
    });

    expect(screen.getByTestId('ThumbUpAltOutlinedIcon')).toBeInTheDocument();
  });

  it('navigates to the next link in-app when the follow-up button is clicked', async () => {
    mockedGetReporterLinkPublic
      .mockResolvedValueOnce({
        id: 1,
        token: 'test-token',
        churchName: 'Trinity Church',
        serviceTemplateId: 10,
        serviceTemplateName: 'Sunday Eucharist',
        activeDate: '2024-04-14',
        serviceInfoItems: [],
        celebrants: [],
      })
      .mockResolvedValueOnce({
        id: 2,
        token: 'next-public-token',
        churchName: 'St. Luke',
        serviceTemplateId: 11,
        serviceTemplateName: 'Evening Prayer',
        activeDate: '2024-04-15',
        serviceInfoItems: [],
        celebrants: [],
      });
    mockedSubmitViaReporterLinkPublic.mockResolvedValue({
      serviceInstanceId: 5,
      nextReporterLinkToken: null,
      nextReporterLinkFollowUpToken: 'follow-up-token',
      nextReporterLinkActiveDate: '2026-04-15',
    });
    mockedGetNextPublicReporterLink.mockResolvedValue({
      nextReporterLinkToken: 'next-public-token',
      nextReporterLinkActiveDate: '2024-04-15',
    });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Open Next Pending Link' })).toBeInTheDocument();
    });

    await user.click(screen.getByRole('button', { name: 'Open Next Pending Link' }));

    await waitFor(() => {
      expect(screen.getByText('Evening Prayer')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /submit report/i })).toBeInTheDocument();
  });

  it('does not show the back-to-home button on the public reporter link form', async () => {
    mockedGetReporterLinkPublic.mockResolvedValue({
      id: 1,
      token: 'test-token',
      churchName: 'Trinity Church',
      serviceTemplateId: 10,
      serviceTemplateName: 'Sunday Eucharist',
      activeDate: '2024-04-14',
      serviceInfoItems: [],
      celebrants: [],
    });

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    expect(screen.queryByRole('link', { name: 'Back to Home' })).not.toBeInTheDocument();
  });

  it('shows the fixed link date and submits without a client-selected date', async () => {
    mockedGetReporterLinkPublic.mockResolvedValue({
      id: 1,
      token: 'test-token',
      churchName: 'Trinity Church',
      serviceTemplateId: 10,
      serviceTemplateName: 'Sunday Eucharist',
      activeDate: '2024-04-14',
      serviceInfoItems: [],
      celebrants: [],
    });
    mockedSubmitViaReporterLinkPublic.mockResolvedValue({
      serviceInstanceId: 5,
      nextReporterLinkToken: null,
      nextReporterLinkFollowUpToken: null,
      nextReporterLinkActiveDate: null,
    });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(
        screen.getByText(
          'This report will be recorded for 14 Apr 2024.',
        ),
      ).toBeInTheDocument();
    });

    const serviceDateField = screen.getByRole('textbox', { name: 'Service Date' });
    expect(serviceDateField).toHaveValue('14 Apr 2024');
    expect(serviceDateField).toHaveAttribute('readonly');

    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(mockedSubmitViaReporterLinkPublic).toHaveBeenCalledWith('test-token', {
        celebrantIds: [],
        responses: [],
      });
    });
  });
});
