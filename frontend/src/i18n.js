import { createI18n } from 'vue-i18n'
import tr from './locales/tr.json'

// Dil seçimi localStorage'da saklanır (useLocale/LANG_KEY).
// Kayıtlı değer yoksa veya desteklenmiyorsa arayüz Türkçe açılır.
export const kayitliDil = typeof localStorage !== 'undefined' && localStorage.getItem('lang') === 'en' ? 'en' : 'tr'

const i18n = createI18n({
  legacy: false,
  locale: kayitliDil,
  fallbackLocale: 'tr',
  // Varsayilan dil (tr) senkron yuklenir; Ingilizce mesajlar kullanici dili
  // degistirdiginde tembel (lazy) olarak indirilir -> baslangic bundle'i kuculur.
  messages: { tr }
})

/**
 * Ingilizce mesajlari (gerekirse) tembel yukler ve locale'i gunceller.
 * useLocale tarafindan cagrilir.
 */
export async function ingilizceYukle() {
  if (i18n.global.getLocaleMessage('en') && i18n.global.availableLocales.includes('en')) return
  const { default: en } = await import('./locales/en.json')
  i18n.global.setLocaleMessage('en', en)
}

// Kayitli dil Ingilizce ise baslangicta yukle.
if (kayitliDil === 'en') {
  ingilizceYukle()
}

export default i18n

