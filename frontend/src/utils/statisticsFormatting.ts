/**
 * Formats a statistics value for display, applying currency symbols for money types.
 *
 * @param value  the numeric value to format
 * @param type   the item type: 'DOLLARS', 'COLONES', or 'NUMERICAL'
 * @returns formatted string representation
 */
export function formatStatisticsValue(value: number, type: string): string {
  if (type === 'DOLLARS')
    return `$${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  if (type === 'COLONES')
    return `₡${value.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  return value.toLocaleString('en-US');
}
