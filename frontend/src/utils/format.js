export const formatCurrency = (value) => {
  if (value === null || value === undefined || isNaN(value)) return '0,00 ₺'
  return new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(value)
}

// Para birimine duyarli bicimlendirme. Kayit para birimi TRY disinda ise dogru
// sembolu gosterir (yanlis ₺ algisini onler). Sembol eslemesi dovizStore ile aynidir.
const PARA_SEMBOL = { TRY: '₺', USD: '$', EUR: '€', GBP: '£', SAR: '﷼', GAU: ' GAU', BTC: '₿' }

export const formatPara = (value, paraBirimi = 'TRY') => {
  const v = value === null || value === undefined || isNaN(value) ? 0 : value
  const birim = (paraBirimi || 'TRY').toUpperCase()
  const sembol = PARA_SEMBOL[birim] || ` ${birim}`
  const formatted = new Intl.NumberFormat('tr-TR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
  return birim === 'GAU' ? `${formatted}${sembol}` : `${formatted} ${sembol}`
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

// Yerel (TR) saat dilimine göre YYYY-MM-DD üretir. toISOString() UTC kullandığı için
// gece 00:00-02:59 arası bir önceki güne kayma hatasını önler.
export const getLocalDateString = (date = new Date()) => {
  if (date === null || date === undefined) return ''
  if (typeof date === 'string') {
    if (/^\d{4}-\d{2}-\d{2}$/.test(date)) return date
    date = new Date(date)
  }
  if (!(date instanceof Date) || isNaN(date.getTime())) return ''
  const yil = date.getFullYear()
  const ay = String(date.getMonth() + 1).padStart(2, '0')
  const gun = String(date.getDate()).padStart(2, '0')
  return `${yil}-${ay}-${gun}`
}

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

// Vade riski renk sinifi. Makine-okunur `gun` (gecikme gün sayısı) kullanılır;
// gosterim metni (`aralik`) ceviri veya bicim degisince bozulmamalidir.
// Esikler backend ile ayni kovalari izler (RaporService.aralik / TahsilatService.aralik):
//   gun <= 0 -> "Vadesi Gelmemis", <=30, <=60, <=90, uzeri.
// "0-30 Gün" kovasi vadesi 1-30 gün GECMIS alacaklari icerir; bu yuzden risk-yok
// rengi yalnizca vadesi gelmemis (gun <= 0) kayitlara verilir.
export const vadeRiskSinifi = (satir) => {
  const ham = satir?.gun
  let gun = typeof ham === 'number' ? ham : Number.parseInt(ham, 10)
  if (Number.isNaN(gun)) {
    // Eski API yanitlarinda `gun` yoktur; bu durumda gosterim metninden kovar.
    const aralik = String(satir?.aralik || '')
    if (aralik.startsWith('Vadesi Gelmemiş')) gun = 0
    else if (aralik.startsWith('0')) gun = 30
    else if (aralik.startsWith('31')) gun = 60
    else if (aralik.startsWith('61')) gun = 90
    else gun = 91
  }
  if (gun <= 0) return 'risk-yok'
  if (gun <= 30) return 'risk-az'
  if (gun <= 60) return 'risk-orta'
  return 'risk-yuksek'
}
