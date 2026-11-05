import { onBeforeUnmount, onMounted, ref, watch } from 'vue'

/** Vitest/jsdom: gozlemci hicbir zaman tetiklenmez, bu yuzden hemen gorunur sayilir. */
const testOrtami = () => {
  try {
    const env = import.meta.env
    return Boolean(env && (env.MODE === 'test' || env.VITEST))
  } catch {
    return false
  }
}

const gozlemciDestekleniyor = () =>
  typeof window !== 'undefined' && typeof window.IntersectionObserver === 'function'

/**
 * Bir DOM elemaninin gorunur alana girip girmedigini izler.
 *
 * Agir bilesenleri (ornegin Chart.js canvas'lari) ilk yukte degil, kart
 * ekrana yaklasinca mount etmek icin kullanilir. `rootMargin` sayesinde
 * eleman gercekten gorunmeden 200px once hazirlanir; kullanici kaydirirken
 * bos alan gormez.
 *
 * Gozlemci yoksa (SSR, jsdom, eski tarayici) eleman hemen gorunur kabul
 * edilir; davranis geriye donuk uyumludur.
 *
 * @param {import('vue').Ref<HTMLElement|null>} hedefRef izlenecek eleman
 * @param {{ rootMargin?: string, threshold?: number }} [secenekler]
 * @returns {{ gorunur: import('vue').Ref<boolean> }}
 */
export function useGorunurluk(hedefRef, secenekler = {}) {
  const { rootMargin = '200px', threshold = 0 } = secenekler
  const hemenGorunur = !gozlemciDestekleniyor() || testOrtami()
  const gorunur = ref(hemenGorunur)

  let gozlemci = null
  let birakildi = false
  let yazdirDinleyicisi = null

  const gozlemciyiBirak = () => {
    if (!gozlemci) return
    gozlemci.disconnect()
    gozlemci = null
  }

  const gorunurYap = () => {
    gorunur.value = true
    // Tek yonlu gecis: bir kez gorundukten sonra izlemeye gerek yok.
    gozlemciyiBirak()
  }

  /**
   * Hedef eleman henuz DOM'da olmayabilir: anasayfa verisi sonradan gelir ve
   * grafik alani ancak o zaman render edilir. Eleman yoksa "gorunur" sonucuna
   * varmak yerine ref'i izleriz; eleman olustugu anda gercek konumuna bakilir.
   */
  const izlemeyeBasla = () => {
    if (gorunur.value || birakildi) return
    if (!gozlemciDestekleniyor()) {
      gorunurYap()
      return
    }
    if (gozlemci) return // zaten izleniyor
    const eleman = hedefRef.value
    if (!eleman) return
    try {
      // Ilk karede gorunen kartlar gozlemci geri cagrisini beklemesin.
      const kutu = typeof eleman.getBoundingClientRect === 'function'
        ? eleman.getBoundingClientRect()
        : null
      const pencereYuksekligi = typeof window !== 'undefined' ? window.innerHeight || 0 : 0
      if (kutu && kutu.top <= pencereYuksekligi + 200 && kutu.bottom >= -200) {
        gorunurYap()
        return
      }
      gozlemci = new window.IntersectionObserver(
        (girdiler) => {
          if (girdiler.some((girdi) => girdi.isIntersecting)) gorunurYap()
        },
        { rootMargin, threshold }
      )
      gozlemci.observe(eleman)
    } catch {
      gorunurYap()
    }
  }

  onMounted(() => {
    // Yazdirma/PDF oncesi tum grafikler hazir olsun: kaydirilmamis kartlardaki
    // iskeletler basilan belgeye girmesin.
    if (typeof window !== 'undefined' && typeof window.addEventListener === 'function') {
      yazdirDinleyicisi = () => gorunurYap()
      window.addEventListener('beforeprint', yazdirDinleyicisi)
    }
    izlemeyeBasla()
  })

  // Veri sonradan gelip grafik alani olusursa izlemeyi o zaman baslat.
  watch(hedefRef, () => izlemeyeBasla())

  onBeforeUnmount(() => {
    birakildi = true
    if (yazdirDinleyicisi && typeof window !== 'undefined') {
      window.removeEventListener('beforeprint', yazdirDinleyicisi)
      yazdirDinleyicisi = null
    }
    gozlemciyiBirak()
  })

  return { gorunur }
}

export default useGorunurluk
