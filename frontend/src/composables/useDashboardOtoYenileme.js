import { computed, onBeforeUnmount, ref, unref, watch } from 'vue'

/**
 * Anasayfa yedek yenileme zamanlayicisi.
 *
 * Davranis sozlesmesi:
 *  - Tercih `localStorage`ta tutulur (`raspel_dashboard_oto` = '1' | '0').
 *  - Acikken `aralikMs` (varsayilan 60 sn) boyunca bir kez `yenile()` cagrilir.
 *  - Yalnizca sekme gorunurken yenilenir; arka plandaki sekme istek uretmez.
 *  - `etkin` verilirse zamanlayici yalnizca `etkin === true` iken calisir.
 *    Canli STOMP baglantisinda polling'i durdurmak icin kullanilir.
 *  - `baslangic: true` verilirse cihaz tercihi okunmaz (canli modun yedek
 *    katmani her zaman aktiftir).
 *  - Bilesen kaldirilinca zamanlayici temizlenir (sizinti yok).
 */
export const OTO_YENILEME_ANAHTAR = 'raspel_dashboard_oto'
export const OTO_YENILEME_ARALIK_MS = 60_000

export function useDashboardOtoYenileme(
  yenile,
  { aralikMs = OTO_YENILEME_ARALIK_MS, etkin, baslangic } = {}
) {
  const oku = () => {
    try {
      return localStorage.getItem(OTO_YENILEME_ANAHTAR) === '1'
    } catch {
      return false
    }
  }

  const aktif = ref(baslangic === true ? true : oku())
  const etkinMi = computed(() => {
    if (typeof etkin === 'undefined') return true
    return Boolean(typeof etkin === 'function' ? etkin() : unref(etkin))
  })
  let zamanlayici = null

  const durdur = () => {
    if (zamanlayici !== null) {
      clearInterval(zamanlayici)
      zamanlayici = null
    }
  }

  const baslat = () => {
    durdur()
    if (!etkinMi.value) return
    zamanlayici = setInterval(() => {
      if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
      if (typeof yenile === 'function') yenile()
    }, aralikMs)
  }

  /** Anahtari degistirir ve tercihi cihaza yazar. Zamanlayiciyi watch yonetir. */
  const ayarla = (deger) => {
    const yeni = deger === true
    try {
      localStorage.setItem(OTO_YENILEME_ANAHTAR, yeni ? '1' : '0')
    } catch {
      /* depolama kapaliysa oturum boyunca bellekte kalir */
    }
    if (aktif.value === yeni) return
    aktif.value = yeni
  }

  // `aktif` veya `etkin` degisince zamanlayici tek noktadan kurulur/kaldirilir;
  // cift zamanlayici olusmaz.
  watch(aktif, (deger) => {
    if (deger) baslat()
    else durdur()
  })

  watch(etkinMi, () => {
    if (aktif.value) baslat()
    else durdur()
  })

  if (aktif.value) baslat()

  onBeforeUnmount(durdur)

  return { aktif, ayarla, baslat, durdur, aralikMs, etkin: etkinMi }
}
