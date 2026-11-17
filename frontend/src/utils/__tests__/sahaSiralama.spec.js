import { describe, it, expect } from 'vitest'
import { sirala } from '../sahaSiralama.js'

const liste = (...idler) => idler.map((id) => ({ id, ad: `kayit-${id}` }))

describe('sirala', () => {
  it('kayitlari verilen siraya gore dizer', () => {
    const sonuc = sirala(liste(1, 2, 3), [3, 1, 2])

    expect(sonuc.map((k) => k.id)).toEqual([3, 1, 2])
  })

  it('siralama listesinde olmayanlari sona atar', () => {
    const sonuc = sirala(liste(1, 2, 3, 4), [3, 1])

    expect(sonuc.map((k) => k.id)).toEqual([3, 1, 2, 4])
  })

  it('esitlikte giris sirasini korur (kararli)', () => {
    const sonuc = sirala(liste(5, 6, 7), [])

    expect(sonuc.map((k) => k.id)).toEqual([5, 6, 7])
  })

  it('giris listesini degistirmez', () => {
    const giris = liste(1, 2, 3)
    const kopya = [...giris]

    sirala(giris, [2, 3, 1])

    expect(giris).toEqual(kopya)
  })

  it('gecersiz girdide bos dizi doner', () => {
    expect(sirala(null, [1])).toEqual([])
    expect(sirala(liste(1), null).map((k) => k.id)).toEqual([1])
  })
})
