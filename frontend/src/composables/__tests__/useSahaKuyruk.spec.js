import { describe, it, expect, vi, beforeEach } from 'vitest'
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
  beforeEach(() => {
    localStorage.clear()
  })

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

  it('eski teslimat imzasini dosyalara tasir ve siparisId korunur', async () => {
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      {
        tur: 'TESLIMAT',
        siparisId: 7,
        teslimAlanAd: 'Ali',
        teslimNotu: 'hasarsiz',
        imzaDataUrl: 'data:image/png;base64,ESKI'
      }
    ]))
    const { kuyruk } = kurulum()

    await kuyruk.yukle()

    const kayit = kuyruk.kuyruk.value[0]
    expect(kayit.payload.siparisId).toBe(7)
    expect(kayit.payload.imzaDataUrl).toBeUndefined()
    expect(kayit.dosyalar).toHaveLength(1)
    expect(kayit.dosyalar[0].dataUrl).toBe('data:image/png;base64,ESKI')
    expect(kayit.dosyalar[0].ad).toMatch(/^imza-7\.png$/)
  })

  it('yazma hatasindaki kayit bir sonraki yuklemede depoya geri alinir', async () => {
    let ilkDeneme = true
    const bellek = bellekDeposu()
    const birKezHataliDepo = {
      liste: bellek.liste,
      sil: bellek.sil,
      guncelle: bellek.guncelle,
      ekle: async (kayit) => {
        if (ilkDeneme) {
          ilkDeneme = false
          throw new Error('quota')
        }
        return bellek.ekle(kayit)
      }
    }
    const { kuyruk } = kurulum({}, birKezHataliDepo)

    await kuyruk.ekle(kayit())
    expect(kuyruk.depolamaKisitli.value).toBe(true)

    await kuyruk.yukle()

    expect(kuyruk.kuyruk.value).toHaveLength(1)
    expect(JSON.parse(localStorage.getItem('raspel_saha_kuyrugu_yedek') || '[]')).toHaveLength(0)
  })

  it('tasinamayan eski kuyruk anahtari silinmez', async () => {
    const hataliDepo = {
      liste: async () => [],
      ekle: async () => { throw new Error('quota') },
      sil: async () => {},
      guncelle: async () => {}
    }
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      { tur: 'NOT', baslik: 'Ziyaret', icerik: 'Not' }
    ]))
    const { kuyruk } = kurulum({}, hataliDepo)

    await kuyruk.yukle()

    expect(localStorage.getItem('raspel_saha_kuyrugu')).not.toBeNull()
  })

  it('ham hata govdesini kullaniciya gostermez', async () => {
    const gonderici = vi.fn().mockRejectedValue({
      response: { status: 500, data: '<html>proxy hatasi</html>' },
      message: 'Request failed with status code 500'
    })
    const { kuyruk } = kurulum({ TESLIMAT: gonderici })
    await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    await kuyruk.senkronize()

    expect(kuyruk.kuyruk.value[0].sonHata).toBe('Request failed with status code 500')
  })

  it('kuyrugu olusturma zamanina gore siralar', async () => {
    const depo = bellekDeposu([
      { id: 'b', tur: 'ZIYARET', payload: {}, dosyalar: [], durum: 'BEKLIYOR', olusturma: '2026-11-17T10:00:00Z' },
      { id: 'a', tur: 'TESLIMAT', payload: {}, dosyalar: [], durum: 'BEKLIYOR', olusturma: '2026-11-17T09:00:00Z' }
    ])
    const { kuyruk } = kurulum({}, depo)

    await kuyruk.yukle()

    expect(kuyruk.kuyruk.value.map((k) => k.id)).toEqual(['a', 'b'])
  })

  it('gondericiye kaydi guncelleme yardimcisi verilir', async () => {
    const depo = bellekDeposu()
    let guncellemeYapildi = false
    const gonderici = vi.fn().mockImplementation(async (kayit, yardimci) => {
      expect(typeof yardimci.guncelle).toBe('function')
      kayit.payload.asama = 'belge'
      await yardimci.guncelle(kayit)
      guncellemeYapildi = true
      throw { response: { status: 500 } }
    })
    const { kuyruk } = kurulum({ TESLIMAT: gonderici }, depo)
    await kuyruk.ekle(kayit())
    await kuyruk.yukle()

    await kuyruk.senkronize()

    expect(guncellemeYapildi).toBe(true)
    const kayitlar = await depo.liste()
    expect(kayitlar[0].payload.asama).toBe('belge')
  })
})
