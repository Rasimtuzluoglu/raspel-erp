import { describe, it, expect, vi } from 'vitest'
import { useSahaKuyruk } from '../useSahaKuyruk.js'
import { bellekDeposu } from '../sahaKuyrukDeposu.js'

const kayit = (overrides = {}) => ({
  tur: 'TESLIMAT',
  payload: { teslimatId: 5 },
  dosyalar: [],
  ...overrides
})

const kurulum = (gondericiler = {}, depo = bellekDeposu()) => {
  const kuyruk = useSahaKuyruk({ gondericiler, depo })
  return { kuyruk, depo }
}

describe('useSahaKuyruk', () => {
  it('kaydi ekler ve listeler', async () => {
    const { kuyruk } = kurulum()

    const id = await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    expect(id).toBeTruthy()
    expect(kuyruk.kuyruk.value).toHaveLength(1)
    expect(kuyruk.kuyruk.value[0].durum).toBe('BEKLIYOR')
    expect(kuyruk.kuyruk.value[0].clientKey).toBeTruthy()
  })

  it('basarili senkron kaydi siler ve gondericiyi cagirir', async () => {
    const gonderici = vi.fn().mockResolvedValue({})
    const { kuyruk } = kurulum({ TESLIMAT: gonderici })
    await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    const sonuc = await kuyruk.senkronize()

    expect(gonderici).toHaveBeenCalledOnce()
    expect(sonuc.gonderilen).toBe(1)
    expect(kuyruk.kuyruk.value).toHaveLength(0)
  })

  it('ag hatasinda kayit beklemede kalir', async () => {
    const gonderici = vi.fn().mockRejectedValue(new TypeError('offline'))
    const { kuyruk } = kurulum({ TESLIMAT: gonderici })
    await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    const sonuc = await kuyruk.senkronize()

    expect(sonuc.basarisiz).toBe(1)
    expect(kuyruk.kuyruk.value).toHaveLength(1)
    expect(kuyruk.kuyruk.value[0].durum).toBe('BEKLIYOR')
    expect(kuyruk.kuyruk.value[0].deneme).toBe(1)
  })

  it('4xx hatasinda kaydi HATA durumuna alir ve tekrar denemez', async () => {
    const gonderici = vi.fn().mockRejectedValue({ response: { status: 422 }, message: 'gecersiz' })
    const { kuyruk } = kurulum({ TESLIMAT: gonderici })
    await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    await kuyruk.senkronize()
    await kuyruk.senkronize()

    expect(kuyruk.kuyruk.value[0].durum).toBe('HATA')
    expect(gonderici).toHaveBeenCalledTimes(1)
  })

  it('clientKey verilmezse uretir, verilirse korur', async () => {
    const { kuyruk } = kurulum()

    await kuyruk.ekle(kayit())
    await kuyruk.ekle(kayit({ clientKey: 'sabit-anahtar' }))
    await kuyruk.yukle()

    const anahtarlar = kuyruk.kuyruk.value.map((k) => k.clientKey)
    expect(anahtarlar).toContain('sabit-anahtar')
    expect(new Set(anahtarlar).size).toBe(2)
  })

  it('eski localStorage kuyrugunu tasir ve anahtari siler', async () => {
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      { tur: 'NOT', baslik: 'Ziyaret', icerik: 'Not' },
      { tur: 'TESLIMAT', siparisId: 7, teslimAlanAd: 'Ali' }
    ]))
    const { kuyruk } = kurulum()

    await kuyruk.yukle()

    expect(kuyruk.kuyruk.value).toHaveLength(2)
    expect(kuyruk.kuyruk.value.map((k) => k.tur).sort()).toEqual(['TESLIMAT', 'ZIYARET'])
    expect(localStorage.getItem('raspel_saha_kuyrugu')).toBeNull()
  })

  it('ayni anda iki senkron calismaz', async () => {
    let coz
    const gonderici = vi.fn().mockImplementation(() => new Promise((r) => { coz = r }))
    const { kuyruk } = kurulum({ TESLIMAT: gonderici })
    await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    const birinci = kuyruk.senkronize()
    const ikinci = kuyruk.senkronize()
    coz({})
    await Promise.all([birinci, ikinci])

    expect(gonderici).toHaveBeenCalledTimes(1)
  })

  it('depo yazamazsa localStorage yedegine duser ve kisit bayragi kalkar', async () => {
    const hataliDepo = {
      liste: async () => [],
      ekle: async () => { throw new Error('quota') },
      sil: async () => {},
      guncelle: async () => {}
    }
    const { kuyruk } = kurulum({}, hataliDepo)

    await kuyruk.ekle(kayit())

    expect(kuyruk.depolamaKisitli.value).toBe(true)
    const yedek = JSON.parse(localStorage.getItem('raspel_saha_kuyrugu_yedek') || '[]')
    expect(yedek).toHaveLength(1)
  })
})
