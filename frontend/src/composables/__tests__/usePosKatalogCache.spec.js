import { describe, it, expect, beforeEach } from 'vitest'
import { usePosKatalogCache, MAKS_ONBELLEK } from '../usePosKatalogCache.js'

const urun = (id) => ({ id, ad: `Urun ${id}` })

describe('usePosKatalogCache', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('kaydedilen urunler geri yuklenir', () => {
    const c = usePosKatalogCache()
    c.kaydet([urun(1), urun(2)])
    expect(c.yukle().map((u) => u.id)).toEqual([1, 2])
  })

  it('onbellek yoksa bos dizi doner', () => {
    const c = usePosKatalogCache()
    expect(c.yukle()).toEqual([])
  })

  it('kayit sayisi maksKayit ile sinirlanir', () => {
    const c = usePosKatalogCache({ maksKayit: 3 })
    c.kaydet([urun(1), urun(2), urun(3), urun(4), urun(5)])
    expect(c.yukle()).toHaveLength(3)
  })

  it('varsayilan ust sinir MAKS_ONBELLEK', () => {
    const c = usePosKatalogCache()
    c.kaydet(Array.from({ length: MAKS_ONBELLEK + 50 }, (_, i) => urun(i)))
    expect(c.yukle()).toHaveLength(MAKS_ONBELLEK)
  })

  it('temizle onbellegi siler', () => {
    const c = usePosKatalogCache()
    c.kaydet([urun(1)])
    c.temizle()
    expect(c.yukle()).toEqual([])
  })

  it('bozuk kayit guvenli sekilde bos doner', () => {
    localStorage.setItem('raspel_pos_katalog_onbellek', 'gecersiz-json')
    const c = usePosKatalogCache()
    expect(c.yukle()).toEqual([])
  })

  it('dizi olmayan icerik bos doner', () => {
    localStorage.setItem('raspel_pos_katalog_onbellek', JSON.stringify({ urunler: 'x' }))
    const c = usePosKatalogCache()
    expect(c.yukle()).toEqual([])
  })
})
