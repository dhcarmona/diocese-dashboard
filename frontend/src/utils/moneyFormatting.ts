const MONEY_FORMAT_OPTIONS: Intl.NumberFormatOptions = {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
};

/**
 * Formats a raw numeric string for display, adding thousands commas and 2 decimal places.
 * e.g. "1500.5" → "1,500.50", "" → ""
 */
export function formatMoneyDisplay(rawValue: string): string {
  if (!rawValue) return '';
  const num = parseFloat(rawValue);
  if (isNaN(num)) return rawValue;
  return num.toLocaleString('en-US', MONEY_FORMAT_OPTIONS);
}

/**
 * Strips thousands commas from a typed value so it can be stored as a plain numeric string.
 * e.g. "1,500.50" → "1500.50"
 */
export function parseMoneyInput(value: string): string {
  return value.replace(/,/g, '');
}
