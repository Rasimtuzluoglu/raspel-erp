import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '../dashboardStore.js'
import { dashboardAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  dashboardAPI: {
    getData: vi.fn(),
    ozet: vi.fn(),
    gorevler: vi.fn(),
    grafikler: vi.fn(),
    akis: vi.fn(),
    icgoruler: vi.fn(),
    tercihGetir: vi.fn(),
    tercihKaydet: vi.fn()
  }
}))

const mockData = {
  toplamCariSayisi: 10,
  toplamBakiye: 50000,
  sonHareketler: [{ id: 1, tutar: 100 }]
}

describe('dashboardStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useDashboardStore()
    vi.clearAllMocks()
  })

  it('initializes with default state', () => {
    expect(store.toplamCariSayisi).toBe(0)
    expect(store.toplamBakiye).toBe(0)
    expect(store.sonHareketler).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getDashboardData sets all fields', async () => {
    dashboardAPI.getData.mockResolvedValue({ data: mockData })
    const result = await store.getDashboardData()
    expect(store.toplamCariSayisi).toBe(10)
    expect(store.toplamBakiye).toBe(50000)
    expect(store.sonHareketler).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual(mockData)
  })

  it('getDashboardData handles error', async () => {
    dashboardAPI.getData.mockRejectedValue(new Error('API down'))
    await expect(store.getDashboardData()).rejects.toThrow('API down')
    expect(store.error).toBe('API down')
    expect(store.loading).toBe(false)
  })

  it('başlangıçta parçalı durumlar boştur', () => {
    expect(store.ozet).toBeNull()
    expect(store.gorevler).toEqual([])
    expect(store.grafikler).toBeNull()
    expect(store.akis).toEqual([])
    expect(store.icgoruler).toEqual([])
    expect(store.tercih).toBeNull()
    expect(store.ozetYukleniyor).toBe(false)
  })

  it('getOzet nesneyi ve yükleniyor bayrağını yönetir', async () => {
    const ozet = { hero: { canli: true }, kpi: { likidite: { deger: 10 } } }
    dashboardAPI.ozet.mockResolvedValue({ data: ozet })
    const istek = store.getOzet()
    expect(store.ozetYukleniyor).toBe(true)
    await istek
    expect(store.ozet).toEqual(ozet)
    expect(store.ozetYukleniyor).toBe(false)
    expect(store.ozetHata).toBeNull()
  })

  it('getGorevler diziyi normalize eder', async () => {
    dashboardAPI.gorevler.mockResolvedValue({ data: [{ tur: 'ONAY_IZIN', adet: 1 }] })
    await store.getGorevler()
    expect(store.gorevler).toHaveLength(1)
    expect(store.gorevlerYukleniyor).toBe(false)
  })

  it('getGrafikler ve getIcgoruler veriyi doldurur', async () => {
    dashboardAPI.grafikler.mockResolvedValue({ data: { hedef: { ciro: 5 } } })
    dashboardAPI.icgoruler.mockResolvedValue({ data: [{ seviye: 'INFO' }] })
    await store.getGrafikler()
    await store.getIcgoruler()
    expect(store.grafikler).toEqual({ hedef: { ciro: 5 } })
    expect(store.icgoruler).toHaveLength(1)
  })

  it('getAkis limit parametresi ile çağrılır', async () => {
    dashboardAPI.akis.mockResolvedValue({ data: [] })
    await store.getAkis()
    expect(dashboardAPI.akis).toHaveBeenCalledWith({ limit: 20 })
    await store.getAkis({ limit: 5 })
    expect(dashboardAPI.akis).toHaveBeenLastCalledWith({ limit: 5 })
  })

  it('bölüm hatası yalnızca ilgili bayrağı doldurur', async () => {
    dashboardAPI.gorevler.mockRejectedValue(new Error('gorev yok'))
    await store.getGorevler()
    expect(store.gorevlerHata).toBe('gorev yok')
    expect(store.ozetHata).toBeNull()
    expect(store.akisHata).toBeNull()
  })

  it('getTercih hata durumunda null döner', async () => {
    dashboardAPI.tercihGetir.mockRejectedValue(new Error('yok'))
    const sonuc = await store.getTercih()
    expect(sonuc).toBeNull()
    expect(store.tercihHata).toBe('yok')
    expect(store.tercihYukleniyor).toBe(false)
  })

  it('kaydetTercih PUT gövdesini iletir', async () => {
    const duzen = { versiyon: 1, sira: [] }
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen } })
    await store.kaydetTercih(duzen)
    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledWith({ duzen })
    expect(store.tercihKaydediliyor).toBe(false)
  })

  it('kaydetTercih hatasında fırlatır', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValue(new Error('offline'))
    await expect(store.kaydetTercih({ versiyon: 1, sira: [] })).rejects.toThrow('offline')
    expect(store.tercihHata).toBe('offline')
  })

  it('legacy ozet metni ayrı alanda tutulur', async () => {
    dashboardAPI.getData.mockResolvedValue({ data: { ozet: 'günün özeti' } })
    await store.getDashboardData()
    expect(store.gununOzetiMetni).toBe('günün özeti')
    expect(store.ozet).toBeNull()
  })
})
