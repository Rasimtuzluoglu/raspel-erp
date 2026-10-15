import { safeGet, safeSet } from '../utils/safeStorage.js'

/**
 * POS katalog onbellegi — son basarili katalog anlik goruntusu.
 *
 * Amac (Faz F, cevrimdisi dayaniklilik): sunucuya ulasilamadiginda izgara
 * BOS kalmasin; kasiyer son bildigi urunleri gormeye devam etsin.
 *
 * NEDEN IndexedDB DEGIL: gercek "offline-first katalog" icin IndexedDB + yeni
 * bagimlilik (idb-keyval) ve test icin `fake-indexeddb` gerekir; bu, fazin
 * kapsamini ve test edilebilirligini asar. Sinirli (<= 300 urun) bir
 * localStorage anlik goruntusu, yeni bagimlilik olmadan AYNI acili kapatir.
 * Buyuk katalog icin tam IndexedDB cozumu ayri bir pass olarak not edildi.
 *
 * Kayit bicimi JSON'dur; `safeStorage` zaten JSON serilestirir.
 */

const ANAHTAR = 'raspel_pos_katalog_onbellek'
export const MAKS_ONBELLEK = 300

export function usePosKatalogCache({ maksKayit = MAKS_ONBELLEK } = {}) {
  /** Son basarili urun listesini kaydeder (ilk N kayitla sinirlanir). */
  const kaydet = (urunler) => {
    const liste = Array.isArray(urunler) ? urunler.slice(0, maksKayit) : []
    safeSet(ANAHTAR, { tarih: Date.now(), urunler: liste })
  }

  /** Onbellekteki urunleri dondurur (yoksa/bzksa bos dizi). */
  const yukle = () => {
    const v = safeGet(ANAHTAR, null)
    if (!v || !Array.isArray(v.urunler)) return []
    return v.urunler
  }

  const temizle = () => safeSet(ANAHTAR, null)

  return { kaydet, yukle, temizle }
}
