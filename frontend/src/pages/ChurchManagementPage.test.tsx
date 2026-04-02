import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import i18n from 'i18next';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, vi } from 'vitest';
import {
  createChurch,
  deleteChurch,
  getChurches,
  updateChurch,
} from '../api/churches';
import ChurchManagementPage from './ChurchManagementPage';

vi.mock('../api/churches', () => ({
  getChurches: vi.fn(),
  createChurch: vi.fn(),
  updateChurch: vi.fn(),
  deleteChurch: vi.fn(),
}));

describe('ChurchManagementPage', () => {
  const mockedGetChurches = vi.mocked(getChurches);
  const mockedCreateChurch = vi.mocked(createChurch);
  const mockedUpdateChurch = vi.mocked(updateChurch);
  const mockedDeleteChurch = vi.mocked(deleteChurch);

  beforeEach(async () => {
    mockedGetChurches.mockReset();
    mockedCreateChurch.mockReset();
    mockedUpdateChurch.mockReset();
    mockedDeleteChurch.mockReset();
    await i18n.changeLanguage('en');
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('renders the church directory after loading', async () => {
    mockedGetChurches.mockResolvedValueOnce([
      {
        name: 'Iglesia San Pablo',
        location: 'Heredia',
        mainCelebrant: null,
        portraitUrl: '/api/portraits/churches?name=Iglesia+San+Pablo',
      },
      {
        name: 'Cathedral',
        location: 'San Jose',
        mainCelebrant: null,
        portraitUrl: '/api/portraits/churches?name=Cathedral',
      },
    ]);

    render(
      <MemoryRouter>
        <ChurchManagementPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('Loading churches...')).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /cathedral/i })).toBeInTheDocument();
    });

    expect(screen.getByText('Location: San Jose')).toBeInTheDocument();
    expect(screen.getByText('Total churches: 2')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /iglesia san pablo/i })).toBeInTheDocument();
    expect(document.querySelector('img[src="/api/portraits/churches?name=Cathedral"]')).not.toBeNull();
  });

  it('creates a church and returns the form to create mode', async () => {
    const user = userEvent.setup();
    mockedGetChurches.mockResolvedValueOnce([]);
    mockedCreateChurch.mockResolvedValueOnce({
      name: 'St. Luke',
      location: 'Cartago',
      mainCelebrant: null,
      portraitUrl: '/api/portraits/churches?name=St.+Luke',
    });

    render(
      <MemoryRouter>
        <ChurchManagementPage />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('No churches have been added yet.')).toBeInTheDocument();
    });

    await user.type(screen.getByLabelText('Church name'), 'St. Luke');
    await user.type(screen.getByLabelText('Location'), 'Cartago');
    await user.click(screen.getByRole('button', { name: /create church/i }));

    await waitFor(() => {
      expect(mockedCreateChurch).toHaveBeenCalledWith({
        name: 'St. Luke',
        location: 'Cartago',
        mainCelebrant: null,
      });
    });

    expect(await screen.findByText('St. Luke was created.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /st. luke/i })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Add Church' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /create church/i })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /save changes/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /delete church/i })).not.toBeInTheDocument();
    expect(screen.getByLabelText('Church name')).toHaveValue('');
    expect(screen.getByLabelText('Location')).toHaveValue('');
  });

  it('lets users dismiss feedback with the close button', async () => {
    const user = userEvent.setup();
    mockedGetChurches.mockResolvedValue([{
      name: 'Trinity',
      location: 'Limon',
      mainCelebrant: null,
      portraitUrl: '/api/portraits/churches?name=Trinity',
    }]);
    mockedUpdateChurch.mockResolvedValueOnce({
      name: 'Trinity',
      location: 'Limon Centro',
      mainCelebrant: null,
      portraitUrl: '/api/portraits/churches?name=Trinity',
    });

    render(
      <MemoryRouter>
        <ChurchManagementPage />
      </MemoryRouter>,
    );

    const churchButton = await screen.findByRole('button', { name: /trinity/i });
    await user.click(churchButton);

    const locationField = screen.getByLabelText('Location');
    await user.clear(locationField);
    await user.type(locationField, 'Limon Centro');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    expect(await screen.findByText('Trinity was updated.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /close/i }));
    expect(screen.queryByText('Trinity was updated.')).not.toBeInTheDocument();
  });

  it('auto-dismisses feedback after five seconds', async () => {
    vi.useFakeTimers();
    mockedGetChurches.mockResolvedValue([]);

    render(
      <MemoryRouter>
        <ChurchManagementPage />
      </MemoryRouter>,
    );

    fireEvent.click(screen.getByRole('button', { name: /create church/i }));

    expect(screen.getByText('Enter a church name before saving.')).toBeInTheDocument();

    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000);
    });

    expect(screen.queryByText('Enter a church name before saving.')).not.toBeInTheDocument();
  });

  it('updates and deletes an existing church', async () => {
    const user = userEvent.setup();
    mockedGetChurches.mockResolvedValueOnce([
      {
        name: 'Trinity',
        location: 'Limon',
        mainCelebrant: null,
        portraitUrl: '/api/portraits/churches?name=Trinity',
      },
    ]);
    mockedUpdateChurch.mockResolvedValueOnce({
      name: 'Trinity',
      location: 'Puerto Limon',
      mainCelebrant: null,
      portraitUrl: '/api/portraits/churches?name=Trinity',
    });
    mockedDeleteChurch.mockResolvedValueOnce();

    render(
      <MemoryRouter>
        <ChurchManagementPage />
      </MemoryRouter>,
    );

    const churchButton = await screen.findByRole('button', { name: /trinity/i });
    await user.click(churchButton);
    expect(screen.getByTestId('church-form-portrait')).toBeInTheDocument();

    const nameField = screen.getByLabelText('Church name');
    expect(nameField).toBeDisabled();

    const locationField = screen.getByLabelText('Location');
    await user.clear(locationField);
    await user.type(locationField, 'Puerto Limon');
    await user.click(screen.getByRole('button', { name: /save changes/i }));

    await waitFor(() => {
      expect(mockedUpdateChurch).toHaveBeenCalledWith('Trinity', {
        name: 'Trinity',
        location: 'Puerto Limon',
        mainCelebrant: null,
      });
    });

    expect(await screen.findByText('Trinity was updated.')).toBeInTheDocument();
    expect(screen.getByText('Location: Puerto Limon')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /delete church/i }));

    await waitFor(() => {
      expect(mockedDeleteChurch).toHaveBeenCalledWith('Trinity');
    });

    expect(await screen.findByText('Trinity was removed.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /trinity/i })).not.toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Add Church' })).toBeInTheDocument();
  });
});
