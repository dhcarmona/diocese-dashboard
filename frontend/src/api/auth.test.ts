import { afterEach, describe, expect, it, vi } from 'vitest';
import { api, ensureCsrfToken, logout } from './auth';

describe('auth API helpers', () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('fetches the CSRF token before logout and sends it in the configured header', async () => {
    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({
      data: {
        headerName: 'X-CSRF-TOKEN',
        token: 'csrf-123',
      },
    });
    const postSpy = vi.spyOn(api, 'post').mockResolvedValueOnce({ data: undefined });

    await logout();

    expect(getSpy).toHaveBeenCalledWith('/api/auth/csrf');
    expect(postSpy).toHaveBeenCalledWith('/api/auth/logout', undefined, {
      headers: { 'X-CSRF-TOKEN': 'csrf-123' },
    });
  });

  it('reuses the cached CSRF token for repeated protected calls', async () => {
    const getSpy = vi.spyOn(api, 'get').mockResolvedValueOnce({
      data: {
        headerName: 'X-CSRF-TOKEN',
        token: 'csrf-123',
      },
    });

    await ensureCsrfToken();
    await ensureCsrfToken();

    expect(getSpy).toHaveBeenCalledTimes(1);
  });
});
