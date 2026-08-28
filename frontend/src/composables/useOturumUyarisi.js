import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

export function useOturumUyarisi() {
  const authStore = useAuthStore()
  const goster = ref(false)
  const kalanSaniye = ref(0)
  let interval = null

  const getExp = () => {
    // JWT httpOnly cookie'de olduğu için exp, login yanıtındaki tokenExpiresAt'tan gelir
    const exp = authStore?.tokenExpiresAt
    if (exp) return exp
    try {
      const kayitli = JSON.parse(localStorage.getItem('raspel_erp_auth') || '{}')
      return kayitli.tokenExpiresAt || null
    } catch {
      return null
    }
  }

  const baslat = () => {
    if (interval) return
    interval = setInterval(() => {
      if (!authStore?.isLoggedIn) {
        if (goster.value) goster.value = false
        return
      }
      const exp = getExp()
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
