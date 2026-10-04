import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  stokAPI: {
    ara: vi.fn()
  }
}))

import { stokAPI } from '../api/index.js'
import { useStokOnerileri } from '../composables/useStokOnerileri.js'

// Bu composable, onlarca view'da stok seçiciyi `getAll({ size: 1000 })` ile
// dolduran düzeni kaldırıyordu. 1000/5000 kayıtlık listeler hem her sayfa
// açılışında ağ trafiği üretiyor, hem de tavan sonrası hiçbir ürün seçilemiyordu.
describe('useStokOnerileri', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    stokAPI.ara.mockReset()
  })

  afterEach(() => {
    vi.useRealTimers()
  })

  it('sorguyu sunucuya gonderir ve sonuclari doner', async () => {
    stokAPI.ara.mockResolvedValue({ data: [{ id: 1, ad: 'Un', barkod: '111' }] })
    const { oneriler, ara } = useStokOnerileri()

    ara({ filter: 'un' })
    await vi.advanceTimersByTimeAsync(250)

    expect(stokAPI.ara).toHaveBeenCalledWith('un')
    expect(oneriler.value).toEqual([{ id: 1, ad: 'Un', barkod: '111' }])
  })

  it('AutoComplete `@complete` olayini da kabul eder', async () => {
    stokAPI.ara.mockResolvedValue({ data: [] })
    const { ara } = useStokOnerileri()

    ara({ query: 'cit' })
    await vi.advanceTimersByTimeAsync(250)

    expect(stokAPI.ara).toHaveBeenCalledWith('cit')
  })

  it('debounce: hizli tus vurusunda tek istek atar', async () => {
    stokAPI.ara.mockResolvedValue({ data: [] })
    const { ara } = useStokOnerileri()

    ara({ filter: 'a' })
    ara({ filter: 'ab' })
    ara({ filter: 'abc' })
    await vi.advanceTimersByTimeAsync(250)

    expect(stokAPI.ara).toHaveBeenCalledTimes(1)
    expect(stokAPI.ara).toHaveBeenCalledWith('abc')
  })

  it('yavas istek yeni sorgunun sonucunu ezmez', async () => {
    let ilkCoz = null
    stokAPI.ara
      .mockImplementationOnce(() => new Promise((r) => { ilkCoz = r }))
      .mockResolvedValueOnce({ data: [{ id: 2, ad: 'Yeni' }] })
    const { oneriler, ara } = useStokOnerileri()

    ara({ filter: 'a' })
    await vi.advanceTimersByTimeAsync(250)
    ara({ filter: 'ab' })
    await vi.advanceTimersByTimeAsync(250)

    ilkCoz({ data: [{ id: 1, ad: 'Eski' }] })
    await vi.advanceTimersByTimeAsync(0)

    expect(oneriler.value).toEqual([{ id: 2, ad: 'Yeni' }])
  })

  it('sonuclari istenen boyutla kirpar (limit yoksa katalog doner)', async () => {
    const cok = Array.from({ length: 120 }, (_, i) => ({ id: i + 1, ad: `S${i + 1}` }))
    stokAPI.ara.mockResolvedValue({ data: cok })
    const { oneriler, ara } = useStokOnerileri({ boyut: 20 })

    ara({ filter: 's' })
    await vi.advanceTimersByTimeAsync(250)

    expect(oneriler.value).toHaveLength(20)
  })

  it('minKacHarf altindaki sorgu sunucuya gitmez', async () => {
    stokAPI.ara.mockResolvedValue({ data: [{ id: 9, ad: 'Eski' }] })
    const { oneriler, ara } = useStokOnerileri({ minKacHarf: 2 })

    ara({ filter: 'a' })
    await vi.advanceTimersByTimeAsync(250)

    expect(stokAPI.ara).not.toHaveBeenCalled()
    expect(oneriler.value).toEqual([])
  })

  it('yukleniyor bayragi istek bitince iner', async () => {
    let coz = null
    stokAPI.ara.mockImplementation(() => new Promise((r) => { coz = r }))
    const { yukleniyor, ara } = useStokOnerileri()

    ara({ filter: 'a' })
    await vi.advanceTimersByTimeAsync(250)
    expect(yukleniyor.value).toBe(true)

    coz({ data: [] })
    await vi.advanceTimersByTimeAsync(0)
    expect(yukleniyor.value).toBe(false)
  })

  it('hata halinde onerileri bosaltir', async () => {
    stokAPI.ara.mockRejectedValue(new Error('500'))
    const { oneriler, ara } = useStokOnerileri()

    ara({ filter: 'a' })
    await vi.advanceTimersByTimeAsync(250)

    expect(oneriler.value).toEqual([])
  })

  it('hemenAra debounce beklemez', async () => {
    stokAPI.ara.mockResolvedValue({ data: [] })
    const { ara, hemenAra } = useStokOnerileri()

    ara({ filter: 'a' })
    await hemenAra()

    expect(stokAPI.ara).toHaveBeenCalledTimes(1)
    expect(stokAPI.ara).toHaveBeenCalledWith('')
  })
})