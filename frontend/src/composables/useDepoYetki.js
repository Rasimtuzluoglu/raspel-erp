import { computed } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

/**
 * Depo islem yetkisi: ADMIN veya depo sorumlusu.
 * Sorumlu atanmamissa islem yalniz ADMIN'dedir; goruntuleme kisitlanmaz.
 */
export function useDepoYetki() {
  const authStore = useAuthStore()
  const adminMi = computed(() => authStore.isAdmin)
  const kullaniciId = computed(() => authStore.kullanici?.id ?? null)

  const islemYapabilir = (depo) => {
    if (adminMi.value) return true
    if (!depo || depo.sorumluKullaniciId == null) return false
    return depo.sorumluKullaniciId === kullaniciId.value
  }

  return { adminMi, kullaniciId, islemYapabilir }
}
