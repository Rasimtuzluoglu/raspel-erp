import { useI18n } from 'vue-i18n'
import { computed } from 'vue'

const LANG_KEY = 'lang'

/**
 * Uygulama dilini (tr/en) yöneten composable.
 * Seçimi localStorage'a kaydeder ve vue-i18n locale'ini günceller.
 */
export function useLocale() {
  const { locale } = useI18n()

  const aktifDil = computed(() => locale.value)

  const dilDegistir = (dil) => {
    const hedef = dil === 'en' ? 'en' : 'tr'
    locale.value = hedef
    localStorage.setItem(LANG_KEY, hedef)
    document.documentElement.setAttribute('lang', hedef)
  }

  return { aktifDil, dilDegistir }
}
