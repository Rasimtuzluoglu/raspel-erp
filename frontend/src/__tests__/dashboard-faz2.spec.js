import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import ToastService from 'primevue/toastservice'
import i18n from '../i18n.js'
import { dashboardAPI } from '../api/index.js'
import Dashboard from '../views/Dashboard.vue'
import App from '../App.vue'
import {
  DASHBOARD_WIDGET_ANAHTARLARI,
  ROL_SIRALARI,
  SABIT_ANAHTARLARI,
  WIDGET_SPANLARI,
  duzenGecerliMi,
  duzenNormallestir,
  rolDuzeni,
  widgetBirak,
  widgetGorunurlukDegistir,
  widgetSpan,
  widgetTasi
} from '../utils/dashboardDuzen.js'

const { authMock } = vi.hoisted(() => ({
  authMock: {
    kullanici: { id: 1, displayName: 'Ayşe Yılmaz', username: 'ayse', role: 'ADMIN' },
    sirketAdi: 'RasPel A.Ş.',
    isAdmin: true,
    isSaha: false,
    isLoggedIn: true
  }
}))

vi.mock('../api/index.js', () => ({
  dashboardAPI: {
    getData: vi.fn(),
    ozet: vi.fn(),
    gorevler: vi.fn(),
    grafikler: vi.fn(),
    akis: vi.fn(),
    icgoruler: vi.fn(),
    tercihGetir: vi.fn(),
    tercihKaydet: vi.fn()
  },
  stokAPI: { kritik: vi.fn() },
  networkStatus: { online: true, showBanner: false, sunucuyaUlasilamiyor: false },
  faturaAPI: { create: vi.fn() },
  teslimatAPI: { olustur: vi.fn() }
}))

vi.mock('../stores/authStore.js', () => ({
  useAuthStore: () => authMock
}))

