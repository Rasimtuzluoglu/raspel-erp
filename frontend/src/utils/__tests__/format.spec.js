import { describe, it, expect } from 'vitest'
import {
  formatCurrency,
  formatPara,
  formatDate,
  formatDateTime,
  durumLabel,
  getLocalDateString,
  vadeRiskSinifi
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

// Esikler backend kovalariyla ayni olmali: <=0 vadesi gelmemis, <=30, <=60, <=90, uzeri.
describe('vadeRiskSinifi', () => {
  it('makine-okunur gun alanini kullanir', () => {
    expect(vadeRiskSinifi({ gun: 0 })).toBe('risk-yok')
    expect(vadeRiskSinifi({ gun: -5 })).toBe('risk-yok')
    expect(vadeRiskSinifi({ gun: 1 })).toBe('risk-az')
    expect(vadeRiskSinifi({ gun: 30 })).toBe('risk-az')
    expect(vadeRiskSinifi({ gun: 31 })).toBe('risk-orta')
    expect(vadeRiskSinifi({ gun: 60 })).toBe('risk-orta')
    expect(vadeRiskSinifi({ gun: 61 })).toBe('risk-yuksek')
    expect(vadeRiskSinifi({ gun: 365 })).toBe('risk-yuksek')
  })

  // Regresyon: "0-30 Gün" kovasi vadesi 1-30 gün gecmis alacaklari icerir ve
  // daha once risk-yok (yesil) renklendiriliyordu.
  it('vadesi gecmis kayitlari risk-yok olarak isaretlemez', () => {
    expect(vadeRiskSinifi({ gun: 30, aralik: '0-30 Gün' })).toBe('risk-az')
    expect(vadeRiskSinifi({ gun: 1, aralik: '0-30 Gün' })).not.toBe('risk-yok')
    expect(vadeRiskSinifi({ gun: 0, aralik: 'Vadesi Gelmemiş' })).toBe('risk-yok')
  })

  it('gosterim metni uyumsuz olsa da gun alani esas alinir', () => {
    expect(vadeRiskSinifi({ gun: 75, aralik: 'Vadesi Gelmemiş' })).toBe('risk-yuksek')
    expect(vadeRiskSinifi({ gun: 0, aralik: '90+ Gün' })).toBe('risk-yok')
  })

  it('string gun degerlerini kabul eder', () => {
    expect(vadeRiskSinifi({ gun: '45' })).toBe('risk-orta')
    expect(vadeRiskSinifi({ gun: '0' })).toBe('risk-yok')
  })

  it('gun yoksa gosterim metnine duser (eski API uyumlulugu)', () => {
    expect(vadeRiskSinifi({ aralik: 'Vadesi Gelmemiş' })).toBe('risk-yok')
    expect(vadeRiskSinifi({ aralik: '0-30 Gün' })).toBe('risk-az')
    expect(vadeRiskSinifi({ aralik: '31-60 Gün' })).toBe('risk-orta')
    expect(vadeRiskSinifi({ aralik: '61-90 Gün' })).toBe('risk-yuksek')
    expect(vadeRiskSinifi({ aralik: '90+ Gün' })).toBe('risk-yuksek')
  })

  it('eksik satirda risk-yuksek doner (temkinli varsayilan)', () => {
    expect(vadeRiskSinifi(undefined)).toBe('risk-yuksek')
    expect(vadeRiskSinifi({})).toBe('risk-yuksek')
    expect(vadeRiskSinifi({ gun: null })).toBe('risk-yuksek')
  })
})
