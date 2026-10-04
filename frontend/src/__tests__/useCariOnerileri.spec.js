import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  cariHesapAPI: {
    filtreli: vi.fn(),
    getById: vi.fn()
  }
}))

import { cariHesapAPI } from '../api/index.js'
import { useCariOnerileri, cariHesapCoz } from '../composables/useCariOnerileri.js'

// Bu composable, alti view'da "cari secici 50 kayitlik tavana takiliyor"
// problemini cozuyor: liste sayfa taşımıyor, her arama sunucuya gidiyor.
// Onemli olan davranislar: dogru parametre adi (`q`, `search` degil),
// yarış korumasi ve tekil kayit fallback'i.
describe('useCariOnerileri', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    cariHesapAPI.filtreli.mockReset()
    cariHesapAPI.getById.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('arama metnini `q` parametresiyle sunucuya gonderir', async () => {
    cariHesapAPI.filtreli.mockResolvedValue({ data: { content: [{ id: 7, ad: 'Atlas' }] } })
    const { oneriler, ara } = useCariOnerileri()

    ara({ query: '  atlas  ' })
    await vi.advanceTimersByTimeAsync(250)

    // `search` gondermek filtreyi sessizce dusuruyordu: her arama filtresiz
    // ilk sayfayi getiriyor, kullanici yazdıkça liste degismiyordu.
    expect(cariHesapAPI.filtreli).toHaveBeenCalledWith({ page: 0, size: 20, q: 'atlas' })
    expect(oneriler.value).toEqual([{ id: 7, ad: 'Atlas' }])
  })

  it('bos sorgu `q` gondermez', async () => {
    cariHesapAPI.filtreli.mockResolvedValue({ data: [] })
    const { ara } = useCariOnerileri()

    ara({ query: '   ' })
    await vi.advanceTimersByTimeAsync(250)

    expect(cariHesapAPI.filtreli).toHaveBeenCalledWith({ page: 0, size: 20 })
  })

  it('debounce: hizli tus vurusunda tek istek atar', async () => {
    cariHesapAPI.filtreli.mockResolvedValue({ data: [] })
    const { ara } = useCariOnerileri()

    ara({ query: 'a' })
    ara({ query: 'at' })
    ara({ query: 'atl' })
    await vi.advanceTimersByTimeAsync(250)

    expect(cariHesapAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(cariHesapAPI.filtreli).toHaveBeenCalledWith({ page: 0, size: 20, q: 'atl' })
  })

  it('yavas istek yeni sorgunun sonucunu ezmez', async () => {
    let ilkCoz = null
    cariHesapAPI.filtreli
      .mockImplementationOnce(() => new Promise((r) => { ilkCoz = r }))
      .mockResolvedValueOnce({ data: { content: [{ id: 2, ad: 'Yeni' }] } })
    const { oneriler, ara } = useCariOnerileri()

    ara({ query: 'a' })
    await vi.advanceTimersByTimeAsync(250)
    ara({ query: 'ab' })
    await vi.advanceTimersByTimeAsync(250)

    // Eski ("a") istegi simdi coz.
    ilkCoz({ data: { content: [{ id: 1, ad: 'Eski' }] } })
    await vi.advanceTimersByTimeAsync(0)

    expect(oneriler.value).toEqual([{ id: 2, ad: 'Yeni' }])
  })

  it('yukleniyor bayragi istek bitince iner', async () => {
    let coz = null
    cariHesapAPI.filtreli.mockImplementation(() => new Promise((r) => { coz = r }))
    const { yukleniyor, ara } = useCariOnerileri()

    ara({ query: 'a' })
    await vi.advanceTimersByTimeAsync(250)
    expect(yukleniyor.value).toBe(true)

    coz({ data: [] })
    await vi.advanceTimersByTimeAsync(0)
    expect(yukleniyor.value).toBe(false)
  })

  it('hata halinde onerileri bosaltir, cokmez', async () => {
    cariHesapAPI.filtreli.mockRejectedValue(new Error('500'))
    const { oneriler, ara } = useCariOnerileri()

    ara({ query: 'a' })
    await vi.advanceTimersByTimeAsync(250)

    expect(oneriler.value).toEqual([])
  })

  it('tur filtresi tedarikci secicisine aktarilir', async () => {
    cariHesapAPI.filtreli.mockResolvedValue({ data: [] })
    const { hemenAra } = useCariOnerileri({ ekParams: { tur: 'Tedarikci' } })

    await hemenAra()

    expect(cariHesapAPI.filtreli).toHaveBeenCalledWith({ page: 0, size: 20, tur: 'Tedarikci' })
  })

  it('hemenAra debounce beklemez', async () => {
    cariHesapAPI.filtreli.mockResolvedValue({ data: [] })
    const { ara, hemenAra } = useCariOnerileri()

    ara({ query: 'a' })
    await hemenAra('x')

    expect(cariHesapAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(cariHesapAPI.filtreli).toHaveBeenCalledWith({ page: 0, size: 20, q: 'x' })
  })
})

describe('cariHesapCoz', () => {
  beforeEach(() => {
    cariHesapAPI.getById.mockReset()
  })

  it('onbellekte varsa sunucuya gitmez', async () => {
    const sonuc = await cariHesapCoz(5, [{ id: 5, ad: 'Onbellekteki' }])
    expect(sonuc).toEqual({ id: 5, ad: 'Onbellekteki' })
    expect(cariHesapAPI.getById).not.toHaveBeenCalled()
  })

  it('onbellekte yoksa sunucudan tekil kayit ceker', async () => {
    cariHesapAPI.getById.mockResolvedValue({ data: { id: 9, ad: 'Uzak' } })
    const sonuc = await cariHesapCoz(9, [{ id: 5, ad: 'Diger' }])
    expect(sonuc).toEqual({ id: 9, ad: 'Uzak' })
    expect(cariHesapAPI.getById).toHaveBeenCalledWith(9)
  })

  it('kimlik bos ise hic istek atmaz', async () => {
    expect(await cariHesapCoz(null, [])).toBeNull()
    expect(await cariHesapCoz('', [])).toBeNull()
    expect(cariHesapAPI.getById).not.toHaveBeenCalled()
  })

  it('404 halinde null doner', async () => {
    cariHesapAPI.getById.mockRejectedValue(new Error('404'))
    expect(await cariHesapCoz(404, [])).toBeNull()
  })
})