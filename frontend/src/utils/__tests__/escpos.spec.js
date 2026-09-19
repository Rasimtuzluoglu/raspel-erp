import { describe, it, expect } from 'vitest'
import { escPosFisiUret } from '../escpos.js'

describe('escPosFisiUret', () => {
  it('bicimli fis uretir', () => {
    const bytes = escPosFisiUret({
      baslik: 'TEST ERP',
      tarih: '01.01.2026',
      fisNo: 'F-1',
      kalemler: [{ ad: 'Urun', adet: 2, tutar: 20 }],
      toplam: 20,
      altNot: 'Tesekkurler'
    })
    expect(bytes).toBeInstanceOf(Uint8Array)
    expect(bytes.length).toBeGreaterThan(10)

    const metin = new TextDecoder().decode(bytes)
    expect(metin).toContain('TEST ERP')
    expect(metin).toContain('Urun x2')
    expect(metin).toContain('GENEL TOPLAM: 20.00')
  })

  it('kalem tutari yoksa tutar yazmaz', () => {
    const bytes = escPosFisiUret({
      baslik: 'T',
      kalemler: [{ ad: 'Kalem', adet: 1 }]
    })
    const metin = new TextDecoder().decode(bytes)
    expect(metin).toContain('Kalem x1')
  })

  it('toplam agirligi yazar ve agir tonaji tona cevirir', () => {
    const bytes = escPosFisiUret({
      baslik: 'T',
      kalemler: [{ ad: 'Cimento', adet: 1, tutar: 100 }],
      toplam: 100,
      toplamAgirlik: 2500
    })
    const metin = new TextDecoder().decode(bytes)
    expect(metin).toContain('Toplam Ağırlık: 2.500 ton')
  })

  it('uzun urun adini 58mm genislikte sarar', () => {
    const uzunAd = 'Cok Uzun Bir Urun Adi Buraya Yazildi Ve Tasmasini Engellemeliyiz'
    const bytes = escPosFisiUret({
      baslik: 'T',
      genislik: 58,
      kalemler: [{ ad: uzunAd, adet: 1, tutar: 12 }]
    })
    const metin = new TextDecoder().decode(bytes)
    const satirlar = metin.split('\n').filter((s) => s.length > 0)
    expect(satirlar.every((s) => s.length <= 32)).toBe(true)
  })
})
