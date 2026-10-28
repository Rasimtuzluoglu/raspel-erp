import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { apiClient } from '../api/client.js'
import { dashboardAPI } from '../api/index.js'
import { useDashboardStore } from '../stores/dashboardStore.js'
import tr from '../locales/tr.json'
import en from '../locales/en.json'

vi.mock('../api/client.js', () => {
  const istemci = {
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
  const networkStatus = { online: true, showBanner: false, sunucuyaUlasilamiyor: false }
  return { apiClient: istemci, networkStatus, default: istemci }
})

const flatten = (obj, prefix = '', out = {}) => {
  for (const k of Object.keys(obj)) {
    const key = prefix ? `${prefix}.${k}` : k
    const v = obj[k]
    if (v && typeof v === 'object' && !Array.isArray(v)) flatten(v, key, out)
    else out[key] = v
  }
  return out
}

describe('Dashboard faz1 — API sözleşmesi', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    apiClient.get.mockResolvedValue({ data: {} })
    apiClient.put.mockResolvedValue({ data: {} })
  })

  it('legacy /dashboard ucu korunur', async () => {
    await dashboardAPI.getData()
    expect(apiClient.get).toHaveBeenCalledWith('/dashboard')
  })

  it.each([
    ['ozet', '/dashboard/ozet'],
    ['gorevler', '/dashboard/gorevler'],
    ['grafikler', '/dashboard/grafikler'],
    ['icgoruler', '/dashboard/icgoruler'],
    ['tercihGetir', '/dashboard/tercihler']
  ])('%s ucu doğru adrese gider', async (metot, adres) => {
    await dashboardAPI[metot]()
    expect(apiClient.get).toHaveBeenCalledWith(adres)
  })

  it('akis ucu limit parametresi ile çağrılır', async () => {
    await dashboardAPI.akis({ limit: 20 })
    expect(apiClient.get).toHaveBeenCalledWith('/dashboard/akis', { params: { limit: 20 } })
  })

  it('tercih kaydı PUT /dashboard/tercihler gövdesiyle yapılır', async () => {
    const duzen = { versiyon: 1, sira: [{ key: 'kpi', boyut: 2, gorunur: true }] }
    await dashboardAPI.tercihKaydet({ duzen })
    expect(apiClient.put).toHaveBeenCalledWith('/dashboard/tercihler', { duzen })
  })
})

describe('Dashboard faz1 — store parçalı yükleme', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useDashboardStore()
    vi.clearAllMocks()
    apiClient.get.mockResolvedValue({ data: {} })
    apiClient.put.mockResolvedValue({ data: {} })
  })

  it('getOzet veriyi ve yükleniyor bayrağını yönetir', async () => {
    apiClient.get.mockResolvedValueOnce({ data: { hero: { canli: true } } })
    const istek = store.getOzet()
    expect(store.ozetYukleniyor).toBe(true)
    await istek
    expect(store.ozetYukleniyor).toBe(false)
    expect(store.ozet).toEqual({ hero: { canli: true } })
    expect(store.ozetHata).toBeNull()
  })

  it('bölüm hatası diğer bölümleri etkilemez', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('gorevler kapalı'))
    await store.getGorevler()
    expect(store.gorevlerHata).toBe('gorevler kapalı')
    expect(store.gorevler).toEqual([])
    expect(store.gorevlerYukleniyor).toBe(false)

    apiClient.get.mockResolvedValueOnce({ data: { kpi: {} } })
    await store.getOzet()
    expect(store.ozet).toEqual({ kpi: {} })
    expect(store.ozetHata).toBeNull()
  })

  it('getAkis limit parametresini iletir ve diziyi normalize eder', async () => {
    apiClient.get.mockResolvedValueOnce({ data: [{ tur: 'TAHSILAT' }] })
    await store.getAkis()
    expect(apiClient.get).toHaveBeenCalledWith('/dashboard/akis', { params: { limit: 20 } })
    expect(store.akis).toHaveLength(1)
  })

  it('getGrafikler ve getIcgoruler veriyi doldurur', async () => {
    apiClient.get.mockResolvedValueOnce({ data: { hedef: { ciro: 1 } } })
    await store.getGrafikler()
    expect(store.grafikler).toEqual({ hedef: { ciro: 1 } })

    apiClient.get.mockResolvedValueOnce({ data: [{ seviye: 'INFO' }] })
    await store.getIcgoruler()
    expect(store.icgoruler).toHaveLength(1)
  })

  it('getTercih hata durumunda null döner ve akışı bozmaz', async () => {
    apiClient.get.mockRejectedValueOnce(new Error('404'))
    const sonuc = await store.getTercih()
    expect(sonuc).toBeNull()
    expect(store.tercihHata).toBe('404')
    expect(store.tercihYukleniyor).toBe(false)
  })

  it('kaydetTercih başarıda tercihi günceller', async () => {
    apiClient.put.mockResolvedValueOnce({ data: { duzen: { versiyon: 1, sira: [] } } })
    const sonuc = await store.kaydetTercih({ versiyon: 1, sira: [] })
    expect(sonuc).toEqual({ duzen: { versiyon: 1, sira: [] } })
    expect(store.tercihKaydediliyor).toBe(false)
  })

  it('kaydetTercih hatasında fırlatır ve hatayı saklar', async () => {
    apiClient.put.mockRejectedValueOnce(new Error('sunucu yok'))
    await expect(store.kaydetTercih({ versiyon: 1, sira: [] })).rejects.toThrow('sunucu yok')
    expect(store.tercihHata).toBe('sunucu yok')
    expect(store.tercihKaydediliyor).toBe(false)
  })

  it('legacy getDashboardData alanları doldurmaya devam eder', async () => {
    apiClient.get.mockResolvedValueOnce({ data: { toplamCariSayisi: 7, ozet: 'günün özeti' } })
    const sonuc = await store.getDashboardData()
    expect(sonuc.toplamCariSayisi).toBe(7)
    expect(store.toplamCariSayisi).toBe(7)
    expect(store.gununOzetiMetni).toBe('günün özeti')
  })
})

