import { describe, it, expect } from 'vitest'
import { formatCurrency, formatPara, formatDate, formatDateTime, durumLabel, getLocalDateString } from '../format.js'

describe('format.js', () => {
  it('formatCurrency handles null/undefined/NaN', () => {
    expect(formatCurrency(null)).toBe('0,00 ₺')
    expect(formatCurrency(undefined)).toBe('0,00 ₺')
    expect(formatCurrency(NaN)).toBe('0,00 ₺')
  })

  it('formatCurrency formats valid number', () => {
    expect(formatCurrency(1000)).toContain('1.000')
  })

  it('formatPara para birimine gore sembol kullanir', () => {
    expect(formatPara(1000, 'TRY')).toContain('₺')
    expect(formatPara(1000, 'USD')).toContain('$')
    expect(formatPara(1000, 'EUR')).toContain('€')
    expect(formatPara(1000)).toContain('₺')
    expect(formatPara(null, 'USD')).toContain('$')
  })

  it('formatPara bilinmeyen birimde kodu gosterir', () => {
    expect(formatPara(100, 'XYZ')).toContain('XYZ')
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

  it('durumLabel uses i18n keys when a translator is provided', () => {
    const fakeT = (key) => `tr:${key}`
    expect(durumLabel('TASLAK', fakeT)).toBe('tr:common.durumTaslak')
    expect(durumLabel('ODEME', fakeT)).toBe('tr:common.durumOdeme')
    expect(durumLabel('BILINMEYEN', fakeT)).toBe('BILINMEYEN')
  })

  it('getLocalDateString defaults to current local day', () => {
    const now = new Date()
    const beklenen = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    expect(getLocalDateString()).toBe(beklenen)
  })

  it('getLocalDateString uses local components (gece 00:00-02:59 dahil)', () => {
    const gece = new Date(2026, 8, 15, 1, 30)
    expect(getLocalDateString(gece)).toBe('2026-09-15')
  })

  it('getLocalDateString keeps an existing YYYY-MM-DD string', () => {
    expect(getLocalDateString('2026-09-15')).toBe('2026-09-15')
  })

  it('getLocalDateString handles datetime strings, null and invalid', () => {
    const now = new Date()
    const bugun = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
    expect(getLocalDateString('2026-09-15T10:30:00')).toBe('2026-09-15')
    expect(getLocalDateString(null)).toBe('')
    expect(getLocalDateString(undefined)).toBe(bugun)
    expect(getLocalDateString('gecersiz')).toBe('')
  })
})
