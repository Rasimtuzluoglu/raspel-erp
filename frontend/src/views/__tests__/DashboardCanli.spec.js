import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import { dashboardAPI, onaylarAPI } from '../../api/index.js'
import Dashboard from '../Dashboard.vue'

const { authMock } = vi.hoisted(() => ({
  authMock: {
    kullanici: { id: 1, displayName: 'Ayşe Yılmaz', username: 'ayse', role: 'ADMIN' },
    sirketAdi: 'RasPel A.Ş.',
    sirketId: 7,
    isAdmin: true,
    isSaha: false,
    isLoggedIn: true
  }
}))

vi.mock('../../api/index.js', () => ({
  dashboardAPI: {
    getData: vi.fn(),
    ozet: vi.fn(),
    gorevler: vi.fn(),
    grafikler: vi.fn(),
    akis: vi.fn(),
    icgoruler: vi.fn(),
    vadeler: vi.fn(),
    karAnalizi: vi.fn(),
    tercihGetir: vi.fn(),
    tercihKaydet: vi.fn()
  },
  onaylarAPI: { bekleyen: vi.fn(), karar: vi.fn() },
  excelAPI: { dashboard: vi.fn() },
  stokAPI: { kritik: vi.fn() },
  networkStatus: { online: true, showBanner: false, sunucuyaUlasilamiyor: false },
  faturaAPI: { create: vi.fn() },
  teslimatAPI: { olustur: vi.fn() }
}))

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => authMock
}))

vi.mock('../../stores/dovizStore.js', () => ({
  useDovizStore: () => ({
    kurlar: [],
    loading: false,
    kurlariYukle: vi.fn().mockResolvedValue([])
  })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  useRoute: () => ({ path: '/', fullPath: '/' })
}))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div class="stub-bar" />' },
  Line: { name: 'Line', props: ['data', 'options'], template: '<div class="stub-line" />' },
  Doughnut: { name: 'Doughnut', props: ['data', 'options'], template: '<div class="stub-doughnut" />' }
}))

vi.mock('../../composables/useWebSocket.js', async () => {
  const { ref } = await import('vue')
  return {
    aboneOl: vi.fn(() => vi.fn()),
    bagli: ref(false),
    yenidenBaglandi: ref(0)
  }
})

import { aboneOl, bagli, yenidenBaglandi } from '../../composables/useWebSocket.js'
import { CANLI_ANAHTAR } from '../../composables/useDashboardCanli.js'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const ozetVerisi = {
  hero: { kullaniciAd: 'Ayşe Yılmaz', sirketAd: 'RasPel A.Ş.', canli: true },
  bugun: { tahsilat: 1000, odeme: 500, siparis: 1, teslimat: 1 },
  kpi: {
    likidite: { deger: 120000, kasa: 20000, banka: 100000, sparkline: [1, 2] },
    netCari: { deger: 50000, alacak: 90000, borc: 40000 },
    ciro: { deger: 300000, hedef: 500000, yuzde: 60 },
    kar: { deger: 80000, hedef: 100000, marj: 26.6 }
  },
  stok: { cesit: 42, deger: 250000, kritikAdet: 3 },
  gorevSayilari: { onayBekleyen: 2, vadesiGecen: 1, kritikStok: 3 }
}

const mountEt = () =>
  mount(Dashboard, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: { 'router-link': RouterLinkStub, DashboardAkis: true }
    }
  })

const varsayilanMocklar = () => {
  dashboardAPI.getData.mockResolvedValue({ data: {} })
  dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi })
  dashboardAPI.gorevler.mockResolvedValue({ data: [] })
  dashboardAPI.grafikler.mockResolvedValue({ data: { gunlukNakitAkisi: [] } })
  dashboardAPI.akis.mockResolvedValue({ data: [] })
  dashboardAPI.icgoruler.mockResolvedValue({ data: [] })
  dashboardAPI.vadeler.mockResolvedValue({ data: [] })
  dashboardAPI.karAnalizi.mockResolvedValue({ data: {} })
  dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
  dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
  onaylarAPI.bekleyen.mockResolvedValue({ data: [] })
  onaylarAPI.karar.mockResolvedValue({ data: { basarili: 1 } })
}

const olayGonder = async (tur, mesaj = 'Değişiklik') => {
  const geriCagri = aboneOl.mock.calls[0]?.[1]
  expect(geriCagri).toBeTypeOf('function')
  geriCagri({ body: JSON.stringify({ tur, mesaj, zaman: '2026-11-05T10:00:00Z' }) })
  await nextTick()
}

const ilerle = async (ms) => {
  vi.advanceTimersByTime(ms)
  await flushPromises()
}

const gorunurluk = (deger) => {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => deger
  })
}

