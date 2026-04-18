const MONEY_FORMAT_OPTIONS: Intl.NumberFormatOptions = {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
};

const NUMERIC_PATTERN = /^[+-]?(?:\d+\.?\d*|\.\d+)$/;

/**
 * Formats a raw numeric string for display, adding thousands commas and 2 decimal places.
 * e.g. "1500.5" → "1,500.50", "" → ""
 */
export function formatMoneyDisplay(rawValue: string): string {
  if (!rawValue) return '';
  const trimmed = rawValue.trim();
  if (!NUMERIC_PATTERN.test(trimmed)) return rawValue;
  const num = Number(trimmed);
  if (!Number.isFinite(num)) return rawValue;
  return num.toLocaleString('en-US', MONEY_FORMAT_OPTIONS);
}

/**
 * Normalizes a typed money value so it can be stored as a plain numeric string.
 * Removes grouping commas, currency symbols ($, ₡), and whitespace, and only returns
 * values that match a valid numeric format.
 * e.g. " $1,500.50 " → "1500.50"
 */
export function parseMoneyInput(value: string): string {
  const normalized = value.trim().replace(/[$₡,\s]/g, '');
  if (!normalized) return '';
  if (!NUMERIC_PATTERN.test(normalized)) return '';
  return normalized;
}
