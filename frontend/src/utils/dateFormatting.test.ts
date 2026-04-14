import { describe, expect, it } from 'vitest';
import { formatDate, formatDateTime, getAppDateLocale } from './dateFormatting';

describe('dateFormatting', () => {
  it('formats dates with English month abbreviations', () => {
    expect(formatDate('2026-04-13', 'en')).toBe('13 Apr 2026');
  });

  it('formats dates with Spanish month abbreviations', () => {
    expect(formatDate('2026-04-13', 'es')).toBe('13 Abr 2026');
  });

  it('formats datetimes while preserving the standardized date portion', () => {
    expect(formatDateTime('2026-04-13T09:45:00', 'en')).toBe('13 Apr 2026, 09:45');
  });

  it('maps regional locale codes to supported picker locales', () => {
    expect(getAppDateLocale('en-US')).toBe('en');
    expect(getAppDateLocale('es-CR')).toBe('es');
  });
});
