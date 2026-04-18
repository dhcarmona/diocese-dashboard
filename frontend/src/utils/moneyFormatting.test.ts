import { describe, expect, it } from 'vitest';
import { formatMoneyDisplay, parseMoneyInput } from './moneyFormatting';

describe('formatMoneyDisplay', () => {
  it('formats a whole number with 2 decimal places and thousands separator', () => {
    expect(formatMoneyDisplay('1500')).toBe('1,500.00');
  });

  it('formats a decimal value with proper rounding', () => {
    expect(formatMoneyDisplay('1500.5')).toBe('1,500.50');
  });

  it('formats a large number with multiple thousands separators', () => {
    expect(formatMoneyDisplay('1234567.89')).toBe('1,234,567.89');
  });

  it('returns empty string for empty input', () => {
    expect(formatMoneyDisplay('')).toBe('');
  });

  it('returns the original string for non-numeric input', () => {
    expect(formatMoneyDisplay('abc')).toBe('abc');
  });

  it('formats zero correctly', () => {
    expect(formatMoneyDisplay('0')).toBe('0.00');
  });
});

describe('parseMoneyInput', () => {
  it('strips thousands commas', () => {
    expect(parseMoneyInput('1,500.00')).toBe('1500.00');
  });

  it('handles multiple commas', () => {
    expect(parseMoneyInput('1,234,567.89')).toBe('1234567.89');
  });

  it('leaves values without commas unchanged', () => {
    expect(parseMoneyInput('1500.50')).toBe('1500.50');
  });

  it('handles empty string', () => {
    expect(parseMoneyInput('')).toBe('');
  });
});