vi.mock('../stores/dovizStore.js', () => ({
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

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const ozetVerisi = {
  hero: { kullaniciAd: 'Ayşe Yılmaz', sirketAd: 'RasPel A.Ş.', canli: true },
  bugun: { tahsilat: 1000, odeme: 500, siparis: 1, teslimat: 1 },
  kpi: {
    likidite: { deger: 120000, kasa: 20000, banka: 100000, trendYuzde: 4.2, sparkline: [1, 2] },
    netCari: { deger: 50000, alacak: 90000, borc: 40000 },
    ciro: { deger: 300000, hedef: 500000, yuzde: 60 },
    kar: { deger: 80000, hedef: 100000, marj: 26.6 }
  },
  stok: { cesit: 42, deger: 250000, kritikAdet: 3 },
  gorevSayilari: { onayBekleyen: 1, vadesiGecen: 0, kritikStok: 0, bekleyenTeslimat: 0, vadesiYaklasan: 0 }
}

const mountDashboard = () =>
  mount(Dashboard, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

const gridSirasi = (wrapper) =>
  wrapper.findAll('.db-grid [data-widget]').map((w) => w.attributes('data-widget'))

const duzenModuAc = async (wrapper) => {
  await wrapper.find('.ust-buton[aria-pressed="false"]').trigger('click')
  await nextTick()
}

describe('Dashboard faz2 — registry ve rol varsayılanları', () => {
  it('registry düz listeyi ve sabit bantları tanımlar (vade/hedefler yok)', () => {
    expect(DASHBOARD_WIDGET_ANAHTARLARI).toEqual([
      'kpi',
      'bugunSerit',
      'gorevler',
      'icgoruler',
      'nakitAkisi',
      'hizliIslemler',
      'aylikGelirGider',
      'kritikStok',
      'kategoriSatislari',
      'enCokSatanlar',
      'akis'
    ])
    expect(SABIT_ANAHTARLARI).toEqual(['kpi', 'bugunSerit'])
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('vade')
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('hedefler')
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('boyut')
  })

  it('kart genişlikleri anahtara göre sabittir (8+4, 8+4, 8+4, 4+4+4)', () => {
    expect(widgetSpan('gorevler')).toBe(8)
    expect(widgetSpan('icgoruler')).toBe(4)
    expect(widgetSpan('nakitAkisi')).toBe(8)
    expect(widgetSpan('hizliIslemler')).toBe(4)
    expect(widgetSpan('aylikGelirGider')).toBe(8)
    expect(widgetSpan('kritikStok')).toBe(4)
    expect(widgetSpan('kategoriSatislari')).toBe(4)
    expect(widgetSpan('enCokSatanlar')).toBe(4)
    expect(widgetSpan('akis')).toBe(4)
    expect(Object.keys(WIDGET_SPANLARI)).toHaveLength(9)
  })

  it('ADMIN tüm bölümleri spec sırasıyla görür', () => {
    const duzen = rolDuzeni('ADMIN')
    expect(duzen.versiyon).toBe(1)
    expect(duzen.sira.map((w) => w.key)).toEqual(DASHBOARD_WIDGET_ANAHTARLARI)
    expect(duzen.sira.every((w) => w.gorunur === true)).toBe(true)
    expect(duzen.sira.every((w) => !('boyut' in w))).toBe(true)
  })

  it('tüm roller aynı sabit yerleşimi kullanır', () => {
    expect(rolDuzeni('MUHASEBE').sira.map((w) => w.key)).toEqual(ROL_SIRALARI.ADMIN)
    expect(ROL_SIRALARI.USER).toEqual(ROL_SIRALARI.ADMIN)
  })

  it('bilinmeyen rol ADMIN varsayılanına düşer', () => {
    expect(rolDuzeni('DRIVER').sira.map((w) => w.key)).toEqual(ROL_SIRALARI.ADMIN)
  })
})

describe('Dashboard faz2 — düzen normalizasyonu', () => {
  it('geçersiz düzeni rol varsayılanına çevirir', () => {
    expect(duzenGecerliMi(null)).toBe(false)
    expect(duzenGecerliMi({ sira: [{ key: 'yok' }] })).toBe(false)
    expect(duzenNormallestir(null, 'USER').sira.map((w) => w.key)).toEqual(ROL_SIRALARI.USER)
  })

  it('bilinmeyen/çift kayıtları atar, eksikleri gizli ekler ve boyutu temizler', () => {
    const duzen = duzenNormallestir(
      {
        versiyon: 1,
        sira: [
          { key: 'akis', boyut: 2, gorunur: true },
          { key: 'kpi', boyut: 9, gorunur: true },
          { key: 'kpi', boyut: 1, gorunur: true },
          { key: 'bilinmeyen', boyut: 1, gorunur: true }
        ]
      },
      'ADMIN'
    )
    const anahtarlar = duzen.sira.map((w) => w.key)
    // kpi ve bugunSerit her zaman başta.
    expect(anahtarlar[0]).toBe('kpi')
    expect(anahtarlar[1]).toBe('bugunSerit')
    expect(anahtarlar[2]).toBe('akis')
    expect(anahtarlar.filter((k) => k === 'kpi')).toHaveLength(1)
    expect(anahtarlar).not.toContain('bilinmeyen')
    expect(duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    expect(duzen.sira.find((w) => w.key === 'gorevler').gorunur).toBe(false)
  })

  it('kaldırılan hedefler/vade anahtarlarını temizler', () => {
    const duzen = duzenNormallestir(
      {
        versiyon: 1,
        sira: [
          { key: 'hedefler', boyut: 2, gorunur: true },
          { key: 'vade', boyut: 1, gorunur: true },
          { key: 'kpi', boyut: 2, gorunur: true },
          { key: 'gorevler', boyut: 1, gorunur: true }
        ]
      },
      'ADMIN'
    )
    const anahtarlar = duzen.sira.map((w) => w.key)
    expect(anahtarlar).not.toContain('hedefler')
    expect(anahtarlar).not.toContain('vade')
    expect(anahtarlar).toEqual(expect.arrayContaining(DASHBOARD_WIDGET_ANAHTARLARI))
    expect(duzen.sira.every((w) => !('boyut' in w))).toBe(true)
  })

  it('taşıma düz listede çalışır, sabit bantlar taşınmaz', () => {
    const sira = rolDuzeni('ADMIN').sira
    // gorevler -> aşağı: icgoruler ile yer değiştirir.
    expect(widgetTasi(sira, 'gorevler', 1).map((w) => w.key).indexOf('gorevler')).toBe(3)
    // Sabit bantlar taşınamaz.
    expect(widgetTasi(sira, 'kpi', 1)).toBe(sira)
    expect(widgetTasi(sira, 'bugunSerit', -1)).toBe(sira)
    // Sabitlerin üzerinden atlayarak yukarı: icgoruler -> gorevler'in yerine.
    expect(widgetTasi(sira, 'icgoruler', -1).map((w) => w.key).indexOf('icgoruler')).toBe(2)
    expect(sira[0].key).toBe('kpi') // kaynak dizi değişmez
  })

  it('görünürlük ve sürükle-bırak yardımcıları yeni dizi döndürür', () => {
    const sira = rolDuzeni('ADMIN').sira
    expect(widgetGorunurlukDegistir(sira, 'kpi', false)[0].gorunur).toBe(false)
    expect(widgetBirak(sira, 'akis', 'kpi')).toBe(sira) // sabit hedef
    expect(widgetBirak(sira, 'kpi', 'akis')).toBe(sira) // sabit kaynak
    expect(widgetBirak(sira, 'akis', 'akis')).toBe(sira)
    const yeni = widgetBirak(sira, 'akis', 'nakitAkisi')
    expect(yeni.findIndex((w) => w.key === 'akis')).toBe(4)
  })
})

describe('Dashboard faz2 — düzen kaydetme ve geri dönüş', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authMock.kullanici.role = 'ADMIN'
    authMock.isAdmin = true
    dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi })
    dashboardAPI.gorevler.mockResolvedValue({ data: [] })
    dashboardAPI.grafikler.mockResolvedValue({ data: {} })
    dashboardAPI.akis.mockResolvedValue({ data: [] })
    dashboardAPI.icgoruler.mockResolvedValue({ data: [] })
    dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('backend tercihi varsa onu uygular', async () => {
    dashboardAPI.tercihGetir.mockResolvedValue({
      data: {
        duzen: {
          versiyon: 1,
          sira: [
            { key: 'kpi', gorunur: true },
            { key: 'bugunSerit', gorunur: true },
            { key: 'akis', gorunur: true },
            { key: 'gorevler', gorunur: true }
          ]
        }
      }
    })
    const wrapper = mountDashboard()
    await flushPromises()
    expect(gridSirasi(wrapper)[0]).toBe('akis')
    wrapper.unmount()
  })

  it('backend boşsa ve kaydedilememiş yerel düzen varsa onu kullanır', async () => {
    localStorage.setItem(
      'raspel_dashboard_duzen',
      JSON.stringify({
        duzen: {
          versiyon: 1,
          sira: [
            { key: 'kpi', gorunur: true },
            { key: 'bugunSerit', gorunur: true },
            { key: 'gorevler', gorunur: true }
          ]
        },
        kaydedilemedi: true
      })
    )
    const wrapper = mountDashboard()
    await flushPromises()
    expect(gridSirasi(wrapper)[0]).toBe('gorevler')
    wrapper.unmount()
  })

  it('tercih yoksa rol varsayılanı ile çizilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    expect(wrapper.find('[data-test="kpi-grid"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="bugun-seridi"]').exists()).toBe(true)
    expect(gridSirasi(wrapper)).toHaveLength(DASHBOARD_WIDGET_ANAHTARLARI.length - 2)
    wrapper.unmount()
  })

  it('düzen modunda gizleme ve sıralama kaydedilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await duzenModuAc(wrapper)

    const panel = wrapper.find('[data-test="duzen-paneli"]')
    expect(panel.exists()).toBe(true)
    expect(wrapper.find('.db-grid [data-widget="gorevler"]').classes()).toContain('dw--duzen')

    // gizle
    await wrapper.find('.db-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.db-grid [data-widget="icgoruler"]').exists()).toBe(false)

    // yukarı taşı: akis bir üst sıraya geçer
    const oncekiSira = gridSirasi(wrapper)
    await wrapper.find('.db-grid [data-widget="akis"] [aria-label*="Yukarı"]').trigger('click')
    expect(gridSirasi(wrapper).indexOf('akis')).toBe(oncekiSira.indexOf('akis') - 1)

    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledTimes(1)
    const gonderilen = dashboardAPI.tercihKaydet.mock.calls[0][0]
    expect(gonderilen.duzen.versiyon).toBe(1)
    expect(gonderilen.duzen.sira.find((w) => w.key === 'icgoruler').gorunur).toBe(false)
    expect(gonderilen.duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    expect(gonderilen.duzen.sira.findIndex((w) => w.key === 'akis')).toBe(
      gonderilen.duzen.sira.findIndex((w) => w.key === 'enCokSatanlar') - 1
    )

    expect(wrapper.find('[data-test="duzen-paneli"]').exists()).toBe(false)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(i18n.global.t('dashboard.duzenKaydedildi'))
    wrapper.unmount()
  })

  it('sürükle-bırak ile düz listede sıralama değişir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await duzenModuAc(wrapper)

    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }
    await wrapper.find('.db-grid [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.db-grid [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()

    expect(gridSirasi(wrapper).indexOf('akis')).toBe(gridSirasi(wrapper).indexOf('nakitAkisi') - 1)
    wrapper.unmount()
  })

  it('kaydetme başarısızsa düzeni localStorage\'a yazar', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.db-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    const kayit = JSON.parse(localStorage.getItem('raspel_dashboard_duzen'))
    expect(kayit.kaydedilemedi).toBe(true)
    expect(kayit.duzen.sira.find((w) => w.key === 'icgoruler').gorunur).toBe(false)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(
      i18n.global.t('dashboard.duzenYerelKaydedildi')
    )
    wrapper.unmount()
  })

  it('rol varsayılanına sıfırla taslağı geri alır', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.db-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.db-grid [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    expect(wrapper.find('.db-grid [data-widget="icgoruler"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('düzen modundan iptal değişiklikleri geri alır', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.db-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')

    expect(wrapper.find('.db-grid [data-widget="icgoruler"]').exists()).toBe(true)
    expect(dashboardAPI.tercihKaydet).not.toHaveBeenCalled()
    wrapper.unmount()
  })
})

