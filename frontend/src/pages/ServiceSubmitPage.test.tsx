import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import type { ReactNode } from 'react';
import { getCelebrants } from '../api/celebrants';
import { getChurches } from '../api/churches';
import { getServiceTemplateById, type ServiceTemplate } from '../api/serviceTemplates';
import { submitServiceInstance } from '../api/serviceInstances';
import { AuthContext, type AuthContextValue } from '../auth/auth-context';
import ServiceSubmitPage from './ServiceSubmitPage';

vi.mock('../api/serviceTemplates', () => ({
  getServiceTemplateById: vi.fn(),
}));
vi.mock('../api/celebrants', () => ({
  getCelebrants: vi.fn(),
}));
vi.mock('../api/churches', () => ({
  getChurches: vi.fn(),
}));
vi.mock('../api/serviceInstances', () => ({
  submitServiceInstance: vi.fn(),
}));

const mockedGetTemplate = vi.mocked(getServiceTemplateById);
const mockedGetCelebrants = vi.mocked(getCelebrants);
const mockedGetChurches = vi.mocked(getChurches);
const mockedSubmit = vi.mocked(submitServiceInstance);

const TEMPLATE: ServiceTemplate = {
  id: 1,
  serviceTemplateName: 'Sunday Eucharist',
  serviceInfoItems: [
    { id: 10, title: 'Attendance', serviceInfoItemType: 'NUMERICAL', required: true },
    { id: 11, title: 'Notes', serviceInfoItemType: 'STRING', required: false },
  ],
};

const CHURCHES = [
  { name: 'Trinity Church', location: 'San José', mainCelebrant: null },
  { name: 'St. Luke', location: 'Heredia', mainCelebrant: null },
];

const CELEBRANTS = [
  { id: 1, name: 'Fr. Smith' },
  { id: 2, name: 'Rev. Jones' },
];

function makeAuthValue(overrides: Partial<AuthContextValue> = {}): AuthContextValue {
  return {
    user: {
      id: 1,
      username: 'reporter',
      role: 'REPORTER',
      preferredLanguage: 'en',
      assignedChurchNames: ['Trinity Church'],
    },
    status: 'authenticated',
    authErrorKey: null,
    signIn: async () => {},
    reporterSignIn: async () => {},
    signOut: async () => {},
    refreshUser: async () => null,
    updatePreferredLanguage: async () => {},
    ...overrides,
  };
}

function renderPage(authOverrides: Partial<AuthContextValue> = {}, templateId = '1') {
  function Wrapper({ children }: Readonly<{ children: ReactNode }>) {
    return (
      <LocalizationProvider dateAdapter={AdapterDayjs}>
        <AuthContext.Provider value={makeAuthValue(authOverrides)}>
          <MemoryRouter initialEntries={[`/submit/service-templates/${templateId}`]}>
            <Routes>
              <Route
                path="/submit/service-templates/:templateId"
                element={children}
              />
            </Routes>
          </MemoryRouter>
        </AuthContext.Provider>
      </LocalizationProvider>
    );
  }
  return render(<ServiceSubmitPage />, { wrapper: Wrapper });
}

