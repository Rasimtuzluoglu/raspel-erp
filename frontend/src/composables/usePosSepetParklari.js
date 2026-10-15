import { ref, computed } from 'vue'

/**
 * POS sepet parklari — birden fazla sepeti askiya alma (park) ve geri alma.
 *
 * Gercek kasa akisinda cok yaygin: musteri "kartini birakti, sonra devam"
 * der, kasiyer mevcut sepeti BIRAKIP yeni musteriye gecer, sonra doner.
 * Tek aktif sepet bunu engelliyordu (sepeti temizlemek zorunda kaliyordu).
 *
 * Bu composable SIRSIZ, SAF bir liste tutar: park edilen sepetlerin KOPYASI
 * saklanir (referans paylasilmaz), boylece parktan sonra aktif sepet
 * degistirilse bile parklanan icerik bozulmaz.
 */

/** Guvenli derin kopya: sepet satirlari duz nesnelerdir (Date/fonksiyon yok). */
const kopyala = (v) => (v == null ? v : JSON.parse(JSON.stringify(v)))

export function usePosSepetParklari({ maks = 5 } = {}) {
  const parklar = ref([])

  /**
   * Aktif sepeti park eder.
   * @param {{baslik?: string, sepet: Array, musteri?: object|null}} veri
   * @returns {object|null} Park kaydi veya limit dolduysa null
   */
  const parkEt = (veri = {}) => {
    if (parklar.value.length >= maks) return null
    const kayit = {
      id: `${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
      tarih: new Date().toISOString(),
      baslik: veri.baslik || '',
      sepet: kopyala(veri.sepet || []),
      musteri: kopyala(veri.musteri || null)
    }
    parklar.value.push(kayit)
    return kayit
  }

  /**
   * Parklanan sepeti geri alir ve listeden cikarir.
   * @returns {object|null}
   */
  const devamEt = (id) => {
    const idx = parklar.value.findIndex((p) => p.id === id)
    if (idx < 0) return null
    const [kayit] = parklar.value.splice(idx, 1)
    return kayit
  }

  const sil = (id) => {
    parklar.value = parklar.value.filter((p) => p.id !== id)
  }

  const temizle = () => {
    parklar.value = []
  }

  const doluMu = computed(() => parklar.value.length >= maks)

  return { parklar, parkEt, devamEt, sil, temizle, doluMu }
}
