import { describe, it, expect } from 'vitest'
import { komutCoz, miktarNormalize, KOMUT_TIP } from '../usePosKomut.js'

describe('usePosKomut - komutCoz', () => {
  it('bos/whitespace girdi -> BOS', () => {
    expect(komutCoz('').tip).toBe(KOMUT_TIP.BOS)
    expect(komutCoz('   ').tip).toBe(KOMUT_TIP.BOS)
    expect(komutCoz(null).tip).toBe(KOMUT_TIP.BOS)
    expect(komutCoz(undefined).tip).toBe(KOMUT_TIP.BOS)
  })

  it('duz metin -> urun aramasi, miktar YOK', () => {
    const k = komutCoz('  vida  ')
    expect(k).toEqual({ tip: KOMUT_TIP.URUN, metin: 'vida' })
    expect(k.miktar).toBeUndefined()
  })

  it('sadece sayi (barkod) -> urun aramasi', () => {
    expect(komutCoz('1234567890')).toEqual({ tip: KOMUT_TIP.URUN, metin: '1234567890' })
  })

  it('@ ile musteri komutu', () => {
    expect(komutCoz('@ali veli')).toEqual({ tip: KOMUT_TIP.MUSTERI, metin: 'ali veli' })
  })

  it('@ bos ise BOS', () => {
    expect(komutCoz('@').tip).toBe(KOMUT_TIP.BOS)
    expect(komutCoz('@   ').tip).toBe(KOMUT_TIP.BOS)
  })

  it('onek miktar: 3*urun', () => {
    expect(komutCoz('3*vida')).toEqual({ tip: KOMUT_TIP.URUN, metin: 'vida', miktar: 3 })
  })

  it('onek miktar: 3x urun (bosluklu)', () => {
    expect(komutCoz('3x vida')).toEqual({ tip: KOMUT_TIP.URUN, metin: 'vida', miktar: 3 })
  })

  it('sonek miktar: urun*3', () => {
    expect(komutCoz('vida*3')).toEqual({ tip: KOMUT_TIP.URUN, metin: 'vida', miktar: 3 })
  })

  it('sonek miktar: urun x 3', () => {
    expect(komutCoz('vida x 3')).toEqual({ tip: KOMUT_TIP.URUN, metin: 'vida', miktar: 3 })
  })

  it('onek, sonekten ONCE denenir (belirsizlik cozumu)', () => {
    // "3*4" -> miktar 3, urun "4"
    expect(komutCoz('3*4')).toEqual({ tip: KOMUT_TIP.URUN, metin: '4', miktar: 3 })
  })

  it('miktar sinirlanir (1..9999) ve tam sayiya cevrilir', () => {
    expect(komutCoz('0*vida').miktar).toBe(1)
    expect(komutCoz('99999*vida').miktar).toBe(9999)
    expect(miktarNormalize(2.9)).toBe(2)
    expect(miktarNormalize(-5)).toBe(1)
    expect(miktarNormalize('abc')).toBe(1)
  })

  it('yalnizca yildiz -> duz arama (miktar yok)', () => {
    // "*" tek basina anlamli bir komut degil.
    expect(komutCoz('*')).toEqual({ tip: KOMUT_TIP.URUN, metin: '*' })
  })
})