describe('Dashboard faz1 — i18n simetrisi', () => {
  const trDuz = flatten(tr)
  const enDuz = flatten(en)

  it('tr ve en anahtar kümeleri eşit', () => {
    const trAnahtar = Object.keys(trDuz).sort()
    const enAnahtar = Object.keys(enDuz).sort()
    expect(trAnahtar).toEqual(enAnahtar)
  })

  it('yeni dashboard anahtarları iki dilde de var', () => {
    const anahtarlar = [
      'dashboard.heroAra',
      'dashboard.duzenle',
      'dashboard.bugunTahsilat',
      'dashboard.kpiLikidite',
      'dashboard.kpiNetCari',
      'dashboard.kpiCiro',
      'dashboard.kpiKar',
      'dashboard.gorevTuruOnayIzin',
      'dashboard.gorevTuruVadesiGecen',
      'dashboard.icgoruKritik',
      'dashboard.hedefCiro',
      'dashboard.grafikNakitAkisi',
      'dashboard.akisTurTahsilat',
      'dashboard.akisDakikaOnce',
      'dashboard.duzenBaslik',
      'dashboard.duzenKaydet',
      'dashboard.duzenSifirla',
      'dashboard.widgetKpi',
      'dashboard.widgetHizliIslemler'
    ]
    for (const anahtar of anahtarlar) {
      expect(trDuz[anahtar], `tr:${anahtar}`).toBeTruthy()
      expect(enDuz[anahtar], `en:${anahtar}`).toBeTruthy()
    }
  })

  it('mevcut dashboard anahtarları korunur', () => {
    const eski = [
      'dashboard.canli',
      'dashboard.sonGuncelleme',
      'dashboard.hizliIslemler',
      'dashboard.gorevBos',
      'dashboard.grafikBos',
      'dashboard.gununOzeti',
      'dashboard.yenile',
      'dashboard.iyiGunler'
    ]
    for (const anahtar of eski) {
      expect(trDuz[anahtar]).toBeTruthy()
      expect(enDuz[anahtar]).toBeTruthy()
    }
  })

  it('yeni metinlerde kaçışsız @ veya literal | yok', () => {
    const yeni = Object.keys(trDuz).filter((k) => k.startsWith('dashboard.'))
    for (const anahtar of yeni) {
      expect(String(trDuz[anahtar])).not.toContain('|')
      expect(String(trDuz[anahtar]).replace(/\{'@'\}/g, '')).not.toContain('@')
      expect(String(enDuz[anahtar])).not.toContain('|')
    }
  })
})
