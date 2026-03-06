import { describe, it, expect } from 'vitest';
import {
  formatDate,
  formatDisplayDate,
  formatMonthYear,
  formatCurrency,
  formatShortDate,
  getTodayDate,
  getDateMonthsAgo,
} from '../utils/date-utils';

describe('formatDate', () => {
  it('converts ISO date to MM-DD-YYYY', () => {
    expect(formatDate('2025-03-15')).toBe('03-15-2025');
  });

  it('returns "No date" for null', () => {
    expect(formatDate(null)).toBe('No date');
  });

  it('returns "No date" for undefined', () => {
    expect(formatDate(undefined)).toBe('No date');
  });

  it('returns "No date" for empty string', () => {
    expect(formatDate('')).toBe('No date');
  });
});

describe('formatDisplayDate', () => {
  it('formats date as "Mon DD, YYYY"', () => {
    // Use a fixed date to avoid timezone edge cases — new Date('2025-01-01') can land on Dec 31
    // We test the structure rather than a hard-coded string
    const result = formatDisplayDate('2025-06-15');
    expect(result).toMatch(/^[A-Z][a-z]{2} \d+, 2025$/);
    expect(result).toContain('2025');
  });
});

describe('formatMonthYear', () => {
  it('converts YYYY-MM to "Mon YYYY"', () => {
    expect(formatMonthYear('2025-03')).toBe('Mar 2025');
  });

  it('handles January correctly (month index 0)', () => {
    expect(formatMonthYear('2025-01')).toBe('Jan 2025');
  });

  it('handles December correctly (month index 11)', () => {
    expect(formatMonthYear('2025-12')).toBe('Dec 2025');
  });
});

describe('formatCurrency', () => {
  it('formats a positive number as USD', () => {
    expect(formatCurrency(1234.56)).toBe('$1,234.56');
  });

  it('formats zero as $0.00', () => {
    expect(formatCurrency(0)).toBe('$0.00');
  });

  it('formats null as $0.00', () => {
    expect(formatCurrency(null)).toBe('$0.00');
  });

  it('formats undefined as $0.00', () => {
    expect(formatCurrency(undefined)).toBe('$0.00');
  });

  it('formats negative amounts correctly', () => {
    expect(formatCurrency(-50)).toContain('50');
  });
});

describe('formatShortDate', () => {
  it('returns a string with a month abbreviation and day number', () => {
    const result = formatShortDate('2025-03-05');
    expect(result).toMatch(/\d{1,2} [A-Z][a-z]{2}/);
  });
});

describe('getTodayDate', () => {
  it('returns a string in YYYY-MM-DD format', () => {
    expect(getTodayDate()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('matches the current date', () => {
    const today = new Date().toISOString().split('T')[0];
    expect(getTodayDate()).toBe(today);
  });
});

describe('getDateMonthsAgo', () => {
  it('returns a string in YYYY-MM-DD format', () => {
    expect(getDateMonthsAgo(3)).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('returns a date in the past', () => {
    const result = new Date(getDateMonthsAgo(1));
    expect(result.getTime()).toBeLessThan(Date.now());
  });

  it('returns today when months is 0', () => {
    const today = getTodayDate();
    expect(getDateMonthsAgo(0)).toBe(today);
  });
});
