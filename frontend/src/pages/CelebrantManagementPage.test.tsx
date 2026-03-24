import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, vi } from 'vitest';
import {
  createCelebrant,
  deleteCelebrant,
  getCelebrants,
  updateCelebrant,
} from '../api/celebrants';
import CelebrantManagementPage from './CelebrantManagementPage';

vi.mock('../api/celebrants', () => ({
  getCelebrants: vi.fn(),
  createCelebrant: vi.fn(),
  updateCelebrant: vi.fn(),
  deleteCelebrant: vi.fn(),
}));

describe('CelebrantManagementPage', () => {
  const mockedGetCelebrants = vi.mocked(getCelebrants);
  const mockedCreateCelebrant = vi.mocked(createCelebrant);
  const mockedUpdateCelebrant = vi.mocked(updateCelebrant);
  const mockedDeleteCelebrant = vi.mocked(deleteCelebrant);

  beforeEach(async () => {
    mockedGetCelebrants.mockReset();
    mockedCreateCelebrant.mockReset();
    mockedUpdateCelebrant.mockReset();
    mockedDeleteCelebrant.mockReset();
    await i18n.changeLanguage('en');
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the celebrant directory after loading', async () => {
    mockedGetCelebrants.mockResolvedValueOnce([
      { id: 2, name: 'Ana Perez' },
      { id: 1, name: 'Bishop Mora' },
    ]);

    render(
      <MemoryRouter>
        <CelebrantManagementPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading celebrants...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /ana perez/i })).toBeInTheDocument();
    });

    expect(screen.getByText('Total celebrants: 2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /bishop mora/i })).toBeInTheDocument();
  });

  it('creates a celebrant and returns the form to create mode', async () => {
    const user = userEvent.setup();
    mockedGetCelebrants.mockResolvedValueOnce([]);
    mockedCreateCelebrant.mockResolvedValueOnce({ id: 14, name: 'Rev. Solis' });

    render(
      <MemoryRouter>
        <CelebrantManagementPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('No celebrants have been added yet.')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Celebrant name'), 'Rev. Solis');
    await user.click(screen.getByRole('button', { name: /create celebrant/i }));

    await waitFor(() => {
      expect(mockedCreateCelebrant).toHaveBeenCalledWith({ name: 'Rev. Solis' });
    });

    expect(await screen.findByText('Rev. Solis was created.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /rev. solis/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Add Celebrant' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create celebrant/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete celebrant/i })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Celebrant name')).toHaveValue('');
  });

  it('lets users dismiss feedback with the close button', async () => {
    const user = userEvent.setup();
    mockedGetCelebrants.mockResolvedValue([{ id: 7, name: 'Canon Vega' }]);
    mockedUpdateCelebrant.mockResolvedValueOnce({ id: 7, name: 'Canon Vega-Soto' });

    render(
      <MemoryRouter>
        <CelebrantManagementPage />
      </MemoryRouter>,
    );

    const celebrantButton = await screen.findByRole('button', { name: /canon vega/i });
    await user.click(celebrantButton);

    const nameField = screen.getByLabelText('Celebrant name');
    await user.clear(nameField);
    await user.type(nameField, 'Canon Vega-Soto');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByText('Canon Vega-Soto was updated.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /close/i }));
    expect(screen.queryByText('Canon Vega-Soto was updated.')).not.toBeInTheDocument();
  });

  it('auto-dismisses feedback after five seconds', async () => {
    vi.useFakeTimers();
    mockedGetCelebrants.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <CelebrantManagementPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: /create celebrant/i }));

    expect(screen.getByText('Enter a celebrant name before saving.')).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });

    expect(screen.queryByText('Enter a celebrant name before saving.')).not.toBeInTheDocument();
  });

  it('updates and deletes an existing celebrant', async () => {
    const user = userEvent.setup();
    mockedGetCelebrants.mockResolvedValueOnce([{ id: 7, name: 'Canon Vega' }]);
    mockedUpdateCelebrant.mockResolvedValueOnce({ id: 7, name: 'Canon Vega-Soto' });
    mockedDeleteCelebrant.mockResolvedValueOnce();

    render(
      <MemoryRouter>
        <CelebrantManagementPage />
      </MemoryRouter>,
    );

    const celebrantButton = await screen.findByRole('button', { name: /canon vega/i });
    await user.click(celebrantButton);

    const nameField = screen.getByLabelText('Celebrant name');
    await user.clear(nameField);
    await user.type(nameField, 'Canon Vega-Soto');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(mockedUpdateCelebrant).toHaveBeenCalledWith(7, { name: 'Canon Vega-Soto' });
    });

    expect(await screen.findByText('Canon Vega-Soto was updated.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /delete celebrant/i }));

    await waitFor(() => {
      expect(mockedDeleteCelebrant).toHaveBeenCalledWith(7);
    });

    expect(await screen.findByText('Canon Vega-Soto was removed.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /canon vega-soto/i })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Add Celebrant' })).toBeInTheDocument();
  });
});
