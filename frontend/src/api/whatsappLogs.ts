import { api } from './auth';

export interface WhatsAppMessageLogEntry {
  id: number;
  sentAt: string; // ISO instant string
  recipientUsername: string;
  body: string | null;
  otp: boolean;
}

export interface WhatsAppMessageLogPage {
  content: WhatsAppMessageLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number; // current page, 0-based
  size: number;
}

/** Fetches a page of WhatsApp message log entries, newest first. Admin only. */
export async function getWhatsAppMessageLogs(
  page = 0,
  size = 25,
): Promise<WhatsAppMessageLogPage> {
  const response = await api.get<WhatsAppMessageLogPage>('/api/whatsapp-logs', {
    params: { page, size },
  });
  return response.data;
}