describe('ServiceSubmitPage', () => {
  beforeEach(async () => {
    mockedGetTemplate.mockReset();
    mockedGetCelebrants.mockReset();
    mockedGetChurches.mockReset();
    mockedSubmit.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows loading state while fetching data', () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue(CELEBRANTS);
    mockedGetChurches.mockResolvedValue(CHURCHES);

    renderPage();

    expect(screen.getByText('Loading form...')).toBeInTheDocument();
  });

  it('shows an error when data fails to load', async () => {
    mockedGetTemplate.mockRejectedValue(new Error('network error'));
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(
        'We could not load this service template.',
      );
    });
  });

  it('renders the form with template info items after loading', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue(CELEBRANTS);
    mockedGetChurches.mockResolvedValue(CHURCHES);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    expect(screen.getByLabelText(/attendance/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/notes/i)).toBeInTheDocument();
  });

  it('filters church list to reporter assigned churches only', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);

    renderPage({
      user: {
        id: 1,
        username: 'reporter',
        role: 'REPORTER',
        preferredLanguage: 'en',
        assignedChurchNames: ['Trinity Church'],
      },
    });

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    // The church select should be auto-selected since there's only one
    const churchCombobox = screen.getByRole('combobox', { name: 'Church' });
    expect(churchCombobox).toHaveTextContent('Trinity Church');
  });

  it('shows a warning when reporter has no assigned churches', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);

    renderPage({
      user: {
        id: 1,
        username: 'reporter',
        role: 'REPORTER',
        preferredLanguage: 'en',
        assignedChurchNames: [],
      },
    });

    await waitFor(() => {
      expect(
        screen.getByText('You have no churches assigned. Contact an administrator.'),
      ).toBeInTheDocument();
    });
  });

  it('admin can see all churches', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);

    renderPage({
      user: {
        id: 2,
        username: 'admin',
        role: 'ADMIN',
        preferredLanguage: 'en',
        assignedChurchNames: [],
      },
    });

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByRole('combobox', { name: 'Church' }));

    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'Trinity Church' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'St. Luke' })).toBeInTheDocument();
    });
  });

  it('submits successfully and shows the success state', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);
    mockedSubmit.mockResolvedValue({
      serviceInstanceId: 99,
      nextReporterLinkToken: 'next-token',
      nextReporterLinkFollowUpToken: null,
      nextReporterLinkActiveDate: '2026-04-15',
    });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    // Fill in the required attendance field
    await user.type(screen.getByLabelText(/attendance/i), '50');

    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(screen.getByText('Report Submitted')).toBeInTheDocument();
    });

    expect(
      screen.getByText('You still have a pending reporter link for 15 Apr 2026.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Open Next Pending Link' })).toHaveAttribute(
      'href',
      '/r/next-token',
    );

    expect(mockedSubmit).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        churchName: 'Trinity Church',
        responses: expect.arrayContaining([
          expect.objectContaining({ serviceInfoItemId: 10, responseValue: '50' }),
        ]),
      }),
    );

    // Optional empty responses should be filtered out
    const callArgs = mockedSubmit.mock.calls[0][1];
    expect(callArgs.responses).not.toContainEqual(
      expect.objectContaining({ serviceInfoItemId: 11 }),
    );
  });

  it('shows the up-to-date message when the reporter has no pending links left', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);
    mockedSubmit.mockResolvedValue({
      serviceInstanceId: 100,
      nextReporterLinkToken: null,
      nextReporterLinkFollowUpToken: null,
      nextReporterLinkActiveDate: null,
    });

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/attendance/i), '40');
    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(screen.getByText('You are up to date')).toBeInTheDocument();
    });

    expect(
      screen.getByText('All your pending reporter links have been completed.'),
    ).toBeInTheDocument();
  });

  it('shows an error when submission fails', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);
    mockedSubmit.mockRejectedValue(new Error('server error'));

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText(/attendance/i), '30');
    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(
        screen.getByText(
          'We could not submit the report. Please check all required fields and try again.',
        ),
      ).toBeInTheDocument();
    });
  });

  it('shows an error and does not submit when required fields are missing', async () => {
    mockedGetTemplate.mockResolvedValue(TEMPLATE);
    mockedGetCelebrants.mockResolvedValue([]);
    mockedGetChurches.mockResolvedValue(CHURCHES);

    const user = userEvent.setup();
    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    // Wait for the church to be auto-selected (enables the submit button)
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /submit report/i })).not.toBeDisabled();
    });

    // Do not fill attendance (required)
    await user.click(screen.getByRole('button', { name: /submit report/i }));

    await waitFor(() => {
      expect(
        screen.getByText(
          'We could not submit the report. Please check all required fields and try again.',
        ),
      ).toBeInTheDocument();
    });

    expect(mockedSubmit).not.toHaveBeenCalled();
  });
});
