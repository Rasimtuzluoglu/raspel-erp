import { ref, watch, onUnmounted } from 'vue'

/**
 * Bir sayısal ref'i hedef değere doğru yumuşak biçimde sayar (count-up).
 * `prefers-reduced-motion` tercihinde animasyon atlanır.
 */
export function useSayac(hedef, { sure = 800 } = {}) {
  const gosterilen = ref(0)
  let rafId = null

  const azHareket = () =>
    typeof window !== 'undefined' &&
    typeof window.matchMedia === 'function' &&
    window.matchMedia('(prefers-reduced-motion: reduce)').matches

  const animasyon = (bitis) => {
    if (rafId) cancelAnimationFrame(rafId)
    const baslangic = gosterilen.value
    if (azHareket() || sure <= 0) {
      gosterilen.value = bitis
      return
    }
    const t0 = performance.now()
    const adim = (now) => {
      const p = Math.min(1, (now - t0) / sure)
      const eased = 1 - Math.pow(1 - p, 3)
      gosterilen.value = baslangic + (bitis - baslangic) * eased
      if (p < 1) rafId = requestAnimationFrame(adim)
      else gosterilen.value = bitis
    }
    rafId = requestAnimationFrame(adim)
  }

  watch(
    hedef,
    (yeni) => animasyon(Number(yeni) || 0),
    { immediate: true }
  )

  onUnmounted(() => {
    if (rafId) cancelAnimationFrame(rafId)
  })

  return { gosterilen }
}
