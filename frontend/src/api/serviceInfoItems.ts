import { api, getCsrfHeaders } from './auth';
import type { ServiceInfoItemType } from './serviceTemplates';

export interface ServiceInfoItem {
  id: number;
  title: string;
  description?: string | null;
  required: boolean;
  serviceInfoItemType: ServiceInfoItemType;
}

export interface ServiceInfoItemDraft {
  title: string;
  description?: string | null;
  required: boolean;
  serviceInfoItemType: ServiceInfoItemType;
}

export async function createServiceInfoItem(
  templateId: number,
  draft: ServiceInfoItemDraft,
): Promise<ServiceInfoItem> {
  const response = await api.post<ServiceInfoItem>(
    `/api/service-info-items?templateId=${templateId}`,
    draft,
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}

export async function updateServiceInfoItem(
  id: number,
  templateId: number,
  draft: ServiceInfoItemDraft,
): Promise<ServiceInfoItem> {
  const response = await api.put<ServiceInfoItem>(
    `/api/service-info-items/${id}?templateId=${templateId}`,
    draft,
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}

export async function deleteServiceInfoItem(id: number): Promise<void> {
  await api.delete(`/api/service-info-items/${id}`, {
    headers: await getCsrfHeaders(),
  });
}

export async function reorderServiceInfoItems(orderedIds: number[]): Promise<void> {
  await api.put(
    '/api/service-info-items/reorder',
    { orderedIds },
    { headers: await getCsrfHeaders() },
  );
}
