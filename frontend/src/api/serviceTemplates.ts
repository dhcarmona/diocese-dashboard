import { api, getCsrfHeaders } from './auth';

export type ServiceTemplateType = 'RECURRING' | 'SPECIAL';
export type ServiceInfoItemType = 'NUMERICAL' | 'DOLLARS' | 'COLONES' | 'STRING';

export interface ServiceInfoItemSummary {
  id: number;
  title: string;
  description?: string | null;
  required: boolean;
  serviceInfoItemType: ServiceInfoItemType;
}

export interface ServiceTemplate {
  id: number;
  serviceTemplateName: string;
  templateType?: ServiceTemplateType;
  serviceInfoItems: ServiceInfoItemSummary[];
  bannerUrl?: string;
}

export interface ServiceTemplateSummary {
  id: number;
  serviceTemplateName: string;
  templateType?: ServiceTemplateType;
  bannerUrl?: string;
}

export interface ServiceTemplateDraft {
  serviceTemplateName: string;
}

export async function getServiceTemplates(): Promise<ServiceTemplate[]> {
  const response = await api.get<ServiceTemplate[]>('/api/service-templates');
  return response.data;
}

export async function getServiceTemplateById(id: number): Promise<ServiceTemplate> {
  const response = await api.get<ServiceTemplate>(`/api/service-templates/${id}`);
  return response.data;
}

export async function createServiceTemplate(draft: ServiceTemplateDraft): Promise<ServiceTemplate> {
  const response = await api.post<ServiceTemplate>('/api/service-templates', draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function updateServiceTemplate(
  id: number,
  draft: ServiceTemplateDraft,
): Promise<ServiceTemplate> {
  const response = await api.put<ServiceTemplate>(`/api/service-templates/${id}`, draft, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function deleteServiceTemplate(id: number): Promise<void> {
  await api.delete(`/api/service-templates/${id}`, {
    headers: await getCsrfHeaders(),
  });
}

