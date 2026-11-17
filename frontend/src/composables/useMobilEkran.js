import { onBeforeUnmount, onMounted, ref } from 'vue'

/**
 * Küçük ekran (mobil) algılama. SSR/test ortamında güvenli varsayılan: false.
 *
 * @param {string} sorgu matchMedia sorgusu
 */
export function useMobilEkran(sorgu = '(max-width: 640px)') {
  const mobil = ref(false)
  let medya = null

  const guncelle = () => {
    mobil.value = !!medya?.matches
  }

  onMounted(() => {
    if (typeof window === 'undefined' || typeof window.matchMedia !== 'function') return
    medya = window.matchMedia(sorgu)
    guncelle()
    medya.addEventListener?.('change', guncelle)
  })

  onBeforeUnmount(() => {
    medya?.removeEventListener?.('change', guncelle)
    medya = null
  })

  return mobil
}
