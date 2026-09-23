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

// Düz '|' vue-i18n'de plural ayiricidir; metnin ilk yarisindan sonrasi kaybolur.
// Bu testler '|' kacisini/degisimini kilitler.
describe('i18n mesaj sozdizimi (literal pipe)', () => {
  const paramli = ['quickSearch.vergiBakiye', 'quickSearch.kodMiktar']
  const paramlar = { vergi: 'V', bakiye: 'B', kod: 'K', miktar: '1', birim: 'Adet' }

  it.each(paramli)('tr: %s pipe icermez ve tam metni dondurur', (k) => {
    const metin = kur(tr, 'tr').global.t(k, paramlar)
    expect(metin).not.toContain('|')
    expect(metin).toContain('·')
  })

  it.each(paramli)('en: %s pipe icermez ve tam metni dondurur', (k) => {
    const metin = kur(en, 'en').global.t(k, paramlar)
    expect(metin).not.toContain('|')
    expect(metin).toContain('·')
  })

  it('sonGuncelleme metinleri pipe icermez (tr/en)', () => {
    const trMetin = kur(tr, 'tr').global.t('kullanimSartlari.sonGuncelleme')
    const enMetin = kur(en, 'en').global.t('kullanimSartlari.sonGuncelleme')
    expect(trMetin).not.toContain('|')
    expect(enMetin).not.toContain('|')
  })
})
