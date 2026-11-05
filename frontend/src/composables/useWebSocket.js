import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

/**
 * Uygulama genelinde tek SockJS/STOMP baglantisi.
 *
 * Bildirim zili, anasayfa canli modu ve ileride eklenecek tuketiciler ayni
 * baglantiyi paylasir. Abone kayit defteri modul seviyesindedir; son abone
 * ayrilinca baglanti kapatilir. Yeniden baglanmada abonelikler `onConnect`
 * icinde yeniden kurulur ve `yenidenBaglandi` sayaci artar (kacirilan
 * olaylari telafi etmek isteyen tuketiciler bu sinyali dinler).
 */

const SOCKET_YOLU = '/ws'

/** Paylasilan baglanti durumu. */
export const bagli = ref(false)
/** Her basarili yeniden baglanmada artan sayac (ilk baglanti sayilmaz). */
export const yenidenBaglandi = ref(0)

let istemci = null
let baglantiSozu = null
let dahaOnceBaglandi = false
let aboneSayac = 1
/** id -> { id, hedef, callback, abonelik } */
const aboneler = new Map()
// Kapanis/iptal sirasinda devam eden async baglanti acilisini gecersiz kilar.
let nesil = 0

const socketAdresi = () => {
  if (import.meta.env?.VITE_WS_URL) return import.meta.env.VITE_WS_URL
  if (typeof window === 'undefined') return SOCKET_YOLU
  return window.location.origin + SOCKET_YOLU
}

const stompAbonelikKur = (kayit) => {
  if (!istemci || !istemci.connected || kayit.abonelik) return
  kayit.abonelik = istemci.subscribe(kayit.hedef, (mesaj) => {
    try {
      kayit.callback?.(mesaj)
    } catch {
      // Bir tuketici hatasi diger abonelikleri veya baglantiyi bozmaz.
    }
  })
}

const abonelikleriKur = () => {
  for (const kayit of aboneler.values()) stompAbonelikKur(kayit)
}

const abonelikleriTemizle = () => {
  for (const kayit of aboneler.values()) {
    try {
      kayit.abonelik?.unsubscribe?.()
    } catch {
      // Zaten kapanmis abonelik yok sayilir.
    }
    kayit.abonelik = null
  }
}

const baglantiAc = async () => {
  if (baglantiSozu) return baglantiSozu
  if (aboneler.size === 0) return null

  const benimNeslim = nesil
  baglantiSozu = (async () => {
    try {
      const [sockjsModul, stompModul] = await Promise.all([
        import('sockjs-client'),
        import('@stomp/stompjs')
      ])
      // Bekleme sirasinda tum aboneler ayrildiysa baglanti acilmaz.
      if (benimNeslim !== nesil || aboneler.size === 0) return null

      const SockJS = sockjsModul.default || sockjsModul
      const { Client } = stompModul
      const socket = new SockJS(socketAdresi())

      istemci = new Client({
        webSocketFactory: () => socket,
        reconnectDelay: 5000,
        onConnect: () => {
          const yeniden = dahaOnceBaglandi
          dahaOnceBaglandi = true
          bagli.value = true
          // Eski baglantiya ait abonelik nesneleri gecersizdir.
          for (const kayit of aboneler.values()) kayit.abonelik = null
          abonelikleriKur()
          if (yeniden) yenidenBaglandi.value += 1
        },
        onDisconnect: () => {
          bagli.value = false
          for (const kayit of aboneler.values()) kayit.abonelik = null
        },
        onWebSocketClose: () => {
          bagli.value = false
          for (const kayit of aboneler.values()) kayit.abonelik = null
        },
        onStompError: () => {
          bagli.value = false
        }
      })
      istemci.activate()
      return istemci
    } catch {
      istemci = null
      bagli.value = false
      return null
    } finally {
      baglantiSozu = null
    }
  })()

  return baglantiSozu
}

const baglantiKapat = () => {
  nesil += 1
  abonelikleriTemizle()
  const kapatilacak = istemci
  istemci = null
  bagli.value = false
  dahaOnceBaglandi = false
  if (kapatilacak) {
    try {
      Promise.resolve(kapatilacak.deactivate?.()).catch(() => {})
    } catch {
      // Kapatma hatasi yok sayilir; durum zaten sifirlandi.
    }
  }
}

const aboneKaldir = (id) => {
  const kayit = aboneler.get(id)
  if (!kayit) return
  try {
    kayit.abonelik?.unsubscribe?.()
  } catch {
    // Abonelik zaten kapanmis olabilir.
  }
  aboneler.delete(id)
  if (aboneler.size === 0) baglantiKapat()
}

/**
 * Bir STOMP hedefine abone olur. Baglanti yoksa tek bir baglanti acilir.
 * @param {string} hedef Örn. `/topic/dashboard/7`
 * @param {(mesaj: { body: string }) => void} callback
 * @returns {() => void} abonelik iptal fonksiyonu
 */
export function aboneOl(hedef, callback) {
  if (typeof hedef !== 'string' || hedef === '') return () => {}
  const id = aboneSayac
  aboneSayac += 1
  const kayit = { id, hedef, callback, abonelik: null }
  aboneler.set(id, kayit)

  if (istemci?.connected) {
    stompAbonelikKur(kayit)
  } else if (!istemci && !baglantiSozu) {
    baglantiAc()
  }

  return () => aboneKaldir(id)
}

/**
 * Test/oturum kapatma icin: tum abonelikleri ve baglantiyi sifirlar.
 * Uygulama akisinda kullanilmaz; abone sayaci 0 olunca zaten kapanir.
 */
export function webSocketSifirla() {
  baglantiKapat()
  aboneler.clear()
  baglantiSozu = null
  yenidenBaglandi.value = 0
}

/**
 * Geriye uyumlu composable: bildirim zilinin `/topic/bildirimler/{sirketId}`
 * aboneligini paylasilan baglanti uzerinden surdurur.
 */
export function useWebSocket() {
  const sonBildirim = ref(null)
  let iptal = null

  const baglan = () => {
    if (iptal) return
    const authStore = useAuthStore()
    if (!authStore?.isLoggedIn || !authStore?.sirketId) return
    iptal = aboneOl(`/topic/bildirimler/${authStore.sirketId}`, (mesaj) => {
      try {
        sonBildirim.value = JSON.parse(mesaj.body)
      } catch {
        // Bozuk mesaj yok sayilir; zil calismaya devam eder.
      }
    })
  }

  const baglantiKes = () => {
    if (!iptal) return
    iptal()
    iptal = null
  }

  onMounted(baglan)
  onUnmounted(baglantiKes)

  return { bagli, sonBildirim, baglan, baglantiKes }
}
