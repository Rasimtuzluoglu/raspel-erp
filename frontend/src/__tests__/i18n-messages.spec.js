import { describe, it, expect } from 'vitest'
import { createI18n } from 'vue-i18n'
import tr from '../locales/tr.json'
import en from '../locales/en.json'

const kur = (messages, locale) =>
  createI18n({ legacy: false, locale, fallbackLocale: 'tr', messages: { [locale]: messages } })

// '@' iceren mesajlar vue-i18n'de linked-message olarak yorumlanip
// "Invalid linked format" hatasi firlatabiliyordu; literal '@' icin {'@'} gerekir.
// Bu testler o sozdizimi regresyonunu kilitler.
describe('i18n mesaj sozdizimi (email yer tutuculari)', () => {
  const anahtarlar = ['kullanicilar.emailPlaceholder', 'raporlar.epostaYerTutucu']

  it.each(anahtarlar)('tr: %s hatasiz derlenir ve @ icerir', (k) => {
    const i18n = kur(tr, 'tr')
    let metin
    expect(() => {
      metin = i18n.global.t(k)
    }).not.toThrow()
    expect(metin).toContain('@')
    expect(metin).not.toContain("{'@'}")
  })

  it.each(anahtarlar)('en: %s hatasiz derlenir ve @ icerir', (k) => {
    const i18n = kur(en, 'en')
    let metin
    expect(() => {
      metin = i18n.global.t(k)
    }).not.toThrow()
    expect(metin).toContain('@')
    expect(metin).not.toContain("{'@'}")
  })
})
