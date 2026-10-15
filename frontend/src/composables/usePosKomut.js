/**
 * POS komut cozumleyici — SAF fonksiyon (yan etki yok, kolay test edilir).
 *
 * Amac: kasiyerin TEK bir kutudan yaptigi girisleri anlamlandirmak; boylece
 * barkod kutusu / urun arama kutusu / musteri arama arasinda odak ziplamasina
 * gerek kalmaz.
 *
 * GRAMER (kucuk ve tahmin edilebilir tutuldu):
 *   "123456"        -> urun arama/barkod (miktar yok)
 *   "3*1234"        -> 1234 urununu 3 adet ekle (onek)
 *   "1234*3"        -> ayni (sonek)
 *   "3x vida"       -> "vida" aramasinda 3 adet
 *   "@ali veli"     -> musteri ara/sec
 *
 * Belirsizlik: `*`/`x` hem onek hem sonek olarak kabul edilir; once onek
 * denenir (cunku bas taraftaki sayi daha net bir "miktar" sinyalidir).
 */

export const KOMUT_TIP = Object.freeze({
  BOS: 'bos',
  MUSTERI: 'musteri',
  URUN: 'urun'
})

const MAKS_MIKTAR = 9999

/** Miktari 1..9999 araligina ceker; gecersizde 1. */
export function miktarNormalize(n) {
  const s = Number(n)
  if (!Number.isFinite(s) || s < 1) return 1
  return Math.min(Math.trunc(s), MAKS_MIKTAR)
}

/**
 * Ham kutudaki metni bir komuta cevirir.
 * @param {string} metin
 * @returns {{tip: string, metin: string, miktar?: number}}
 */
export function komutCoz(metin) {
  const ham = (metin ?? '').toString().trim()
  if (!ham) return { tip: KOMUT_TIP.BOS, metin: '' }

  // Musteri: @ad
  if (ham.startsWith('@')) {
    const ad = ham.slice(1).trim()
    if (!ad) return { tip: KOMUT_TIP.BOS, metin: '' }
    return { tip: KOMUT_TIP.MUSTERI, metin: ad }
  }

  // Onek miktar: "3*urun" / "3x urun"
  let m = /^(\d{1,6})\s*[x*]\s*(.+)$/i.exec(ham)
  if (m) {
    const kalan = m[2].trim()
    if (kalan) return { tip: KOMUT_TIP.URUN, metin: kalan, miktar: miktarNormalize(m[1]) }
  }

  // Sonek miktar: "urun*3" / "urun x 3"
  m = /^(.+?)\s*[x*]\s*(\d{1,6})$/i.exec(ham)
  if (m) {
    const kalan = m[1].trim()
    if (kalan) return { tip: KOMUT_TIP.URUN, metin: kalan, miktar: miktarNormalize(m[2]) }
  }

  // Duz urun/barkod aramasi (miktar belirtilmemis).
  return { tip: KOMUT_TIP.URUN, metin: ham }
}
