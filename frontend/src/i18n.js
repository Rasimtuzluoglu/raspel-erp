import { createI18n } from 'vue-i18n'
import tr from './locales/tr.json'
import en from './locales/en.json'

// Dil seçimi localStorage'da saklanır (useLocale/LANG_KEY).
// Kayıtlı değer yoksa veya desteklenmiyorsa arayüz Türkçe açılır.
const kayitliDil = typeof localStorage !== 'undefined' && localStorage.getItem('lang') === 'en' ? 'en' : 'tr'

export default createI18n({
  legacy: false,
  locale: kayitliDil,
  fallbackLocale: 'tr',
  messages: { tr, en }
})
