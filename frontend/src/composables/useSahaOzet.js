import { ref } from 'vue'
import { sahaAPI } from '../api/index.js'

const THROTTLE_MS = 60_000

/**
 * Saha "Bugün" özeti. 60 saniye içinde tekrar istek atılmaz; `zorla: true`
 * (kullanıcı yenile dediğinde) bu sınırı atlar.
 */
export function useSahaOzet() {
  const ozet = ref(null)
  const yukleniyor = ref(false)
  const hata = ref(null)
  let sonYukleme = 0

  const yukle = async ({ zorla = false } = {}) => {
    const simdi = Date.now()
    if (!zorla && sonYukleme && simdi - sonYukleme < THROTTLE_MS) {
      return ozet.value
    }
    yukleniyor.value = true
    hata.value = null
    try {
      const yanit = await sahaAPI.ozet()
      ozet.value = yanit?.data || null
      sonYukleme = Date.now()
      return ozet.value
    } catch (err) {
      hata.value = err?.message ? String(err.message) : 'Özet yüklenemedi'
      return null
    } finally {
      yukleniyor.value = false
    }
  }

  return { ozet, yukleniyor, hata, yukle }
}
