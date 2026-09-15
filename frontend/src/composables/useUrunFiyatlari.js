import { ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { stokAPI, cariHesapAPI, faturaAPI } from '../api/index.js'

/**
 * Bir cari + ürün için uygulanabilir tüm fiyat seçeneklerini toplar:
 *  - Son satış fiyatı (bu cariye bu ürünün en son kesilen satış fiyatı)
 *  - Cari özel fiyat (cariye tanımlı özel fiyat)
 *  - Stok fiyat seviyeleri (Perakende/Toptan/Kurumsal...)
 *  - Liste fiyatı (stok kartındaki temel fiyat)
 */
export function useUrunFiyatlari() {
  const { t } = useI18n()
  const secenekler = ref([])
  const yukleniyor = ref(false)

  const ekle = (ad, fiyat) => {
    if (fiyat === null || fiyat === undefined || fiyat === '') return
    const sayisal = Number(fiyat)
    if (Number.isNaN(sayisal)) return
    secenekler.value.push({ ad, fiyat: sayisal })
  }

  const yukle = async (cariId, stokId, stokFallback) => {
    secenekler.value = []
    if (!stokId) return
    yukleniyor.value = true
    try {
      if (cariId) {
        try {
          const r = await faturaAPI.cariUrunFiyatGecmisi(cariId, stokId)
          ekle(t('cariFiyat.kaynakSonSatis'), r.data?.sonFiyat)
        } catch {
          /* kayıt yok */
        }
        try {
          const r = await cariHesapAPI.getFiyatlar(cariId)
          const ozel = (r.data || []).find((f) => f.stokId === stokId)
          ekle(t('cariFiyat.kaynakOzel'), ozel?.fiyat)
        } catch {
          /* kayıt yok */
        }
      }
      try {
        const r = await stokAPI.getFiyatlar(stokId)
        for (const f of r.data || []) {
          ekle(f.ad || t('cariFiyat.kaynakSeviye'), f.fiyat)
        }
      } catch {
        /* kayıt yok */
      }
      ekle(t('cariFiyat.kaynakListe'), stokFallback)
      // Aynı fiyat değerlerini tekilleştir (ilk gelen öncelikli).
      const gorulen = new Set()
      secenekler.value = secenekler.value.filter((s) => {
        if (gorulen.has(s.fiyat)) return false
        gorulen.add(s.fiyat)
        return true
      })
    } finally {
      yukleniyor.value = false
    }
  }

  const temizle = () => {
    secenekler.value = []
  }

  return { secenekler, yukleniyor, yukle, temizle }
}
