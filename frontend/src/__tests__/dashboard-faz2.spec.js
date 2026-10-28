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
  RAY_ANAHTARLARI,
  ROL_SIRALARI,
  duzenGecerliMi,
  duzenNormallestir,
  rolDuzeni,
  widgetBirak,
  widgetBolgesi,
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

const anaSirasi = (wrapper) =>
  wrapper.findAll('.pano-ana [data-widget]').map((w) => w.attributes('data-widget'))

const raySirasi = (wrapper) =>
  wrapper.findAll('.pano-ray [data-widget]').map((w) => w.attributes('data-widget'))

const tumSirasi = (wrapper) => [...anaSirasi(wrapper), ...raySirasi(wrapper)]

const duzenModuAc = async (wrapper) => {
  await wrapper.find('.hero-buton[aria-pressed="false"]').trigger('click')
  await nextTick()
}

describe('Dashboard faz2 — registry ve rol varsayılanları', () => {
  it('registry bölgeleri ayırır (vade/hedefler yok)', () => {
    expect(DASHBOARD_WIDGET_ANAHTARLARI).toEqual([
      'kpi',
      'gorevler',
      'icgoruler',
      'nakitAkisi',
      'aylikGelirGider',
      'kategoriSatislari',
      'enCokSatanlar',
      'akis',
      'kritikStok',
      'hizliIslemler'
    ])
    expect(RAY_ANAHTARLARI).toEqual(['icgoruler', 'hizliIslemler', 'kritikStok'])
    expect(widgetBolgesi('gorevler')).toBe('ana')
    expect(widgetBolgesi('kritikStok')).toBe('ray')
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('vade')
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('hedefler')
  })

  it('ADMIN tüm bölümleri spec sırasıyla görür', () => {
    const duzen = rolDuzeni('ADMIN')
    expect(duzen.versiyon).toBe(1)
    expect(duzen.sira.map((w) => w.key)).toEqual([
      'kpi',
      'gorevler',
      'nakitAkisi',
      'aylikGelirGider',
      'kategoriSatislari',
      'enCokSatanlar',
      'akis',
      'icgoruler',
      'hizliIslemler',
      'kritikStok'
    ])
    expect(duzen.sira.every((w) => w.gorunur === true)).toBe(true)
    expect(duzen.sira.every((w) => !('boyut' in w))).toBe(true)
  })

  it('MUHASEBE finans odaklı sıraya sahiptir', () => {
    expect(rolDuzeni('MUHASEBE').sira.map((w) => w.key)).toEqual(ROL_SIRALARI.MUHASEBE)
    expect(ROL_SIRALARI.MUHASEBE.indexOf('nakitAkisi')).toBeLessThan(ROL_SIRALARI.MUHASEBE.indexOf('akis'))
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
    // kpi her zaman başta.
    expect(anahtarlar[0]).toBe('kpi')
    expect(anahtarlar[1]).toBe('akis')
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

  it('taşıma yalnızca aynı bölge içinde çalışır', () => {
    const sira = rolDuzeni('ADMIN').sira
    // akis -> bir üst (ana içerik komşusu enCokSatanlar)
    expect(widgetTasi(sira, 'akis', -1).map((w) => w.key).indexOf('akis')).toBe(5)
    // Ray bölgesinde yukarı: hizliIslemler -> icgoruler'in yerine
    expect(widgetTasi(sira, 'hizliIslemler', -1).map((w) => w.key).indexOf('hizliIslemler')).toBe(7)
    // Bölgenin ilk elemanı daha yukarı çıkmaz (değişmez dizi).
    expect(widgetTasi(sira, 'gorevler', -1)).toBe(sira)
    // kpi taşınamaz.
    expect(widgetTasi(sira, 'kpi', 1)).toBe(sira)
    expect(sira[0].key).toBe('kpi') // kaynak dizi değişmez
  })

  it('görünürlük ve sürükle-bırak yardımcıları yeni dizi döndürür', () => {
    const sira = rolDuzeni('ADMIN').sira
    expect(widgetGorunurlukDegistir(sira, 'kpi', false)[0].gorunur).toBe(false)
    expect(widgetBirak(sira, 'akis', 'kpi')).toBe(sira) // kpi sabit
    expect(widgetBirak(sira, 'akis', 'kritikStok')).toBe(sira) // bölgeler arası
    expect(widgetBirak(sira, 'akis', 'akis')).toBe(sira)
    const ayniBolge = widgetBirak(sira, 'akis', 'nakitAkisi')
    expect(ayniBolge.findIndex((w) => w.key === 'akis')).toBe(2)
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
            { key: 'akis', gorunur: true },
            { key: 'gorevler', gorunur: true }
          ]
        }
      }
    })
    const wrapper = mountDashboard()
    await flushPromises()
    expect(anaSirasi(wrapper)[0]).toBe('akis')
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
            { key: 'gorevler', gorunur: true }
          ]
        },
        kaydedilemedi: true
      })
    )
    const wrapper = mountDashboard()
    await flushPromises()
    expect(anaSirasi(wrapper)[0]).toBe('gorevler')
    wrapper.unmount()
  })

  it('tercih yoksa rol varsayılanı ile çizilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    expect(wrapper.find('[data-widget="kpi"]').exists()).toBe(true)
    expect(tumSirasi(wrapper)).toHaveLength(ROL_SIRALARI.ADMIN.length - 1)
    wrapper.unmount()
  })

  it('düzen modunda gizleme ve bölge içi sıralama kaydedilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await duzenModuAc(wrapper)

    const panel = wrapper.find('[data-test="duzen-paneli"]')
    expect(panel.exists()).toBe(true)
    expect(wrapper.find('.pano-ana [data-widget="gorevler"]').classes()).toContain('dw--duzen')

    // gizle
    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(false)

    // aynı bölge içinde yukarı taşıma
    const oncekiSira = anaSirasi(wrapper)
    await wrapper.find('.pano-ana [data-widget="akis"] [aria-label*="Yukarı"]').trigger('click')
    expect(anaSirasi(wrapper).indexOf('akis')).toBe(oncekiSira.indexOf('akis') - 1)

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

  it('sürükle-bırak ile aynı bölgede sıralama değişir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await duzenModuAc(wrapper)

    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }
    await wrapper.find('.pano-ana [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.pano-ana [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()

    expect(anaSirasi(wrapper)[1]).toBe('akis')
    wrapper.unmount()
  })

  it('kaydetme başarısızsa düzeni localStorage\'a yazar', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
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
    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('düzen modundan iptal değişiklikleri geri alır', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')

    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(true)
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
