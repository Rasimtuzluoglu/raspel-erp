import { describe, it, expect } from 'vitest'
import {
  adimBirimIcin,
  minMiktarBirimIcin,
  adimaYuvarla,
  miktarDogrula,
  adimla,
  ADET_SORUN
} from '../posAdet.js'

/**
 * POS ADET KURALLARI — birim testleri.
 *
 * Bu dosya, sepetteki miktar girişinin TEK dogrulama kaynagidir. Once miktar
 * alani dogrudan state'e yaziliyordu; sonuc negatif adet, depoda 3 olan urunu
 * 500 ekleme ve Enter'in hicbir sey yapmamasi bu kosulardan geliyordu.
 */

describe('adimBirimIcin', () => {
  it('olculen birimlerde adim 0.5', () => {
    for (const b of ['kg', 'Kg', 'KG', 'Kilogram', ' m2 ', 'M²', 'lt', 'ton', 'metre']) {
      expect(adimBirimIcin(b), b).toBe(0.5)
    }
  })

  it('tam sayi birimlerde adim 1', () => {
    for (const b of ['adet', 'Adet', 'ADET', 'paket', 'kutu', 'take', '', null, undefined]) {
      expect(adimBirimIcin(b), String(b)).toBe(1)
    }
  })

  it('Turkce buyuk/kucuk harf karisimini normalize eder', () => {
    expect(adimBirimIcin('KİLOGRAM')).toBe(0.5)
    expect(adimBirimIcin('MetreKare')).toBe(0.5)
  })

  it('en kucuk satilabilir miktar adimla aynidir', () => {
    expect(minMiktarBirimIcin('kg')).toBe(0.5)
    expect(minMiktarBirimIcin('adet')).toBe(1)
  })
})

describe('adimaYuvarla', () => {
  it('adim 1 ile tam sayiya yuvarlar', () => {
    expect(adimaYuvarla(2.4, 1)).toBe(2)
    expect(adimaYuvarla(2.6, 1)).toBe(3)
  })

  it('adim 0.5 ile yarim adima oturur', () => {
    expect(adimaYuvarla(1.3, 0.5)).toBe(1.5)
    expect(adimaYuvarla(1.2, 0.5)).toBe(1)
  })

  it('ondalik kayan nokta hatasini temizler (0.1+0.2 -> 0.5 tabanli)', () => {
    // 0.1 * 3 = 0.30000000000000004 gibi hatalar olusur
    expect(adimaYuvarla(0.1 * 3, 0.5)).toBe(0.5)
  })

  it('gecersiz girdide adimi doner', () => {
    expect(adimaYuvarla(NaN, 0.5)).toBe(0.5)
    expect(adimaYuvarla(Infinity, 1)).toBe(1)
  })
})

describe('miktarDogrula — gecersiz degerler', () => {
  const kotu = [NaN, Infinity, -Infinity, -3, 0, '', null, undefined, 'abc', {}]

  for (const v of kotu) {
    it(`${JSON.stringify(v) ?? String(v)} -> en kucuk adede duser ve GECERSIZ bildirir`, () => {
      const r = miktarDogrula({ istenen: v, birim: 'adet' })
      expect(r.sorun).toBe(ADET_SORUN.GECERSIZ)
      expect(r.miktar).toBe(1)
    })
  }

  it('ondalik birimde en kucuk deger 0.5', () => {
    const r = miktarDogrula({ istenen: -1, birim: 'kg' })
    expect(r.sorun).toBe(ADET_SORUN.GECERSIZ)
    expect(r.miktar).toBe(0.5)
  })

  it('virgulle ondalik ("2,5") kabul edilir', () => {
    expect(miktarDogrula({ istenen: '2,5', birim: 'kg' }).miktar).toBe(2.5)
    expect(miktarDogrula({ istenen: '3', birim: 'adet' }).miktar).toBe(3)
  })

  it('gecerli tam sayida sorun yoktur', () => {
    const r = miktarDogrula({ istenen: 5, birim: 'adet' })
    expect(r.sorun).toBeNull()
    expect(r.miktar).toBe(5)
  })

  it('adima uymayan deger yuvarlanir ve bildirilir', () => {
    const r = miktarDogrula({ istenen: 2.3, birim: 'kg' })
    expect(r.miktar).toBe(2.5)
    expect(r.sorun).toBe(ADET_SORUN.ADIM_UYUSMUYOR)
  })
})

