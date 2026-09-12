import { ref, computed } from 'vue'
import { pushAPI } from '../api/index.js'

const izin = ref(typeof Notification !== 'undefined' ? Notification.permission : 'default')
const aktif = ref(false)
const destek = typeof window !== 'undefined'
  && 'serviceWorker' in navigator
  && 'PushManager' in window
  && 'Notification' in window

const urlBase64ToUint8Array = (base64String) => {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4)
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/')
  const raw = window.atob(base64)
  const output = new Uint8Array(raw.length)
  for (let i = 0; i < raw.length; ++i) output[i] = raw.charCodeAt(i)
  return output
}

export function usePushBildirim() {
  const izinli = computed(() => izin.value === 'granted')

  const aboneOl = async () => {
    if (!destek) return false
    try {
      const cfg = await pushAPI.vapidKey()
      if (!cfg.data?.aktif || !cfg.data?.publicKey) return false
      const reg = await navigator.serviceWorker.ready
      let sub = await reg.pushManager.getSubscription()
      if (!sub) {
        sub = await reg.pushManager.subscribe({
          userVisibleOnly: true,
          applicationServerKey: urlBase64ToUint8Array(cfg.data.publicKey)
        })
      }
      await pushAPI.abone(sub.toJSON())
      aktif.value = true
      return true
    } catch {
      aktif.value = false
      return false
    }
  }

  const izinIste = async () => {
    if (!destek) return 'unsupported'
    const sonuc = await Notification.requestPermission()
    izin.value = sonuc
    if (sonuc === 'granted') await aboneOl()
    return sonuc
  }

  const aboneKaldir = async () => {
    try {
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.getSubscription()
      if (sub) {
        await pushAPI.aboneSil(sub.endpoint)
        await sub.unsubscribe()
      }
      aktif.value = false
      return true
    } catch {
      return false
    }
  }

  const durumYenile = async () => {
    if (!destek) return
    izin.value = Notification.permission
    try {
      const reg = await navigator.serviceWorker.ready
      const sub = await reg.pushManager.getSubscription()
      aktif.value = !!sub
      if (izin.value === 'granted' && !sub) {
        await aboneOl()
      }
    } catch {
      aktif.value = false
    }
  }

  return { destek, izin, izinli, aktif, izinIste, aboneOl, aboneKaldir, durumYenile }
}
