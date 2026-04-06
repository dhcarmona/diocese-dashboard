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

export interface ServiceInstanceSubmitResult {
  id: number;
}

export async function submitServiceInstance(
  templateId: number,
  request: ServiceInstanceSubmitRequest,
): Promise<ServiceInstanceSubmitResult> {
  const response = await api.post<ServiceInstanceSubmitResult>(
    `/api/service-templates/${templateId}/submit`,
    request,
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}
