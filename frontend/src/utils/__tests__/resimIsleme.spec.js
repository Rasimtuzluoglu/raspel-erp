import { describe, it, expect } from 'vitest'
import { createI18n } from 'vue-i18n'
import { resimDogrula, boyutDogrula, MAKS_DOSYA_BOYUTU } from '../dosyaDogrula.js'
import { resimSikistir } from '../resimSikistir.js'
import tr from '../../locales/tr.json'
import en from '../../locales/en.json'

const dosya = (ad, tip, boyut) => ({ name: ad, type: tip, size: boyut })

describe('resimDogrula', () => {
  it('geçerli JPEG kabul edilir', () => {
    expect(resimDogrula(dosya('a.jpg', 'image/jpeg', 1024))).toBeNull()
  })

  it('10MB üzeri reddedilir', () => {
    const hata = resimDogrula(dosya('a.jpg', 'image/jpeg', MAKS_DOSYA_BOYUTU + 1))
    expect(hata?.key).toBe('common.dosya.boyutAsildi')
  })

  it('HEIC için özel mesaj döner', () => {
    const hata = resimDogrula(dosya('foto.heic', 'image/heic', 1024))
    expect(hata?.key).toBe('common.dosya.heicDesteklenmiyor')
  })

  it('desteklenmeyen tip reddedilir', () => {
    const hata = resimDogrula(dosya('x.bmp', 'image/bmp', 1024))
    expect(hata?.key).toBe('common.dosya.gecersizResimTipi')
  })

  it('dosya seçilmedi hatası döner', () => {
    expect(resimDogrula(null)?.key).toBe('common.dosya.dosyaSecilmedi')
  })
})

describe('boyutDogrula', () => {
  it('boyut sınırı içinde kabul edilir', () => {
    expect(boyutDogrula({ name: 'x.pdf', type: 'application/pdf', size: MAKS_DOSYA_BOYUTU })).toBeNull()
  })

  it('boyut sınırı üzerinde reddedilir', () => {
    const hata = boyutDogrula({ name: 'x.pdf', type: 'application/pdf', size: MAKS_DOSYA_BOYUTU + 1 })
    expect(hata?.key).toBe('common.dosya.boyutAsildi')
    expect(hata?.params).toEqual({ mb: 10 })
  })

  it('dosya yoksa hata döner', () => {
    expect(boyutDogrula(undefined)?.key).toBe('common.dosya.dosyaSecilmedi')
  })
})

// Bütün görünümler hata nesnesini doğrudan `t(hata.key, hata.params)` ile
// çevirir. Anahtar sözlükte yoksa kullanıcı ham anahtarı görür; bu regresyonu
// kilitlemek için üretilen her anahtarın tr ve en'de çözümlendiği doğrulanır.
describe('dosya dogrulama hata anahtarlari cevirilerde mevcut', () => {
  const hatalar = [
    resimDogrula(null),
    resimDogrula(dosya('a.jpg', 'image/jpeg', MAKS_DOSYA_BOYUTU + 1)),
    resimDogrula(dosya('foto.heic', 'image/heic', 1024)),
    resimDogrula(dosya('x.bmp', 'image/bmp', 1024)),
    boyutDogrula(undefined),
    boyutDogrula({ name: 'x.pdf', type: 'application/pdf', size: MAKS_DOSYA_BOYUTU + 1 })
  ]

  const kur = (messages, locale) =>
    createI18n({ legacy: false, locale, fallbackLocale: 'tr', messages: { [locale]: messages } })

  it.each(hatalar.map((h) => [h.key]))('%s anahtari her iki dilde cozumlenir', (key) => {
    for (const [locale, messages] of [['tr', tr], ['en', en]]) {
      const metin = kur(messages, locale).global.t(key, { mb: 10 })
      expect(metin).not.toBe(key)
      expect(metin).toBeTruthy()
      expect(metin).not.toContain('common.dosya')
    }
  })
})

describe('resimSikistir', () => {
  it('resim olmayan dosya olduğu gibi döner', async () => {
    const pdf = dosya('belge.pdf', 'application/pdf', 1024)
    expect(await resimSikistir(pdf)).toBe(pdf)
  })

  it('gif ve png yeniden kodlanmaz', async () => {
    const gif = dosya('a.gif', 'image/gif', 1024)
    const png = dosya('a.png', 'image/png', 1024)
    expect(await resimSikistir(gif)).toBe(gif)
    expect(await resimSikistir(png)).toBe(png)
  })

  it('createImageBitmap yoksa dosya olduğu gibi döner (güvenli geri düşüş)', async () => {
    const jpg = dosya('a.jpg', 'image/jpeg', 5 * 1024 * 1024)
    const orijinal = globalThis.createImageBitmap
    // Bazı ortamlarda (jsdom) canvas API'si yoktur; fonksiyon dosyayı değiştirmemeli.
    globalThis.createImageBitmap = undefined
    try {
      expect(await resimSikistir(jpg)).toBe(jpg)
    } finally {
      globalThis.createImageBitmap = orijinal
    }
  })
})
