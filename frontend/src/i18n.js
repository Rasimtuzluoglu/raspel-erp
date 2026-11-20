import { createI18n } from 'vue-i18n'

// Dil seçimi localStorage'da saklanır (useLocale/LANG_KEY).
// Kayıtlı değer yoksa veya desteklenmiyorsa arayüz Türkçe açılır.
export const kayitliDil = typeof localStorage !== 'undefined' && localStorage.getItem('lang') === 'en' ? 'en' : 'tr'

/**
 * Mesajlar tembel yüklenir: tr.json/en.json ~300 KB olduğu için ana pakete
 * gömülmez. `dilleriYukle()` uygulama mount edilmeden önce çağrılır
 * (main.js); testlerde `test-setup.js` mesajları senkron yükler.
 */
const i18n = createI18n({
  legacy: false,
  locale: kayitliDil,
  fallbackLocale: 'tr',
  messages: {}
})

let yuklemeSozu = null

/** İngilizce mesajları (gerekirse) tembel yükler ve locale'i günceller. */
export async function ingilizceYukle() {
  if (i18n.global.availableLocales.includes('en')) {
    if (i18n.global.locale.value !== 'en') i18n.global.locale.value = 'en'
    return
  }
  const { default: en } = await import('./locales/en.json')
  i18n.global.setLocaleMessage('en', en)
  if (kayitliDil === 'en') i18n.global.locale.value = 'en'
}

/** Başlangıç dilini (ve gerekiyorsa İngilizceyi) bir kez yükler. */
export async function dilleriYukle() {
  if (yuklemeSozu) return yuklemeSozu
  yuklemeSozu = (async () => {
    const { default: tr } = await import('./locales/tr.json')
    i18n.global.setLocaleMessage('tr', tr)
    if (kayitliDil === 'en') await ingilizceYukle()
    return i18n
  })().catch((hata) => {
    // Yükleme başarısız olursa tekrar denenebilsin; uygulama yine açılır.
    yuklemeSozu = null
    throw hata
  })
  return yuklemeSozu
}

export default i18n
