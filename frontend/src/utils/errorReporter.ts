import { api, getCsrfHeaders } from '../api/auth';

/**
 * Fire-and-forget: reports a client-side error to the backend.
 * Fetches a CSRF token first (creating a session if needed), then POSTs.
 * Never throws — errors during reporting are silently swallowed.
 */
export async function reportClientError(params: {
  message: string;
  stack?: string | null;
  url?: string;
  userAgent?: string;
}): Promise<void> {
  try {
    const headers = await getCsrfHeaders();
    await api.post(
      '/api/client-errors',
      {
        message: params.message.slice(0, 500),
        stack: params.stack?.slice(0, 5000) ?? null,
        url: (params.url ?? window.location.href).slice(0, 500),
        userAgent: (params.userAgent ?? navigator.userAgent).slice(0, 500),
      },
      { headers },
    );
  } catch {
    // Intentionally silent: error reporting must never crash the app
  }
}
