import { useAuthStore } from '../stores/authStore.js'

/**
 * v-permission="'FATURA_DELETE'" — kullanıcının yetkisi yoksa elemanı kaldırır.
 * Yetki listesi henüz yüklenmemişse (boş) gizleme yapılmaz; bu, eski davranışı korur.
 */
function yetkiVarMi(authStore, kod) {
  const liste = authStore.yetkiler || []
  if (liste.length === 0) return true
  return authStore.hasPermission(kod)
}

export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const { value } = binding

    if (value && typeof value === 'string' && !yetkiVarMi(authStore, value)) {
      el.parentNode && el.parentNode.removeChild(el)
    }
  },
  updated(el, binding) {
    const authStore = useAuthStore()
    const { value } = binding
    if (value && typeof value === 'string' && !yetkiVarMi(authStore, value) && el.parentNode) {
      el.parentNode.removeChild(el)
    }
  }
}
