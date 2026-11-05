/**
 * Anasayfa kisisel not/todo yardimcilari.
 *
 * Notlar yalnizca istemcide (localStorage) tutulur; backend'e gonderilmez.
 * Anahtar kullanici bazlidir: `raspel_dashboard_notlar_<kullaniciId>`.
 * Kullanici kimligi yoksa `genel` anahtari kullanilir.
 */

export const MAX_NOT = 50
export const MAX_NOT_METIN = 200
export const NOTLAR_OLAYI = 'raspel:dashboard-notlar-guncellendi'

export const notlarAnahtari = (kullaniciId) =>
  `raspel_dashboard_notlar_${kullaniciId === null || kullaniciId === undefined ? 'genel' : kullaniciId}`

const guvenliDepo = () => {
  try {
    return typeof localStorage !== 'undefined' ? localStorage : null
  } catch {
    return null
  }
}

const metniTemizle = (deger) => (typeof deger === 'string' ? deger.trim() : '')

const notGecerliMi = (not) =>
  not &&
  typeof not === 'object' &&
  not.id !== null &&
  not.id !== undefined &&
  metniTemizle(not.metin).length > 0

/** Kayitli notlari dogrular, temizler ve en fazla `MAX_NOT` kayit dondurur. */
export const notlarOku = (kullaniciId, depo = guvenliDepo()) => {
  try {
    const ham = depo?.getItem?.(notlarAnahtari(kullaniciId))
    if (!ham) return []
    const kayit = JSON.parse(ham)
    if (!Array.isArray(kayit)) return []
    return kayit
      .filter(notGecerliMi)
      .slice(0, MAX_NOT)
      .map((not) => ({
        id: not.id,
        metin: metniTemizle(not.metin).slice(0, MAX_NOT_METIN),
        tamam: not.tamam === true,
        tarih: typeof not.tarih === 'string' ? not.tarih : ''
      }))
  } catch {
    return []
  }
}

/** Notlari cihaza yazar; en fazla `MAX_NOT` kayit tutar. */
export const notlarYaz = (notlar, kullaniciId, depo = guvenliDepo()) => {
  const sinirli = Array.isArray(notlar) ? notlar.filter(notGecerliMi).slice(0, MAX_NOT) : []
  try {
    depo?.setItem?.(notlarAnahtari(kullaniciId), JSON.stringify(sinirli))
  } catch {
    /* depolama kapaliysa sessizce yut */
  }
  return sinirli
}

/** Acik (tamamlanmamis) not sayisi; ust bar rozetini besler. */
export const acikNotSayisi = (kullaniciId, depo = guvenliDepo()) =>
  notlarOku(kullaniciId, depo).filter((not) => !not.tamam).length

/** Yeni not kimligi (mumkunse crypto.randomUUID, degilse zaman + rastgele). */
export const yeniNotId = () => {
  try {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID()
    }
  } catch {
    /* yoksay */
  }
  return `not-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`
}

/** Diger bilesenleri (ust bar rozeti) bilgilendiren istemci olayi. */
export const notlarOlayiYay = (detay = {}) => {
  try {
    if (typeof window !== 'undefined' && typeof window.dispatchEvent === 'function') {
      window.dispatchEvent(new CustomEvent(NOTLAR_OLAYI, { detail: detay }))
    }
  } catch {
    /* yoksay */
  }
}
