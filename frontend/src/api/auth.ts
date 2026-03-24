import axios from 'axios';

export type UserRole = 'ADMIN' | 'REPORTER';

export interface AuthenticatedUser {
  id: number;
  username: string;
  role: UserRole;
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

export function isUnauthorizedError(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 401;
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