describe('Dashboard — canli STOMP guncellemesi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authMock.kullanici.role = 'ADMIN'
    authMock.isAdmin = true
    authMock.isSaha = false
    authMock.sirketId = 7
    bagli.value = false
    yenidenBaglandi.value = 0
    aboneOl.mockReset()
    aboneOl.mockImplementation(() => vi.fn())
    varsayilanMocklar()
    vi.useFakeTimers({
      toFake: ['setTimeout', 'clearTimeout', 'setInterval', 'clearInterval', 'Date']
    })
  })

  afterEach(() => {
    vi.useRealTimers()
    delete document.visibilityState
    vi.clearAllMocks()
  })

  it('olaylari 800 ms debounce ile getOzet + getAkis olarak birlestirir', async () => {
    const wrapper = mountEt()
    await flushPromises()
    expect(aboneOl).toHaveBeenCalledWith('/topic/dashboard/7', expect.any(Function))

    dashboardAPI.ozet.mockClear()
    dashboardAPI.akis.mockClear()
    dashboardAPI.grafikler.mockClear()

    await olayGonder('HAREKET')
    await olayGonder('KASA')
    await olayGonder('BANKA')

    await ilerle(799)
    expect(dashboardAPI.ozet).not.toHaveBeenCalled()
    expect(dashboardAPI.akis).not.toHaveBeenCalled()

    await ilerle(1)
    expect(dashboardAPI.ozet).toHaveBeenCalledTimes(1)
    expect(dashboardAPI.ozet).toHaveBeenCalledWith({ donem: 'BU_AY' })
    expect(dashboardAPI.akis).toHaveBeenCalledTimes(1)
    expect(dashboardAPI.grafikler).not.toHaveBeenCalled()
    expect(wrapper.find('p.sr-only').text()).toBe(i18n.global.t('dashboard.canliGuncellendi'))

    wrapper.unmount()
  })

  it('grafik olaylarinda getGrafikler en fazla 5 saniyede bir calisir', async () => {
    const wrapper = mountEt()
    await flushPromises()
    dashboardAPI.grafikler.mockClear()

    await olayGonder('FATURA')
    await ilerle(800)
    expect(dashboardAPI.grafikler).toHaveBeenCalledTimes(1)

    // 5 sn dolmadan gelen grafik olayi grafik istegini tekrarlamaz.
    await olayGonder('SIPARIS')
    await ilerle(800)
    expect(dashboardAPI.grafikler).toHaveBeenCalledTimes(1)

    await ilerle(4000)
    await olayGonder('STOK')
    await ilerle(800)
    expect(dashboardAPI.grafikler).toHaveBeenCalledTimes(2)

    wrapper.unmount()
  })

  it('sekme gizliyken bekletir, gorunur olunca hemen tazeler', async () => {
    const wrapper = mountEt()
    await flushPromises()
    dashboardAPI.ozet.mockClear()

    gorunurluk('hidden')
    await olayGonder('STOK')
    await ilerle(800)
    expect(dashboardAPI.ozet).not.toHaveBeenCalled()

    gorunurluk('visible')
    document.dispatchEvent(new Event('visibilitychange'))
    await flushPromises()
    expect(dashboardAPI.ozet).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })

  it('canli mod kapaliyken abonelik kurmaz ve olay yenilemez', async () => {
    localStorage.setItem(CANLI_ANAHTAR, '0')
    const wrapper = mountEt()
    await flushPromises()

    expect(aboneOl).not.toHaveBeenCalled()
    expect(wrapper.find('[data-test="oto-yenileme"]').attributes('aria-checked')).toBe('false')

    dashboardAPI.ozet.mockClear()
    await ilerle(120_000)
    expect(dashboardAPI.ozet).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('anahtar kapatilinca bekleyen canli olaylari yenilemez', async () => {
    const wrapper = mountEt()
    await flushPromises()
    const geriCagri = aboneOl.mock.calls[0][1]

    await wrapper.find('[data-test="oto-yenileme"]').trigger('click')
    await nextTick()
    expect(localStorage.getItem(CANLI_ANAHTAR)).toBe('0')

    dashboardAPI.ozet.mockClear()
    geriCagri({ body: '{"tur":"FATURA","mesaj":"x","zaman":"z"}' })
    await nextTick()
    await ilerle(800)
    expect(dashboardAPI.ozet).not.toHaveBeenCalled()

    wrapper.unmount()
  })

  it('STOMP kopukken 60 sn yedek polling calisir', async () => {
    const wrapper = mountEt()
    await flushPromises()
    dashboardAPI.ozet.mockClear()

    await ilerle(60_000)
    expect(dashboardAPI.ozet).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })

  it('yeniden baglaninca tam yenileme yapar', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await flushPromises()
    dashboardAPI.ozet.mockClear()
    dashboardAPI.gorevler.mockClear()
    dashboardAPI.icgoruler.mockClear()

    yenidenBaglandi.value = 1
    await flushPromises()

    expect(dashboardAPI.ozet).toHaveBeenCalledTimes(1)
    expect(dashboardAPI.gorevler).toHaveBeenCalledTimes(1)
    expect(dashboardAPI.icgoruler).toHaveBeenCalledTimes(1)

    wrapper.unmount()
  })
})
