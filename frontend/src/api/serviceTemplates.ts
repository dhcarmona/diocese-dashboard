import { api } from './auth';

export interface ServiceTemplateSummary {
  id: number;
  serviceTemplateName: string;
}

export async function getServiceTemplates(): Promise<ServiceTemplateSummary[]> {
  const response = await api.get<ServiceTemplateSummary[]>('/api/service-templates');
  return response.data;
}
