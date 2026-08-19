export const formatCurrency = (value) => {
  if (value === null || value === undefined || isNaN(value)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value)
}

export const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleDateString('tr-TR')
}

export const formatDateTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  if (isNaN(d.getTime())) return ''
  return d.toLocaleDateString('tr-TR') + ' ' + d.toLocaleTimeString('tr-TR', { hour: '2-digit', minute: '2-digit' })
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
