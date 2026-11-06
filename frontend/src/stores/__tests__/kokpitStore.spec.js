import { setActivePinia, createPinia } from 'pinia'
import { useKokpitStore, KOKPIT_BOLUMLERI } from '../kokpitStore.js'
import { dashboardAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  dashboardAPI: {
    kokpit: vi.fn(),
    tercihGetir: vi.fn(),
    tercihKaydet: vi.fn()
  }
}))

describe('kokpitStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useKokpitStore()
    vi.clearAllMocks()
  })

  it('varsayilanBolumlerKurali', () => {
    expect(store.donem).toBe('BU_AY')
    expect(store.gorunurBolumler).toEqual(KOKPIT_BOLUMLERI)
    expect(store.yukleniyor).toBe(false)
    expect(store.hata).toBeNull()
  })

  it('kokpitApiDonemleCagirir', async () => {
    const veri = { ozet: [{ anahtar: 'netNakit' }] }
    dashboardAPI.kokpit.mockResolvedValue({ data: veri })

    const sonuc = await store.getKokpit('GECEN_AY')

    expect(dashboardAPI.kokpit).toHaveBeenCalledWith({ donem: 'GECEN_AY' })
    expect(store.kokpit).toEqual(veri)
    expect(sonuc).toEqual(veri)
    expect(store.sonGuncelleme).toBeInstanceOf(Date)
    expect(store.yukleniyor).toBe(false)
  })

  it('donemDegistirYeniIstekAtar', async () => {
    dashboardAPI.kokpit.mockResolvedValue({ data: { ozet: [] } })

    await store.donemDegistir('BU_YIL')

    expect(store.donem).toBe('BU_YIL')
    expect(dashboardAPI.kokpit).toHaveBeenCalledWith({ donem: 'BU_YIL' })
  })

  it('apiHatasiHataMesajiBirakir', async () => {
    dashboardAPI.kokpit.mockResolvedValueOnce({ data: { ozet: [{ anahtar: 'netNakit' }] } })
    await store.getKokpit('BU_AY')
    const onceki = store.kokpit

    dashboardAPI.kokpit.mockRejectedValueOnce(new Error('API down'))
    await store.getKokpit('GECEN_AY')

    expect(store.hata).toBe('API down')
    expect(store.kokpit).toEqual(onceki)
    expect(store.yukleniyor).toBe(false)
  })

  it('bolumGorunurlukDegistirTercihiGunceller', async () => {
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: {} } })

    store.bolumGorunurlukDegistir('karMarj', false)
    expect(store.gorunurBolumler).not.toContain('karMarj')

    await store.tercihKaydet()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledWith({
      duzen: {
        bolumSirasi: ['nakit', 'alacakVerecek', 'faturaSiparis'],
        gorunurBolumler: ['nakit', 'alacakVerecek', 'faturaSiparis']
      }
    })
  })

  it('enAzBirBolumKalmali', () => {
    store.bolumGorunurlukDegistir('nakit', false)
    store.bolumGorunurlukDegistir('alacakVerecek', false)
    store.bolumGorunurlukDegistir('faturaSiparis', false)
    expect(store.gorunurBolumler).toEqual(['karMarj'])

    store.bolumGorunurlukDegistir('karMarj', false)
    expect(store.gorunurBolumler).toEqual(['karMarj'])
  })

  it('tercihYukleKayitliSirayiGeriYukler', async () => {
    dashboardAPI.tercihGetir.mockResolvedValue({
      data: { duzen: { bolumSirasi: ['faturaSiparis', 'nakit', 'gecersiz'] } }
    })

    await store.tercihYukle()

    expect(store.gorunurBolumler).toEqual(['faturaSiparis', 'nakit'])
    expect(store.tercihYukleniyor).toBe(false)
  })

  it('tercihYukleKayitYoksaVarsayilanaDoner', async () => {
    dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })

    await store.tercihYukle()

    expect(store.gorunurBolumler).toEqual(KOKPIT_BOLUMLERI)
  })

  it('tercihiSifirlaVarsayilanDuzeneDoner', async () => {
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: {} } })
    store.bolumGorunurlukDegistir('nakit', false)

    await store.tercihiSifirla()

    expect(store.gorunurBolumler).toEqual(KOKPIT_BOLUMLERI)
    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledWith({
      duzen: {
        bolumSirasi: KOKPIT_BOLUMLERI,
        gorunurBolumler: KOKPIT_BOLUMLERI
      }
    })
  })
})
