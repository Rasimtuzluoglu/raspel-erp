import { createI18n } from 'vue-i18n'
import tr from './locales/tr.json'
import en from './locales/en.json'

const savedLang = localStorage.getItem('lang') || 'tr'

export default createI18n({
  legacy: false,
  locale: savedLang,
  fallbackLocale: 'tr',
  messages: { tr, en }
})
