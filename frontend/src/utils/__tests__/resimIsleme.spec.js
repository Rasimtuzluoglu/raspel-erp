import { describe, it, expect } from 'vitest'
import { resimDogrula, MAKS_DOSYA_BOYUTU } from '../dosyaDogrula.js'
import { resimSikistir } from '../resimSikistir.js'

const dosya = (ad, tip, boyut) => ({ name: ad, type: tip, size: boyut })

describe('resimDogrula', () => {
  it('geçerli JPEG kabul edilir', () => {
    expect(resimDogrula(dosya('a.jpg', 'image/jpeg', 1024))).toBeNull()
  })

  it('10MB üzeri reddedilir', () => {
    const hata = resimDogrula(dosya('a.jpg', 'image/jpeg', MAKS_DOSYA_BOYUTU + 1))
    expect(hata?.key).toBe('dosya.boyutAsildi')
  })

  it('HEIC için özel mesaj döner', () => {
    const hata = resimDogrula(dosya('foto.heic', 'image/heic', 1024))
    expect(hata?.key).toBe('dosya.heicDesteklenmiyor')
  })

  it('desteklenmeyen tip reddedilir', () => {
    const hata = resimDogrula(dosya('x.bmp', 'image/bmp', 1024))
    expect(hata?.key).toBe('dosya.gecersizResimTipi')
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
