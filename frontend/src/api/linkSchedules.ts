import { api, getCsrfHeaders } from './auth';

export type DayOfWeek =
  | 'MONDAY'
  | 'TUESDAY'
  | 'WEDNESDAY'
  | 'THURSDAY'
  | 'FRIDAY'
  | 'SATURDAY'
  | 'SUNDAY';

export const ALL_DAYS: DayOfWeek[] = [
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
];

export interface LinkSchedule {
  id: number;
  serviceTemplateId: number;
  serviceTemplateName: string;
  churchNames: string[];
  daysOfWeek: DayOfWeek[];
  /** Hour 0–23 in Costa Rica time (America/Costa_Rica, UTC-6). */
  sendHour: number;
  /** ISO date string of last trigger, or null if never triggered. */
  lastTriggeredDate: string | null;
  createdAt: string;
}

export interface LinkSchedulePayload {
  serviceTemplateId: number;
  churchNames: string[];
  daysOfWeek: DayOfWeek[];
  /** Hour 0–23 in Costa Rica time (America/Costa_Rica, UTC-6). */
  sendHour: number;
}

export async function getLinkSchedules(): Promise<LinkSchedule[]> {
  const response = await api.get<LinkSchedule[]>('/api/link-schedules');
  return response.data;
}

export async function createLinkSchedule(payload: LinkSchedulePayload): Promise<LinkSchedule> {
  const response = await api.post<LinkSchedule>('/api/link-schedules', payload, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function updateLinkSchedule(
  id: number,
  payload: LinkSchedulePayload,
): Promise<LinkSchedule> {
  const response = await api.put<LinkSchedule>(`/api/link-schedules/${id}`, payload, {
    headers: await getCsrfHeaders(),
  });
  return response.data;
}

export async function deleteLinkSchedule(id: number): Promise<void> {
  await api.delete(`/api/link-schedules/${id}`, {
    headers: await getCsrfHeaders(),
  });
}
