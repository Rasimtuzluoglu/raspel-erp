export const formatCurrency = (value) => {
  if (value === null || value === undefined || isNaN(value)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value)
}

const parseDate = (value) => {
  if (!value) return null
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)) {
    const [y, m, d] = value.split('-').map(Number)
    return new Date(y, m - 1, d)
  }
  const d = new Date(value)
  return isNaN(d.getTime()) ? null : d
}

export const formatDate = (date, bos = '') => {
  const d = parseDate(date)
  if (!d) return bos
  return d.toLocaleDateString('tr-TR')
}

export const formatDateTime = (date, bos = '') => {
  const d = parseDate(date)
  if (!d) return bos
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
}

export const formatTarih = (date) => formatDate(date) || '-'

export const formatTarihSaat = (date, bos = '-') => {
  const d = parseDate(date)
  if (!d) return bos
  return d.toLocaleString('tr-TR')
}

export const formatTarihKisa = (date, bos = '') => {
  const d = parseDate(date)
  if (!d) return bos
  return new Intl.DateTimeFormat('tr-TR', { dateStyle: 'short', timeStyle: 'short' }).format(d)
}

export const formatGunAy = (date) => {
  const d = parseDate(date)
  if (!d) return ''
  return d.toLocaleDateString('tr-TR', { day: '2-digit', month: '2-digit' })
}

export const formatGunSaat = (date) => {
  const d = parseDate(date)
  if (!d) return ''
  return d.toLocaleString('tr-TR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const DURUM_KEYS = {
  TASLAK: 'common.durumTaslak',
  TEKLIF: 'common.durumTeklif',
  KESILDI: 'common.durumKesildi',
  IPTAL: 'common.durumIptal',
  BEKLEMEDE: 'common.durumBeklemede',
  ONAYLANDI: 'common.durumOnaylandi',
  TAMAMLANDI: 'common.durumTamamlandi',
  DEVAM_EDIYOR: 'common.durumDevamEdiyor',
  PORTFOY: 'common.durumPortfoy',
  TAHSILAT: 'common.durumTahsilat',
  ODEME: 'common.durumOdeme'
}

const DURUM_TR = {
  TASLAK: 'Taslak',
  TEKLIF: 'Teklif',
  KESILDI: 'Kesildi',
  IPTAL: 'İptal',
  BEKLEMEDE: 'Beklemede',
  ONAYLANDI: 'Onaylandı',
  TAMAMLANDI: 'Tamamlandı',
  DEVAM_EDIYOR: 'Devam Ediyor',
  PORTFOY: 'Portföy',
  TAHSILAT: 'Tahsilat',
  ODEME: 'Ödeme'
}

export const durumLabel = (durum, t) => {
  const key = DURUM_KEYS[durum]
  if (!key) return durum
  return t ? t(key) : DURUM_TR[durum]
}