describe('Dashboard faz2 — App.vue arama olayı', () => {
  it('raspel:hizli-ara olayı QuickSearch panelini açar', async () => {
    const QuickSearchStub = {
      name: 'QuickSearch',
      props: ['visible'],
      template: '<div class="qs-stub" :data-visible="String(visible)" />'
    }
    const SlotStub = { template: '<div><slot /></div>' }
    const wrapper = mount(App, {
      global: {
        plugins: [createPinia(), i18n, ToastService],
        stubs: {
          AppSidebar: true,
          OturumSayaci: true,
          ErrorBoundary: SlotStub,
          AppBreadcrumb: true,
          GuncellemeNotlari: true,
          GeriAlToast: true,
          KisayolRehberi: true,
          MobilAltMenu: true,
          Toast: true,
          ConfirmDialog: true,
          'router-view': true,
          QuickSearch: QuickSearchStub
        }
      }
    })
    await nextTick()
    expect(wrapper.find('.qs-stub').attributes('data-visible')).toBe('false')

    window.dispatchEvent(new CustomEvent('raspel:hizli-ara'))
    await nextTick()
    expect(wrapper.find('.qs-stub').attributes('data-visible')).toBe('true')

    wrapper.unmount()
    // Unmount sonrası dinleyici kalmamalı.
    window.dispatchEvent(new CustomEvent('raspel:hizli-ara'))
  })
})
