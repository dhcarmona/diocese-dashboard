import { api, getCsrfHeaders } from './auth';

export interface ChurchCelebrantSummary {
  id: number;
  name: string;
  portraitDataUrl?: string;
}

export interface Church {
  name: string;
  location: string | null;
  mainCelebrant?: ChurchCelebrantSummary | null;
  portraitDataUrl?: string;
}

export interface ChurchDraft {
  name: string;
  location: string;
  mainCelebrant?: ChurchCelebrantSummary | null;
}

export async function getChurches(): Promise<Church[]> {
  const response = await api.get<Church[]>('/api/churches');
  return response.data;
}

export async function createChurch(draft: ChurchDraft): Promise<Church> {
  const response = await api.post<Church>('/api/churches', draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function updateChurch(name: string, draft: ChurchDraft): Promise<Church> {
  const response = await api.put<Church>(`/api/churches/${encodeURIComponent(name)}`, draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function deleteChurch(name: string): Promise<void> {
  await api.delete(`/api/churches/${encodeURIComponent(name)}`, {
    headers: await getCsrfHeaders(),
  });
}
