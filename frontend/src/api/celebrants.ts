import { api, getCsrfHeaders } from './auth';

export interface Celebrant {
  id: number;
  name: string;
  portraitDataUrl?: string;
}

export interface CelebrantDraft {
  name: string;
}

export async function getCelebrants(): Promise<Celebrant[]> {
  const response = await api.get<Celebrant[]>('/api/celebrants');
  return response.data;
}

export async function createCelebrant(draft: CelebrantDraft): Promise<Celebrant> {
  const response = await api.post<Celebrant>('/api/celebrants', draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function updateCelebrant(id: number, draft: CelebrantDraft): Promise<Celebrant> {
  const response = await api.put<Celebrant>(`/api/celebrants/${id}`, draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function deleteCelebrant(id: number): Promise<void> {
  await api.delete(`/api/celebrants/${id}`, {
    headers: await getCsrfHeaders(),
  });
}
