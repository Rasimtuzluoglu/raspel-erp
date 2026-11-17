/**
 * Saha kartlarındaki dış bağlantılar (harita, telefon, WhatsApp).
 * Adres/telefon boşsa `null` döner; bileşenler bu durumda aksiyonu gizler.
 */

export function haritaUrl(adres) {
  const temiz = (adres || '').trim()
  if (!temiz) return null
  return `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent(temiz)}`
}

/** "0500 111 22 33" → "905001112233" (wa.me biçimi). */
export function whatsappNumarasi(telefon) {
  const rakamlar = (telefon || '').replace(/\D/g, '')
  if (!rakamlar) return null
  if (rakamlar.startsWith('90')) return rakamlar
  if (rakamlar.startsWith('0')) return `90${rakamlar.slice(1)}`
  if (rakamlar.length === 10) return `90${rakamlar}`
  return rakamlar
}

export function whatsappUrl(telefon) {
  const numara = whatsappNumarasi(telefon)
  return numara ? `https://wa.me/${numara}` : null
}

export function telUrl(telefon) {
  const temiz = (telefon || '').trim()
  if (!temiz) return null
  return `tel:${temiz.replace(/\s+/g, '')}`
}
