import { api, getCsrfHeaders } from './auth';

export interface SectionHeader {
  id: number;
  title: string;
  sortOrder?: number;
}

export interface SectionHeaderDraft {
  title: string;
}

export async function createSectionHeader(
  templateId: number,
  draft: SectionHeaderDraft,
): Promise<SectionHeader> {
  const response = await api.post<SectionHeader>(
    `/api/section-headers?templateId=${templateId}`,
    draft,
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}

export async function updateSectionHeader(
  id: number,
  draft: SectionHeaderDraft,
): Promise<SectionHeader> {
  const response = await api.put<SectionHeader>(`/api/section-headers/${id}`, draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function deleteSectionHeader(id: number): Promise<void> {
  await api.delete(`/api/section-headers/${id}`, {
    headers: await getCsrfHeaders(),
  });
}
