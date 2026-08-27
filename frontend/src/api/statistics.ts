import { api } from './auth';
import type { ServiceTemplateSummary } from './serviceTemplates';

export interface CelebrantStat {
  celebrantId: number;
  celebrantName: string;
  serviceCount: number;
}

export interface TimeSeriesPoint {
  date: string; // ISO date: YYYY-MM-DD
  value: number;
}

export interface AggregatedItem {
  itemId: number;
  itemTitle: string;
  itemType: 'NUMERICAL' | 'DOLLARS' | 'COLONES';
  total: number;
  timeSeriesData: TimeSeriesPoint[];
}

export interface PendingLink {
  token: string;
  reporterUsername: string;
  reporterFullName: string | null;
  churchName: string;
  activeDate: string; // ISO date
}

export interface StatisticsReport {
  templateId: number;
  templateName: string;
  churchName: string | null;
  global: boolean;
  startDate: string;
  endDate: string;
  totalServiceCount: number;
  celebrantStats: CelebrantStat[];
  numericalItems: AggregatedItem[];
  moneyItems: AggregatedItem[];
  pendingLinks: PendingLink[];
}

export interface StatisticsParams {
  templateId: number;
  churchName?: string;
  startDate: string;
  endDate: string;
}

export async function getStatisticsTemplates(): Promise<ServiceTemplateSummary[]> {
  const response = await api.get<ServiceTemplateSummary[]>('/api/statistics/templates');
  return response.data;
}

export async function getStatistics(params: StatisticsParams): Promise<StatisticsReport> {
  const queryParams: Record<string, string> = {
    templateId: String(params.templateId),
    startDate: params.startDate,
    endDate: params.endDate,
  };
  if (params.churchName !== undefined) {
    queryParams.churchName = params.churchName;
  }
  const response = await api.get<StatisticsReport>('/api/statistics', { params: queryParams });
  return response.data;
}

export interface DrillDownRow {
  serviceInstanceId: number;
  churchName: string;
  filledByUsername: string | null;
  filledByFullName: string | null;
  value: number;
}

export interface ChartDrillDown {
  itemId: number;
  itemTitle: string;
  itemType: 'NUMERICAL' | 'DOLLARS' | 'COLONES';
  date: string; // ISO date
  rows: DrillDownRow[];
}

export interface DrillDownParams {
  templateId: number;
  itemId: number;
  date: string; // ISO date
  churchName?: string;
}

export async function getDrillDown(params: DrillDownParams): Promise<ChartDrillDown> {
  const queryParams: Record<string, string> = {
    templateId: String(params.templateId),
    itemId: String(params.itemId),
    date: params.date,
  };
  if (params.churchName !== undefined) {
    queryParams.churchName = params.churchName;
  }
  const response = await api.get<ChartDrillDown>('/api/statistics/drill-down', {
    params: queryParams,
  });
  return response.data;
}