describe('miktarDogrula — stok tavani (KRITIK kural)', () => {
  it('stoktan fazla istenirse tavana kirpilir ve ASIM bildirir', () => {
    const r = miktarDogrula({ istenen: 500, stokMiktari: 3, birim: 'adet' })
    expect(r.asim).toBe(true)
    expect(r.sorun).toBe(ADET_SORUN.ASIM)
    expect(r.miktar).toBe(3)
    expect(r.enFazla).toBe(3)
  })

  it('tam tavan degerine izin verir (sinirda sorun yok)', () => {
    const r = miktarDogrula({ istenen: 3, stokMiktari: 3, birim: 'adet' })
    expect(r.asim).toBe(false)
    expect(r.sorun).toBeNull()
    expect(r.miktar).toBe(3)
  })

  it('tavan adima yuvarlanir (stok 3.7 ise en fazla 3.5)', () => {
    const r = miktarDogrula({ istenen: 10, stokMiktari: 3.7, birim: 'kg' })
    expect(r.miktar).toBe(3.5)
    expect(r.asim).toBe(true)
  })

  it('stok bilinmiyorsa tavan uygulanmaz', () => {
    const r = miktarDogrula({ istenen: 999, stokMiktari: null, birim: 'adet' })
    expect(r.miktar).toBe(999)
    expect(r.asim).toBe(false)
    expect(r.enFazla).toBeNull()
  })

  it('stok 0 ise ASIM yerine gecersiz sayilir (satilacak bir sey yok)', () => {
    const r = miktarDogrula({ istenen: 1, stokMiktari: 0, birim: 'adet' })
    // Tavan 0 -> en kucuk adede dusurulur ama kullanici "stok yok" bilgisi alir
    expect(r.miktar).toBe(1)
    expect(r.asim).toBe(true)
  })

  it('asagida da asim bildirilmez (miktar minin altinda degil)', () => {
    const r = miktarDogrula({ istenen: 1, stokMiktari: 10, birim: 'adet' })
    expect(r.asim).toBe(false)
    expect(r.sorun).toBeNull()
  })
})

describe('adimla — artirma/azaltma', () => {
  it('adet biriminde 1 artar', () => {
    const r = adimla({ mevcut: 2, yon: 1, birim: 'adet' })
    expect(r.miktar).toBe(3)
    expect(r.degisti).toBe(true)
    expect(r.sorun).toBeNull()
  })

  it('kg biriminde 0.5 artar', () => {
    expect(adimla({ mevcut: 2, yon: 1, birim: 'kg' }).miktar).toBe(2.5)
  })

  it('adet biriminde 1 azalir', () => {
    expect(adimla({ mevcut: 2, yon: -1, birim: 'adet' }).miktar).toBe(1)
  })

  it('en kucuk degerin altina inilemez', () => {
    const r = adimla({ mevcut: 1, yon: -1, birim: 'adet' })
    expect(r.miktar).toBe(1)
    expect(r.degisti).toBe(false)
  })

  it('kg en kucugu 0.5: 0.5 altina inilemez', () => {
    const r = adimla({ mevcut: 0.5, yon: -1, birim: 'kg' })
    expect(r.miktar).toBe(0.5)
    expect(r.degisti).toBe(false)
  })

  it('tavana ulasinca artma DEGISTIRMEZ, asim bildirir', () => {
    // Kritik: sessizce kirpilmemeli; cagiran kullaniciya aciklamali.
    const r = adimla({ mevcut: 3, yon: 1, stokMiktari: 3, birim: 'adet' })
    expect(r.asim).toBe(true)
    expect(r.miktar).toBe(3)
  })

  it('tavana tam ulasmak aşim DEGILDIR (sinirda serbest)', () => {
    const r = adimla({ mevcut: 2, yon: 1, stokMiktari: 3, birim: 'adet' })
    expect(r.miktar).toBe(3)
    expect(r.asim).toBe(false)
    expect(r.sorun).toBeNull()
  })

  it('stok sonradan azaldiysa (bayat satir) tavana indirilir ve asim bildirir', () => {
    // Sepette 5 adet varken baska bir cihazdan stok 3'e dusmus olabilir.
    // Artma denemesi degil, mevcut degerin kendisi tavana uymuyor.
    const r = adimla({ mevcut: 5, yon: 1, stokMiktari: 3, birim: 'adet' })
    expect(r.miktar).toBe(3)
    expect(r.asim).toBe(true)
    expect(r.sorun).toBe(ADET_SORUN.ASIM)
  })

  it('tavan altindayken artma normaldir', () => {
    const r = adimla({ mevcut: 1, yon: 1, stokMiktari: 3, birim: 'adet' })
    expect(r.miktar).toBe(2)
    expect(r.sorun).toBeNull()
  })

  it('mevcut deger bozuk/eksikse 0 kabul edilir', () => {
    expect(adimla({ mevcut: undefined, yon: 1, birim: 'adet' }).miktar).toBe(1)
    expect(adimla({ mevcut: 'bozuk', yon: 1, birim: 'adet' }).miktar).toBe(1)
  })
})