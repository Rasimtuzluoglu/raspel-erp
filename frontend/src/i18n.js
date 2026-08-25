import { createI18n } from 'vue-i18n'
import tr from './locales/tr.json'
import en from './locales/en.json'

// İngilizce çeviriler tamamlanana kadar arayüz zorunlu olarak Türkçe kalır.
// (Dil seçici ThemeSwitcher'dan kaldırıldı; localStorage'daki eski 'lang' değerleri yok sayılır.)
export default createI18n({
  legacy: false,
  locale: 'tr',
  fallbackLocale: 'tr',
  messages: { tr, en }
})
