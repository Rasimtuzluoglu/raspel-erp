import { computed } from 'vue'
import { useAuthStore } from '../stores/authStore.js'

// Urun (RasPel) marka gorselleri; sirket logosu yoksa bunlara dusulur.
export const URUN_LOGO_ICON = '/logo-icon.png'
export const URUN_LOGO_FULL = '/logo-full.png'
export const URUN_ADI = 'RasPel'

/**
 * Kurumsal marka (beyaz etiket) tek kaynagi.
 * Sirket logosu varsa onu, yoksa RasPel urun markasini doner. Tum bilesenler
 * logoyu buradan okur; tekrar tekrar sirketAPI cagrisi yapilmaz.
 */
export function useMarka() {
  const authStore = useAuthStore()

  const sirketLogosu = computed(() => authStore.sirketLogo || '')
  const logoVar = computed(() => !!sirketLogosu.value)
  const logoIcon = computed(() => sirketLogosu.value || URUN_LOGO_ICON)
  const logoFull = computed(() => sirketLogosu.value || URUN_LOGO_FULL)
  const markaAdi = computed(() => authStore.sirketAdi || URUN_ADI)

  const yukle = () => authStore.sirketLogosunuYukle()
  const ayarla = (url) => authStore.sirketLogosunuAyarla(url)

  return { sirketLogosu, logoVar, logoIcon, logoFull, markaAdi, yukle, ayarla, URUN_LOGO_ICON, URUN_LOGO_FULL, URUN_ADI }
}
