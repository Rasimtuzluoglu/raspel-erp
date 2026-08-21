import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

export function useOturumUyarisi() {
  const authStore = useAuthStore()
  const goster = ref(false)
  const kalanSaniye = ref(0)
  let interval = null

  let cachedExp = null
  let lastToken = ''

  const getExp = (token) => {
    if (!token) return null
    if (token === lastToken && cachedExp !== null) return cachedExp
    try {
      lastToken = token
      const payload = token.split('.')[1]
      if (!payload) return null
      const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
      cachedExp = decoded.exp ? decoded.exp * 1000 : null
      return cachedExp
    } catch {
      cachedExp = null
      return null
    }
  }

  const baslat = () => {
    if (interval) return
    interval = setInterval(() => {
      if (!authStore?.isLoggedIn || !authStore?.token) {
        if (goster.value) goster.value = false
        return
      }
      const exp = getExp(authStore.token)
      if (!exp) return
      const kalan = exp - Date.now()
      if (kalan > 0 && kalan < 2 * 60 * 1000) {
        kalanSaniye.value = Math.floor(kalan / 1000)
        goster.value = true
      } else if (kalan <= 0) {
        goster.value = false
      }
    }, 10000)
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
