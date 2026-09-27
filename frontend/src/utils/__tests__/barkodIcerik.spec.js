import { describe, it, expect } from 'vitest'
import { asciiSadelestir, etiketGosterim, barkodIcerik } from '../barkodIcerik.js'

describe('asciiSadelestir', () => {
  it('Turkce karakterleri translitere eder', () => {
    expect(asciiSadelestir('ÜRÜN-ŞİŞE 5L')).toBe('URUN-SISE 5L')
    expect(asciiSadelestir('Iğdır Çağrı')).toBe('Igdir Cagri')
  })

  it('ASCII disi kalan karakterleri atar', () => {
    expect(asciiSadelestir('A€B C')).toBe('AB C')
  })
})

describe('etiketGosterim', () => {
  it('barkod oncelikli', () => {
    expect(etiketGosterim({ id: 7, barkod: '8690002', stokKodu: 'PVC-B01' })).toBe('8690002')
  })

  it('barkod yoksa stok kodu', () => {
    expect(etiketGosterim({ id: 7, barkod: '', stokKodu: 'MDF-18' })).toBe('MDF-18')
    expect(etiketGosterim({ id: 7, stokKodu: 'MDF-18' })).toBe('MDF-18')
  })

  it('ikisi de yoksa STK{id}', () => {
    expect(etiketGosterim({ id: 7 })).toBe('STK7')
  })

  it('bos stokta bos metin', () => {
    expect(etiketGosterim(null)).toBe('')
  })
})

describe('barkodIcerik', () => {
  it('Türkçe barkodu ASCII olarak kodlar', () => {
    expect(barkodIcerik({ id: 7, barkod: 'ÜRÜN-1' })).toBe('URUN-1')
  })

  it('barkodsuz urunde stok kodunu kodlar', () => {
    expect(barkodIcerik({ id: 7, stokKodu: 'MDF-18' })).toBe('MDF-18')
  })

  it('tamamen bos icerikte STK{id} dondurur', () => {
    expect(barkodIcerik({ id: 7, barkod: '€€' })).toBe('STK7')
  })
})
