import { computed, onBeforeUnmount, ref, unref, watch } from 'vue'
import {
  aboneOl,
  bagli as stompBagli,
  yenidenBaglandi as stompYenidenBaglandi
} from './useWebSocket.js'
import { useDashboardOtoYenileme, OTO_YENILEME_ARALIK_MS } from './useDashboardOtoYenileme.js'

/**
 * Anasayfa canli modu.
 *
 * `/topic/dashboard/{sirketId}` aboneligini paylasilan tek STOMP baglantisi
 * uzerinden kurar. Canli mod kapaliyken abonelik ve yedek polling calismaz.
 * STOMP baglantisi kopukken (henuz kurulmamis veya yeniden baglaniyor) 60 sn
 * yedek polling devreye girer; baglanti kurulunca polling durur.
 */
export const CANLI_ANAHTAR = 'raspel_dashboard_canli'

/** Kokpit canlı yenilemesinde ardışık STOMP olaylarını birleştirme penceresi. */
export const CANLI_YENILEME_GECIKME_MS = 2000

const canliTercihiOku = () => {
  try {
    // Varsayilan aciktir; yalnizca acikca '0' yazildiysa kapali sayilir.
    return localStorage.getItem(CANLI_ANAHTAR) !== '0'
  } catch {
    return true
  }
}

export function useDashboardCanli({
  sirketId,
  yedekYenile,
  yedekAralikMs = OTO_YENILEME_ARALIK_MS
} = {}) {
  const aktif = ref(canliTercihiOku())
  const sonOlay = ref(null)
  const yenidenBaglandi = ref(0)

  let iptal = null
  let aboneSirket = null

  const sirketDegeri = () => {
    const deger = typeof sirketId === 'function' ? sirketId() : unref(sirketId)
    return deger === null || deger === undefined || deger === '' ? null : deger
  }

  const aboneKaldir = () => {
    if (!iptal) return
    iptal()
    iptal = null
    aboneSirket = null
  }

  const aboneBaslat = () => {
    const sirket = sirketDegeri()
    if (!aktif.value || !sirket) {
      aboneKaldir()
      return
    }
    if (iptal && aboneSirket === sirket) return
    aboneKaldir()
    aboneSirket = sirket
    iptal = aboneOl(`/topic/dashboard/${sirket}`, (mesaj) => {
      try {
        const veri = JSON.parse(mesaj.body)
        sonOlay.value = {
          tur: veri?.tur ?? null,
          mesaj: veri?.mesaj ?? '',
          zaman: veri?.zaman ?? new Date().toISOString()
        }
      } catch {
        // Bozuk yayin mesaji yok sayilir; baglanti etkilenmez.
      }
    })
  }

  // Canli mod veya sirket degisince abonelik tek noktadan kurulur/kaldirilir.
  watch(
    [aktif, () => sirketDegeri()],
    ([acik, sirket]) => {
      if (acik && sirket) aboneBaslat()
      else aboneKaldir()
    },
    { immediate: true }
  )

  // Paylasilan baglanti durumu; canli mod kapaliyken "bagli" sayilmaz.
  const bagli = computed(() => aktif.value && Boolean(sirketDegeri()) && stompBagli.value)

  watch(stompYenidenBaglandi, (yeni, eski) => {
    if (!aktif.value || !(yeni > eski)) return
    yenidenBaglandi.value += 1
  })

  // Yedek polling: canli mod acik ama STOMP baglantisi yoksa 60 sn.
  const yedekGerekli = computed(() => aktif.value && !stompBagli.value)
  useDashboardOtoYenileme(
    () => {
      if (typeof yedekYenile === 'function') yedekYenile()
    },
    { aralikMs: yedekAralikMs, etkin: yedekGerekli, baslangic: true }
  )

  /** Canli modu acar/kapatir ve tercihi cihaza yazar. */
  const ayarla = (deger) => {
    const yeni = deger === true
    try {
      localStorage.setItem(CANLI_ANAHTAR, yeni ? '1' : '0')
    } catch {
      /* depolama kapaliysa oturum boyunca bellekte kalir */
    }
    if (aktif.value !== yeni) aktif.value = yeni
  }

  onBeforeUnmount(aboneKaldir)

  return { aktif, bagli, sonOlay, yenidenBaglandi, ayarla, aboneBaslat, aboneKaldir }
}
