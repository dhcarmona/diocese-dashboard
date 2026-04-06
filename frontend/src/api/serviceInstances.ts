import { api, getCsrfHeaders } from './auth';

export interface ServiceInstanceSubmitRequest {
  churchName: string;
  celebrantIds: number[];
  serviceDate: string; // ISO date string: YYYY-MM-DD
  responses: ResponseEntry[];
}

export interface ResponseEntry {
  serviceInfoItemId: number;
  responseValue: string;
}

export interface ServiceInstanceSummary {
  id: number;
  serviceDate: string;
  churchName: string;
  templateId: number;
  templateName: string;
  submittedByUsername: string | null;
  submittedByFullName: string | null;
}

export interface ServiceInfoItemType {
  serviceInfoItemType: 'NUMERICAL' | 'DOLLARS' | 'COLONES' | 'STRING';
}

export interface ResponseDetail {
  responseId: number;
  serviceInfoItemId: number;
  serviceInfoItemTitle: string;
  serviceInfoItemDescription: string | null;
  serviceInfoItemType: 'NUMERICAL' | 'DOLLARS' | 'COLONES' | 'STRING';
  required: boolean | null;
  responseValue: string;
}

export interface ServiceInstanceDetail {
  id: number;
  serviceDate: string;
  churchName: string;
  templateId: number;
  templateName: string;
  submittedByUsername: string | null;
  submittedByFullName: string | null;
  responses: ResponseDetail[];
}

export async function submitServiceInstance(
  templateId: number,
  request: ServiceInstanceSubmitRequest,
): Promise<void> {
  await api.post<unknown>(
    `/api/service-templates/${templateId}/submit`,
    request,
    { headers: await getCsrfHeaders() },
  );
}

export async function getInstancesByTemplate(
  templateId: number,
): Promise<ServiceInstanceSummary[]> {
  const response = await api.get<ServiceInstanceSummary[]>(
    '/api/service-instances',
    { params: { templateId } },
  );
  return response.data;
}

export async function getInstanceDetail(id: number): Promise<ServiceInstanceDetail> {
  const response = await api.get<ServiceInstanceDetail>(`/api/service-instances/${id}`);
  return response.data;
}

export async function updateInstance(
  id: number,
  responses: ResponseEntry[],
  notifyReporter: boolean,
): Promise<ServiceInstanceDetail> {
  const response = await api.put<ServiceInstanceDetail>(
    `/api/service-instances/${id}`,
    { responses, notifyReporter },
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}

export async function deleteInstance(id: number, notify: boolean): Promise<void> {
  await api.delete(`/api/service-instances/${id}`, {
    params: { notify },
    headers: await getCsrfHeaders(),
  });
}
