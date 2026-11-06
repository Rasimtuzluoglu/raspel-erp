import { ref } from 'vue'

/**
 * `prefers-reduced-motion` tercihini bir ref olarak döndürür.
 *
 * Kokpit bileşenleri animasyonları bu bayrağa göre kapatır; CSS medya
 * sorgusu da yedek olarak kullanılır.
 */
export function useHareketAzaltma() {
  const azaltHareket = ref(
    typeof window !== 'undefined' &&
      typeof window.matchMedia === 'function' &&
      window.matchMedia('(prefers-reduced-motion: reduce)').matches
  )
  return { azaltHareket }
}
