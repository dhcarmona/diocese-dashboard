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
