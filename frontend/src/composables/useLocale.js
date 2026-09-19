import { useI18n } from 'vue-i18n'
import { computed } from 'vue'
import { usePrimeVue } from 'primevue/config'
import { pvTr, pvEn } from '../utils/primevueLocales.js'
import { ingilizceYukle } from '../i18n.js'

const LANG_KEY = 'lang'

/**
 * Uygulama dilini (tr/en) yöneten composable.
 * Seçimi localStorage'a kaydeder, vue-i18n locale'ini ve PrimeVue yerleşik
 * bileşenlerinin (DatePicker, DataTable filtreleri...) dilini günceller.
 */
export function useLocale() {
  const { locale } = useI18n()

  const aktifDil = computed(() => locale.value)

  const dilDegistir = async (dil) => {
    const hedef = dil === 'en' ? 'en' : 'tr'
    // Ingilizce mesajlari once yukle (fallback 'tr' oldugu icin yukleme
    // tamamlanana kadar arayuz Turkce metinlerle tutarli kalir).
    if (hedef === 'en') {
      try {
        await ingilizceYukle()
      } catch {
        /* mesajlar yuklenemezse tr'de kal */
      }
    }
    locale.value = hedef
    localStorage.setItem(LANG_KEY, hedef)
    if (typeof document !== 'undefined') document.documentElement.setAttribute('lang', hedef)
    try {
      const primevue = usePrimeVue()
      if (primevue?.config) primevue.config.locale = hedef === 'en' ? pvEn : pvTr
    } catch {
      /* PrimeVue kurulu değil (birim test) - yoksay */
    }
  }

  return { aktifDil, dilDegistir }
}
