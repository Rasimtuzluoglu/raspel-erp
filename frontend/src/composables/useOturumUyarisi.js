import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

export function useOturumUyarisi() {
  const authStore = useAuthStore()
  const goster = ref(false)
  const kalanSaniye = ref(0)
  let interval = null

  const decodeExp = (token) => {
    try {
      const payload = token.split('.')[1]
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
      return decoded.exp ? decoded.exp * 1000 : null
    } catch {
      return null
    }
  }

  const baslat = () => {
    if (interval) return
    interval = setInterval(() => {
      const exp = decodeExp(authStore.token)
      if (!exp || !authStore.isLoggedIn) return
      const kalan = exp - Date.now()
      if (kalan > 0 && kalan < 2 * 60 * 1000) {
        kalanSaniye.value = Math.floor(kalan / 1000)
        goster.value = true
      }
    }, 5000)
  }

  const devamEt = async () => {
    goster.value = false
    try {
      await authStore.kullaniciGuncelle()
    } catch {
      /* empty */
    }
  }

  const cikis = () => {
    goster.value = false
    authStore.cikisYap()
    window.location.href = '/giris'
  }

  onMounted(baslat)
  onUnmounted(() => {
    if (interval) clearInterval(interval)
  })

  return { goster, kalanSaniye, devamEt, cikis }
}
