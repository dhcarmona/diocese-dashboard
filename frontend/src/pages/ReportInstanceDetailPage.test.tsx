import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import { getCelebrants } from '../api/celebrants';
import {
  type ServiceInstanceDetail,
  deleteInstance,
  getInstanceDetail,
  updateInstance,
} from '../api/serviceInstances';
import ReportInstanceDetailPage from './ReportInstanceDetailPage';

vi.mock('../api/serviceInstances', () => ({
  getInstanceDetail: vi.fn(),
  updateInstance: vi.fn(),
  deleteInstance: vi.fn(),
}));

vi.mock('../api/celebrants', () => ({
  getCelebrants: vi.fn(),
}));

function renderPage(templateId = '10', instanceId = '1') {
  render(
    <MemoryRouter
      initialEntries={[`/reports/view/individual/${templateId}/${instanceId}`]}
    >
      <Routes>
        <Route
          path="/reports/view/individual/:templateId/:instanceId"
          element={<ReportInstanceDetailPage />}
        />
        <Route
          path="/reports/view/individual/:templateId"
          element={<div>Reports List Page</div>}
        />
      </Routes>
    </MemoryRouter>,
  );
}

const DETAIL: ServiceInstanceDetail = {
  id: 1,
  serviceDate: '2026-01-15',
  churchName: 'Trinity Church',
  templateId: 10,
  templateName: 'Sunday Eucharist',
  submittedByUsername: 'jsmith',
  submittedByFullName: 'Jonathan Smith',
  celebrants: [{ id: 2, name: 'Fr. John' }],
  responses: [
    {
      responseId: 100,
      serviceInfoItemId: 5,
      serviceInfoItemTitle: 'Attendance',
      serviceInfoItemDescription: 'Number of people present',
      serviceInfoItemType: 'NUMERICAL',
      required: true,
      responseValue: '42',
    },
  ],
};

