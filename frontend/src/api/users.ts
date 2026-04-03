import { api, getCsrfHeaders } from './auth';

export interface ReporterUser {
  id: number;
  username: string;
  fullName: string;
  phoneNumber: string;
  role: 'REPORTER';
  enabled: boolean;
  assignedChurches: { name: string }[];
}

export interface CreateReporterUserRequest {
  username: string;
  fullName: string;
  phoneNumber: string;
  churchNames: string[];
}

export interface UpdateReporterUserRequest {
  username: string;
  fullName: string;
  phoneNumber: string;
  churchNames: string[];
}

export async function getReporterUsers(): Promise<ReporterUser[]> {
  const response = await api.get<ReporterUser[]>('/api/users');
  return response.data.filter((u) => u.role === 'REPORTER');
}

export async function createReporterUser(
  request: CreateReporterUserRequest,
): Promise<ReporterUser> {
  const headers = await getCsrfHeaders();
  const response = await api.post<ReporterUser>(
    '/api/users',
    { ...request, role: 'REPORTER', password: null },
    { headers },
  );
  return response.data;
}

export async function updateReporterUser(
  id: number,
  request: UpdateReporterUserRequest,
): Promise<ReporterUser> {
  const headers = await getCsrfHeaders();
  const response = await api.put<ReporterUser>(
    `/api/users/${id}`,
    { ...request, role: 'REPORTER', password: null },
    { headers },
  );
  return response.data;
}

export async function deleteReporterUser(id: number): Promise<void> {
  const headers = await getCsrfHeaders();
  await api.delete(`/api/users/${id}`, { headers });
}
