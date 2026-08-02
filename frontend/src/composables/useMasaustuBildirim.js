import { ref } from 'vue'

export function useMasaustuBildirim() {
  const izinli = ref('Notification' in window && Notification.permission === 'granted')
  const istekYapildi = ref(false)

  const izinIste = async () => {
    if (!('Notification' in window)) return false
    if (Notification.permission === 'granted') {
      izinli.value = true
      return true
    }
    if (Notification.permission === 'denied') return false
    istekYapildi.value = true
    try {
      const sonuc = await Notification.requestPermission()
      izinli.value = sonuc === 'granted'
      return izinli.value
    } catch {
      return false
    }
  }

  const goster = (baslik, mesaj, ikon = '/icon-192.png') => {
    if (!izinli.value) return false
    try {
      const not = new Notification(baslik, {
        body: mesaj,
        icon: ikon,
        tag: 'raspel-' + Date.now()
      })
      not.onclick = () => { window.focus(); not.close() }
      return true
    } catch {
      return false
    }
  }

  return { izinli, istekYapildi, izinIste, goster }
}
