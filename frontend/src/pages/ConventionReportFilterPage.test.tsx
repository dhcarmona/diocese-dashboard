import { render, screen, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import dayjs from 'dayjs';
import i18n from 'i18next';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { vi } from 'vitest';
import ConventionReportFilterPage from './ConventionReportFilterPage';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importOriginal) => {
  const actual = await importOriginal<typeof import('react-router-dom')>();
  return { ...actual, useNavigate: () => mockNavigate };
});

vi.mock('@mui/x-date-pickers/DatePicker', () => ({
  DatePicker: ({ label, onChange }: { label: string; onChange?: (v: unknown) => void }) => (
    <input
      aria-label={label}
      onChange={(e) => onChange?.(e.target.value ? dayjs(e.target.value) : null)}
    />
  ),
}));

function renderPage() {
  return render(
    <MemoryRouter initialEntries={['/convention-report/1']}>
      <Routes>
        <Route path="/convention-report/:templateId" element={<ConventionReportFilterPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('ConventionReportFilterPage', () => {
  beforeEach(async () => {
    mockNavigate.mockReset();
    await i18n.changeLanguage('en');
  });

  it('shows a Generate Report button', () => {
    renderPage();

    expect(screen.getByRole('button', { name: /generate report/i })).toBeInTheDocument();
  });

  it('does not render a church selector', () => {
    renderPage();

    expect(screen.queryByRole('combobox', { name: /church/i })).not.toBeInTheDocument();
  });

  it('generate navigates to the report route with start/end dates', async () => {
    renderPage();

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: /generate report/i }));

    expect(mockNavigate).toHaveBeenCalledWith(
      expect.stringMatching(/^\/convention-report\/1\/report\?startDate=.+&endDate=.+$/),
    );
  });

  it('shows date validation error when end date is before start date', async () => {
    renderPage();

    const user = userEvent.setup();
    fireEvent.change(screen.getByLabelText('Start Date'), { target: { value: '2024-12-01' } });
    fireEvent.change(screen.getByLabelText('End Date'), { target: { value: '2024-01-01' } });
    await user.click(screen.getByRole('button', { name: /generate report/i }));

    expect(screen.getByText('End date must be after start date.')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });
});
