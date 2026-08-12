import { describe, it, expect } from 'vitest'
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  durumLabel
} from '../format.js'

describe('format.js', () => {
  it('formatCurrency handles null/undefined/NaN', () => {
    expect(formatCurrency(null)).toBe('0,00 ₺')
    expect(formatCurrency(undefined)).toBe('0,00 ₺')
    expect(formatCurrency(NaN)).toBe('0,00 ₺')
  })

  it('formatCurrency formats valid number', () => {
    expect(formatCurrency(1000)).toContain('1.000')
  })

  it('formatDate returns empty for invalid', () => {
    expect(formatDate(null)).toBe('')
    expect(formatDate('invalid')).toBe('')
  })

  it('formatDate formats valid date', () => {
    expect(formatDate('2026-01-01')).not.toBe('')
  })

  it('formatDateTime includes date and time', () => {
    expect(formatDateTime('2026-01-01T10:30:00')).toContain('2026')
    expect(formatDateTime(null)).toBe('')
  })

  it('durumLabel maps known values', () => {
    expect(durumLabel('TASLAK')).toBe('Taslak')
    expect(durumLabel('KESILDI')).toBe('Kesildi')
    expect(durumLabel('BILINMEYEN')).toBe('BILINMEYEN')
  })
})
