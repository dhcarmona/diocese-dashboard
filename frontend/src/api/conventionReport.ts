import { api } from './auth';
import type { ServiceTemplateSummary } from './serviceTemplates';

export interface ConventionReportColumn {
  itemId: number;
  itemTitle: string;
  itemType: 'NUMERICAL' | 'DOLLARS' | 'COLONES';
}

export interface ConventionReportRow {
  churchName: string;
  reportCount: number;
  values: number[];
}

export interface ConventionReportDuplicateNote {
  churchName: string;
  serviceDate: string; // ISO date: YYYY-MM-DD
  reportCount: number;
}

export interface ConventionReport {
  templateId: number;
  templateName: string;
  startDate: string;
  endDate: string;
  columns: ConventionReportColumn[];
  rows: ConventionReportRow[];
  duplicateNotes: ConventionReportDuplicateNote[];
}

export interface ConventionReportParams {
  templateId: number;
  startDate: string;
  endDate: string;
}

export async function getConventionReportTemplates(): Promise<ServiceTemplateSummary[]> {
  const response = await api.get<ServiceTemplateSummary[]>('/api/convention-report/templates');
  return response.data;
}

export async function getConventionReport(
  params: ConventionReportParams,
): Promise<ConventionReport> {
  const queryParams: Record<string, string> = {
    templateId: String(params.templateId),
    startDate: params.startDate,
    endDate: params.endDate,
  };
  const response = await api.get<ConventionReport>('/api/convention-report', {
    params: queryParams,
  });
  return response.data;
}
