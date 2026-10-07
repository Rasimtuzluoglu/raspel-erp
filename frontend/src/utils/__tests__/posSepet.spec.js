import { describe, it, expect } from 'vitest'
import {
  sepetSatiriBul,
  kullanilanFiyatTipleri,
  sonrakiFiyatTipi,
  sepetteToplamAdet
} from '../posSepet.js'

/**
 * SEPET SATIRI KURALLARI — birim testleri.
 *
 * Sepet satirinin kimligi `(stokId + fiyatTipi)` ciftidir. Yalniz-id aramasi
 * su dort hatayi uretiyordu: miktar artmama, yanlis satirin zenginlestirilmesi,
 * eksik kart rozeti, anlamsiz cogaltma.
 */

const FIYATLAR = [
  { ad: 'Perakende', fiyat: 100 },
  { ad: 'Toptan', fiyat: 90 },
  { ad: 'Özel', fiyat: 80 }
]

const satir = (id, fiyatTipi, miktar = 1) => ({ id, ad: `U${id}`, fiyatTipi, miktar, fiyatlar: FIYATLAR })

describe('sepetSatiriBul', () => {
  it('id + fiyatTipi ciftini tam eslestirir', () => {
    const sepet = [satir(1, 'Perakende', 5), satir(1, 'Toptan', 2)]
    expect(sepetSatiriBul(sepet, 1, 'Toptan').miktar).toBe(2)
    expect(sepetSatiriBul(sepet, 1, 'Perakende').miktar).toBe(5)
  })

  it('fiyat tipi farkliysa ESLESMEZ (ilk satiri dondurmez)', () => {
    const sepet = [satir(1, 'Perakende', 5)]
    expect(sepetSatiriBul(sepet, 1, 'Toptan')).toBeUndefined()
  })

  it('bulunamazsa undefined', () => {
    expect(sepetSatiriBul([], 99, 'Perakende')).toBeUndefined()
  })
})

describe('kullanilanFiyatTipleri', () => {
  it('yalniz o urunun kullandigi tipleri dondurur', () => {
    const sepet = [satir(1, 'Perakende'), satir(1, 'Toptan'), satir(2, 'Özel')]
    expect([...kullanilanFiyatTipleri(sepet, 1)].sort()).toEqual(['Perakende', 'Toptan'])
  })

  it('hic satir yoksa bos kume', () => {
    expect(kullanilanFiyatTipleri([], 1).size).toBe(0)
  })
})

describe('sonrakiFiyatTipi', () => {
  it('kullanilmayan ILK alternatifi secer', () => {
    const kullanilan = new Set(['Perakende'])
    expect(sonrakiFiyatTipi(FIYATLAR, kullanilan)).toEqual({ ad: 'Toptan', fiyat: 90 })
  })

  it('hepsi kullanildiysa null doner (cogaltma yapilmamali)', () => {
    const kullanilan = new Set(['Perakende', 'Toptan', 'Özel'])
    expect(sonrakiFiyatTipi(FIYATLAR, kullanilan)).toBeNull()
  })

  it('fiyat listesi bossa null doner', () => {
    expect(sonrakiFiyatTipi([], new Set())).toBeNull()
    expect(sonrakiFiyatTipi(undefined, new Set())).toBeNull()
  })

  it('tek fiyat tipi varsa cogaltma MUMKUN DEGILDIR', () => {
    // Tek fiyatli urunde "cogalt" anlamsizdir: yeni satir ayni seyin kopyasi olur.
    const tek = [{ ad: 'Perakende', fiyat: 100 }]
    expect(sonrakiFiyatTipi(tek, new Set(['Perakende']))).toBeNull()
  })
})

describe('sepetteToplamAdet', () => {
  it('ayni urunun tum satirlarini TOPLAR', () => {
    const sepet = [satir(1, 'Perakende', 5), satir(1, 'Toptan', 3), satir(2, 'Perakende', 1)]
    expect(sepetteToplamAdet(sepet, 1)).toBe(8)
  })

  it('ondalik adetleri dogru toplar (kg)', () => {
    const sepet = [
      { id: 1, fiyatTipi: 'Perakende', miktar: 2.5, fiyatlar: FIYATLAR },
      { id: 1, fiyatTipi: 'Toptan', miktar: 0.5, fiyatlar: FIYATLAR }
    ]
    expect(sepetteToplamAdet(sepet, 1)).toBe(3)
  })

  it('bozuk/eksik miktar 0 sayilir', () => {
    const sepet = [
      { id: 1, fiyatTipi: 'A', miktar: undefined },
      { id: 1, fiyatTipi: 'B', miktar: 'x' },
      { id: 1, fiyatTipi: 'C', miktar: 4 }
    ]
    expect(sepetteToplamAdet(sepet, 1)).toBe(4)
  })

  it('sepette olmayan urun 0', () => {
    expect(sepetteToplamAdet([], 7)).toBe(0)
  })
})