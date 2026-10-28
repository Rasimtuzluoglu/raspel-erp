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
  ROL_SIRALARI,
  duzenGecerliMi,
  duzenNormallestir,
  rolDuzeni,
  widgetBirak,
  widgetBoyutDegistir,
  widgetGorunurlukDegistir,
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

const duzenSirasi = (wrapper) =>
  wrapper.findAll('.dashboard-grid [data-widget]').map((w) => w.attributes('data-widget'))

describe('Dashboard faz2 — rol varsayılanları', () => {
  it('ADMIN tüm widget\'ları spec sırasıyla görür', () => {
    const duzen = rolDuzeni('ADMIN')
    expect(duzen.versiyon).toBe(1)
    expect(duzen.sira.map((w) => w.key)).toEqual([
      'kpi',
      'gorevler',
      'icgoruler',
      'hedefler',
      'nakitAkisi',
      'aylikGelirGider',
      'akis',
      'kategoriSatislari',
      'enCokSatanlar',
      'vade',
      'kritikStok',
      'hizliIslemler'
    ])
    expect(duzen.sira.every((w) => w.gorunur === true)).toBe(true)
  })

  it('MUHASEBE finans odaklı sıraya sahiptir', () => {
    expect(rolDuzeni('MUHASEBE').sira.map((w) => w.key)).toEqual(ROL_SIRALARI.MUHASEBE)
    expect(ROL_SIRALARI.MUHASEBE.indexOf('hedefler')).toBeLessThan(ROL_SIRALARI.MUHASEBE.indexOf('akis'))
  })

  it('USER rolünde hedefler ve vade yoktur', () => {
    const anahtarlar = rolDuzeni('USER').sira.map((w) => w.key)
    expect(anahtarlar).toEqual([
      'kpi',
      'gorevler',
      'akis',
      'hizliIslemler',
      'kritikStok',
      'enCokSatanlar',
      'icgoruler',
      'nakitAkisi'
    ])
    expect(anahtarlar).not.toContain('hedefler')
    expect(anahtarlar).not.toContain('vade')
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

  it('bilinmeyen/çift kayıtları atar, eksikleri gizli ekler', () => {
    const duzen = duzenNormallestir(
      {
        versiyon: 1,
        sira: [
          { key: 'kpi', boyut: 9, gorunur: true },
          { key: 'kpi', boyut: 1, gorunur: true },
          { key: 'bilinmeyen', boyut: 1, gorunur: true }
        ]
      },
      'ADMIN'
    )
    const anahtarlar = duzen.sira.map((w) => w.key)
    expect(anahtarlar[0]).toBe('kpi')
    expect(anahtarlar.filter((k) => k === 'kpi')).toHaveLength(1)
    expect(anahtarlar).not.toContain('bilinmeyen')
    expect(duzen.sira[0].boyut).toBe(1)
    expect(duzen.sira.find((w) => w.key === 'akis').gorunur).toBe(false)
  })

  it('taşıma, boyut ve görünürlük yardımcıları yeni dizi döndürür', () => {
    const sira = rolDuzeni('ADMIN').sira
    const tasinan = widgetTasi(sira, 'gorevler', -1)
    expect(tasinan[0].key).toBe('gorevler')
    expect(sira[0].key).toBe('kpi') // kaynak dizi değişmez

    expect(widgetBoyutDegistir(sira, 'kpi')[0].boyut).toBe(1)
    expect(widgetGorunurlukDegistir(sira, 'kpi', false)[0].gorunur).toBe(false)
    expect(widgetBirak(sira, 'akis', 'kpi')[0].key).toBe('akis')
    expect(widgetBirak(sira, 'akis', 'akis')).toBe(sira)
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
            { key: 'akis', boyut: 1, gorunur: true },
            { key: 'kpi', boyut: 2, gorunur: true }
          ]
        }
      }
    })
    const wrapper = mountDashboard()
    await flushPromises()
    expect(duzenSirasi(wrapper)[0]).toBe('akis')
    wrapper.unmount()
  })

  it('backend boşsa ve kaydedilememiş yerel düzen varsa onu kullanır', async () => {
    localStorage.setItem(
      'raspel_dashboard_duzen',
      JSON.stringify({
        duzen: {
          versiyon: 1,
          sira: [
            { key: 'gorevler', boyut: 1, gorunur: true },
            { key: 'kpi', boyut: 2, gorunur: true }
          ]
        },
        kaydedilemedi: true
      })
    )
    const wrapper = mountDashboard()
    await flushPromises()
    expect(duzenSirasi(wrapper)[0]).toBe('gorevler')
    wrapper.unmount()
  })

  it('tercih yoksa rol varsayılanı ile çizilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    expect(duzenSirasi(wrapper)[0]).toBe('kpi')
    expect(duzenSirasi(wrapper)).toHaveLength(ROL_SIRALARI.ADMIN.length)
    wrapper.unmount()
  })

  it('düzen modunda gizle/boyut/klavye sıralama çalışır ve kaydedilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[aria-pressed="false"]').trigger('click')
    expect(wrapper.find('[data-test="duzen-paneli"]').exists()).toBe(true)
    expect(wrapper.find('.dashboard-grid [data-widget="kpi"]').classes()).toContain('dw--duzen')

    // gizle
    await wrapper.find('.dashboard-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.dashboard-grid [data-widget="icgoruler"]').exists()).toBe(false)

    // boyut
    await wrapper.find('.dashboard-grid [data-widget="kpi"] .dw-buton--boyut').trigger('click')
    expect(wrapper.find('.dashboard-grid [data-widget="kpi"]').classes()).toContain('dw--1')

    // klavye (yukarı) ile sıralama
    const oncekiSira = duzenSirasi(wrapper)
    await wrapper.find('.dashboard-grid [data-widget="akis"] [aria-label*="Yukarı"]').trigger('click')
    const yeniSira = duzenSirasi(wrapper)
    expect(yeniSira.indexOf('akis')).toBe(oncekiSira.indexOf('akis') - 1)

    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledTimes(1)
    const gonderilen = dashboardAPI.tercihKaydet.mock.calls[0][0]
    expect(gonderilen.duzen.versiyon).toBe(1)
    const kpi = gonderilen.duzen.sira.find((w) => w.key === 'kpi')
    const icgoru = gonderilen.duzen.sira.find((w) => w.key === 'icgoruler')
    expect(kpi.boyut).toBe(1)
    expect(icgoru.gorunur).toBe(false)
    expect(gonderilen.duzen.sira.findIndex((w) => w.key === 'akis')).toBe(
      gonderilen.duzen.sira.findIndex((w) => w.key === 'aylikGelirGider') - 1
    )

    expect(wrapper.find('[data-test="duzen-paneli"]').exists()).toBe(false)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(i18n.global.t('dashboard.duzenKaydedildi'))
    wrapper.unmount()
  })

  it('sürükle-bırak ile sıralama değişir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await wrapper.find('[aria-pressed="false"]').trigger('click')

    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }
    await wrapper.find('.dashboard-grid [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.dashboard-grid [data-widget="kpi"]').trigger('drop', { dataTransfer })
    await nextTick()

    expect(duzenSirasi(wrapper)[0]).toBe('akis')
    wrapper.unmount()
  })

  it('kaydetme başarısızsa düzeni localStorage\'a yazar', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[aria-pressed="false"]').trigger('click')
    await wrapper.find('.dashboard-grid [data-widget="kpi"] .dw-buton--boyut').trigger('click')
    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    const kayit = JSON.parse(localStorage.getItem('raspel_dashboard_duzen'))
    expect(kayit.kaydedilemedi).toBe(true)
    expect(kayit.duzen.sira.find((w) => w.key === 'kpi').boyut).toBe(1)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(
      i18n.global.t('dashboard.duzenYerelKaydedildi')
    )
    wrapper.unmount()
  })

  it('rol varsayılanına sıfırla taslağı geri alır', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[aria-pressed="false"]').trigger('click')
    await wrapper.find('.dashboard-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.dashboard-grid [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    expect(wrapper.find('.dashboard-grid [data-widget="icgoruler"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('düzen modundan iptal değişiklikleri geri alır', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[aria-pressed="false"]').trigger('click')
    await wrapper.find('.dashboard-grid [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')

    expect(wrapper.find('.dashboard-grid [data-widget="icgoruler"]').exists()).toBe(true)
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
