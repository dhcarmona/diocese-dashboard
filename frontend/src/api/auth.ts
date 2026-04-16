import axios from 'axios';

export type UserRole = 'ADMIN' | 'REPORTER';
export type PreferredLanguage = 'en' | 'es';

export interface AuthenticatedUser {
  id: number;
  username: string;
  role: UserRole;
  preferredLanguage: PreferredLanguage;
  assignedChurchNames: string[];
}

interface CsrfTokenResponse {
  headerName: string;
  token: string;
}

const backendUnavailableStatuses = new Set([502, 503, 504]);
let csrfHeaderName: string | null = null;
let csrfToken: string | null = null;

export const api = axios.create({
  withCredentials: true,
});

export async function login(username: string, password: string): Promise<void> {
  const params = new URLSearchParams();
  params.append('username', username);
  params.append('password', password);
  await api.post('/api/auth/login', params, {
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
  });
}

export async function ensureCsrfToken(): Promise<void> {
  if (csrfHeaderName && csrfToken) {
    return;
  }
  const response = await api.get<CsrfTokenResponse>('/api/auth/csrf');
  csrfHeaderName = response.data.headerName;
  csrfToken = response.data.token;
}

export async function getCsrfHeaders(): Promise<Record<string, string>> {
  await ensureCsrfToken();
  return { [csrfHeaderName ?? 'X-CSRF-TOKEN']: csrfToken ?? '' };
}

export async function logout(): Promise<void> {
  try {
    await api.post('/api/auth/logout', undefined, {
      headers: await getCsrfHeaders(),
    });
  } finally {
    csrfHeaderName = null;
    csrfToken = null;
  }
}

export async function fetchAuthenticatedUser(): Promise<AuthenticatedUser | null> {
  try {
    const response = await api.get<AuthenticatedUser>('/api/auth/me');
    return response.data;
  } catch (error) {
    if (isUnauthorizedError(error)) {
      return null;
    }
    throw error;
  }
}

export async function requestReporterOtp(username: string): Promise<void> {
  await api.post('/api/auth/reporter/request-otp', { username });
}

export async function verifyReporterOtp(username: string, code: string): Promise<void> {
  await api.post('/api/auth/reporter/verify-otp', { username, code });
}

export async function requestReporterLoginLink(username: string, locale: string): Promise<void> {
  await api.post('/api/auth/reporter/request-login-link', { username, locale });
}

export async function redeemLoginToken(token: string): Promise<void> {
  await api.post('/api/auth/reporter/redeem-login-token', { token });
}

export async function updatePreferredLanguage(
  language: PreferredLanguage,
): Promise<AuthenticatedUser> {
  const response = await api.put<AuthenticatedUser>(
    '/api/auth/me/language',
    { language },
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}

export function isUnauthorizedError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 401;
}

export function isTooManyRequestsError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 429;
}

export function getRetryAfterSeconds(error: unknown): number | null {
  if (!axios.isAxiosError(error)) {
    return null;
  }
  const retryAfterHeader = error.response?.headers?.['retry-after'];
  if (typeof retryAfterHeader === 'string') {
    const retryAfterSeconds = Number.parseInt(retryAfterHeader, 10);
    if (!Number.isNaN(retryAfterSeconds) && retryAfterSeconds > 0) {
      return retryAfterSeconds;
    }
  }
  return null;
}

export function isBackendUnavailableError(error: unknown): boolean {
  if (!axios.isAxiosError(error)) {
    return false;
  }
  if (!error.response) {
    return true;
  }
  return backendUnavailableStatuses.has(error.response.status);
}