describe('ReportInstanceDetailPage', () => {
  const mockedGetInstanceDetail = vi.mocked(getInstanceDetail);
  const mockedUpdateInstance = vi.mocked(updateInstance);
  const mockedDeleteInstance = vi.mocked(deleteInstance);
  const mockedGetCelebrants = vi.mocked(getCelebrants);

  beforeEach(async () => {
    mockedGetInstanceDetail.mockReset();
    mockedUpdateInstance.mockReset();
    mockedDeleteInstance.mockReset();
    mockedGetCelebrants.mockReset();
    mockedGetCelebrants.mockResolvedValue([
      { id: 1, name: 'Fr. Alice' },
      { id: 2, name: 'Fr. John' },
    ]);
    await i18n.changeLanguage('en');
  });

  it('shows a loading indicator initially', () => {
    mockedGetInstanceDetail.mockReturnValue(new Promise(() => {}));

    renderPage();

    expect(screen.getByText('Loading report...')).toBeInTheDocument();
  });

  it('shows an error when loading fails', async () => {
    mockedGetInstanceDetail.mockRejectedValueOnce(new Error('network error'));

    renderPage();

    await waitFor(() => {
      expect(
        screen.getAllByText('We could not load this report.').length,
      ).toBeGreaterThan(0);
    });
  });

  it('pre-selects celebrants that are already on the instance', async () => {
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);

    renderPage();

    await screen.findByText('Sunday Eucharist');

    // DETAIL has celebrant id=2 "Fr. John"; the chip should appear in the Autocomplete
    expect(screen.getByRole('button', { name: /fr\. john/i })).toBeInTheDocument();
    // Fr. Alice (id=1) is not on the instance and should not appear as a chip
    expect(screen.queryByRole('button', { name: /fr\. alice/i })).not.toBeInTheDocument();
  });

  it('passes the selected celebrant IDs to updateInstance on save', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedUpdateInstance.mockResolvedValueOnce({ ...DETAIL });

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /save changes/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(mockedUpdateInstance).toHaveBeenCalledWith(
        1, expect.any(Array), false, [2],
      );
    });
  });

  it('updates celebrant IDs when the selection is changed before saving', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedUpdateInstance.mockResolvedValueOnce({ ...DETAIL });

    renderPage();

    await screen.findByText('Sunday Eucharist');

    // Remove Fr. John by clicking the delete (cancel) icon inside his chip
    const frJohnChip = screen.getByRole('button', { name: /fr\. john/i });
    await user.click(within(frJohnChip).getByTestId('CancelIcon'));

    await user.click(screen.getByRole('button', { name: /save changes/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(mockedUpdateInstance).toHaveBeenCalledWith(
        1, expect.any(Array), false, [],
      );
    });
  });


  it('renders report metadata and responses after loading', async () => {
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);

    renderPage();

    await waitFor(() => {
      expect(screen.getByText('Sunday Eucharist')).toBeInTheDocument();
    });

    expect(screen.getByText(/Trinity Church/)).toBeInTheDocument();
    expect(screen.getByText(/Jonathan Smith/)).toBeInTheDocument();
    expect(screen.getByText(/2026-01-15/)).toBeInTheDocument();
    expect(screen.getByLabelText(/attendance/i)).toHaveValue(42);
  });

  it('saves with notification when user chooses Yes in the notify dialog', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedUpdateInstance.mockResolvedValueOnce({ ...DETAIL });

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByRole('heading', { name: /notify reporter/i })).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /yes, notify/i }));

    await waitFor(() => {
      expect(mockedUpdateInstance).toHaveBeenCalledWith(
        1, expect.any(Array), true, expect.any(Array),
      );
    });

    expect(await screen.findByText('Changes were saved successfully.')).toBeInTheDocument();
  });

  it('saves without notification when user chooses No in the notify dialog', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedUpdateInstance.mockResolvedValueOnce({ ...DETAIL });

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /save changes/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(mockedUpdateInstance).toHaveBeenCalledWith(
        1, expect.any(Array), false, expect.any(Array),
      );
    });
  });

  it('shows a save error when updateInstance fails', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedUpdateInstance.mockRejectedValueOnce(new Error('server error'));

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /save changes/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(
        screen.getByText('We could not save the changes. Please try again.'),
      ).toBeInTheDocument();
    });
  });

  it('cancelling the delete confirm dialog does not call deleteInstance', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /delete report/i }));

    expect(
      await screen.findByText(
        'Are you sure you want to delete this report? This action cannot be undone.',
      ),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /^cancel$/i }));

    expect(mockedDeleteInstance).not.toHaveBeenCalled();
  });

  it('deletes with notification when user confirms and chooses Yes', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedDeleteInstance.mockResolvedValueOnce();

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /delete report/i }));
    await screen.findByRole('heading', { name: /^delete report$/i });
    await user.click(screen.getByRole('button', { name: /^delete$/i }));

    expect(
      await screen.findByRole('heading', { name: /notify reporter/i }),
    ).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /yes, notify/i }));

    await waitFor(() => {
      expect(mockedDeleteInstance).toHaveBeenCalledWith(1, true);
    });
  });

  it('deletes without notification when user confirms and chooses No', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedDeleteInstance.mockResolvedValueOnce();

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /delete report/i }));
    await screen.findByRole('heading', { name: /^delete report$/i });
    await user.click(screen.getByRole('button', { name: /^delete$/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(mockedDeleteInstance).toHaveBeenCalledWith(1, false);
    });
  });

  it('navigates back to the list after successful deletion', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedDeleteInstance.mockResolvedValueOnce();

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /delete report/i }));
    await screen.findByRole('heading', { name: /^delete report$/i });
    await user.click(screen.getByRole('button', { name: /^delete$/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(screen.getByText('Reports List Page')).toBeInTheDocument();
    });
  });

  it('shows a delete error when deleteInstance fails', async () => {
    const user = userEvent.setup();
    mockedGetInstanceDetail.mockResolvedValueOnce(DETAIL);
    mockedDeleteInstance.mockRejectedValueOnce(new Error('server error'));

    renderPage();

    await screen.findByText('Sunday Eucharist');

    await user.click(screen.getByRole('button', { name: /delete report/i }));
    await screen.findByRole('heading', { name: /^delete report$/i });
    await user.click(screen.getByRole('button', { name: /^delete$/i }));
    await screen.findByRole('heading', { name: /notify reporter/i });
    await user.click(screen.getByRole('button', { name: /no, skip/i }));

    await waitFor(() => {
      expect(
        screen.getByText('We could not delete the report. Please try again.'),
      ).toBeInTheDocument();
    });
  });
});
