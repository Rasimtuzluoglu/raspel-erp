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

export const formatDate = (date) => {
  const d = parseDate(date)
  if (!d) return ''
  return d.toLocaleDateString('tr-TR')
}

export const formatDateTime = (date) => {
  const d = parseDate(date)
  if (!d) return ''
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
}

export const formatTarih = (date) => formatDate(date) || '-'

export const formatTarihSaat = (date) => {
  const d = parseDate(date)
  if (!d) return '-'
  return d.toLocaleString('tr-TR')
}

export const durumLabel = (durum) => {
  const labels = {
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
  return labels[durum] || durum
}
