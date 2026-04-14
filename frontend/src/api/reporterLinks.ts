import { api, getCsrfHeaders } from './auth';
import type { ReportSubmissionResponse } from './reportSubmissions';
import type { Celebrant } from './celebrants';
import type { ServiceInfoItemSummary } from './serviceTemplates';

export interface ReporterLink {
  id: number;
  token: string;
  reporterId: number;
  reporterUsername: string;
  reporterFullName: string | null;
  churchName: string;
  serviceTemplateId: number;
  serviceTemplateName: string;
  activeDate: string; // ISO date string: YYYY-MM-DD
}

export interface ReporterLinkWithTemplate extends ReporterLink {
  serviceInfoItems?: ServiceInfoItemSummary[];
}

/** Full link data returned by the public (unauthenticated) endpoint. */
export interface ReporterLinkPublicData {
  id: number;
  token: string;
  churchName: string;
  serviceTemplateId: number;
  serviceTemplateName: string;
  activeDate: string;
  serviceInfoItems: ServiceInfoItemSummary[];
  celebrants: Celebrant[];
}

export interface ReporterLinkBulkResult {
  created: ReporterLink[];
  skippedChurches: string[];
}

export interface ReporterLinkBulkPayload {
  serviceTemplateId: number;
  activeDate: string;
  churchNames: string[];
}

export interface ReporterLinkSubmitPayload {
  celebrantIds: number[];
  serviceDate: string;
  responses: Array<{ serviceInfoItemId: number; responseValue: string }>;
}

export async function getReporterLinks(): Promise<ReporterLink[]> {
  const response = await api.get<ReporterLink[]>('/api/reporter-links');
  return response.data;
}

export async function createReporterLinksBulk(
  payload: ReporterLinkBulkPayload,
): Promise<ReporterLinkBulkResult> {
  const response = await api.post<ReporterLinkBulkResult>('/api/reporter-links/bulk', payload, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function getReporterLinkByToken(token: string): Promise<ReporterLink> {
  const response = await api.get<ReporterLink>(`/api/reporter-links/${encodeURIComponent(token)}`);
  return response.data;
}

/**
 * Fetches all data needed to render the reporter link form without authentication.
 * The token itself serves as the authorization credential.
 */
export async function getReporterLinkPublic(token: string): Promise<ReporterLinkPublicData> {
  const response = await api.get<ReporterLinkPublicData>(
    `/api/reporter-links/public/${encodeURIComponent(token)}`,
  );
  return response.data;
}

export async function revokeReporterLink(token: string): Promise<void> {
  await api.delete(`/api/reporter-links/${encodeURIComponent(token)}`, {
    headers: await getCsrfHeaders(),
  });
}

export async function submitViaReporterLink(
  token: string,
  payload: ReporterLinkSubmitPayload,
): Promise<ReportSubmissionResponse> {
  const response = await api.post<ReportSubmissionResponse>(
    `/api/reporter-links/${encodeURIComponent(token)}/submit`,
    payload,
    { headers: await getCsrfHeaders() },
  );
  return response.data;
}

/**
 * Submits a report via the public (unauthenticated) reporter link endpoint.
 * No session or CSRF token is required.
 */
export async function submitViaReporterLinkPublic(
  token: string,
  payload: ReporterLinkSubmitPayload,
): Promise<ReportSubmissionResponse> {
  const response = await api.post<ReportSubmissionResponse>(
    `/api/reporter-links/public/${encodeURIComponent(token)}/submit`,
    payload,
  );
  return response.data;
}
