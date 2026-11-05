import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import ToastService from 'primevue/toastservice'
import i18n from '../i18n.js'
import { dashboardAPI, excelAPI, notAPI } from '../api/index.js'
import Dashboard from '../views/Dashboard.vue'
import App from '../App.vue'
import {
  BOLGE_SAG,
  BOLGE_SOL,
  DASHBOARD_WIDGET_ANAHTARLARI,
  ROL_SIRALARI,
  SABIT_ANAHTARLARI,
  SAG_WIDGET_ANAHTARLARI,
  SOL_WIDGET_ANAHTARLARI,
  WIDGET_SPANLARI,
  duzenGecerliMi,
  duzenNormallestir,
  rolDuzeni,
  widgetBirak,
  widgetBolgesi,
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
  excelAPI: { dashboard: vi.fn() },
  notAPI: { getAll: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn() },
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

const solSirasi = (wrapper) =>
  wrapper.findAll('.db-sol > [data-widget]').map((w) => w.attributes('data-widget'))

const sagSirasi = (wrapper) =>
  wrapper.findAll('.db-sag > [data-widget]').map((w) => w.attributes('data-widget'))

const duzenModuAc = async (wrapper) => {
  await wrapper.find('.ust-buton[aria-pressed="false"]').trigger('click')
  await nextTick()
}

const bolgeSirasi = (sira, bolge) =>
  sira.filter((w) => widgetBolgesi(w.key) === bolge).map((w) => w.key)

describe('Dashboard faz2 — registry ve rol varsayılanları', () => {
  it('registry iki bölgeyi ve sabit bantları tanımlar (vade/hedefler yok)', () => {
    expect(DASHBOARD_WIDGET_ANAHTARLARI).toEqual([
      'kpi',
      'bugunSerit',
      ...SOL_WIDGET_ANAHTARLARI,
      ...SAG_WIDGET_ANAHTARLARI
    ])
    expect(SABIT_ANAHTARLARI).toEqual(['kpi', 'bugunSerit'])
    expect(SOL_WIDGET_ANAHTARLARI).toHaveLength(10)
    expect(SAG_WIDGET_ANAHTARLARI).toEqual([
      'gorevler',
      'hizliIslemler',
      'akis',
      'icgoruler',
      'siparisDurumlari',
      'butceGerceklesen'
    ])
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('vade')
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('hedefler')
    expect(DASHBOARD_WIDGET_ANAHTARLARI).not.toContain('boyut')
  })

  it('widget bölgeleri: SOL grafikler, SAG aksiyon, sabitler null', () => {
    expect(widgetBolgesi('nakitAkisi')).toBe(BOLGE_SOL)
    expect(widgetBolgesi('stokDegeri')).toBe(BOLGE_SOL)
    expect(widgetBolgesi('gorevler')).toBe(BOLGE_SAG)
    expect(widgetBolgesi('icgoruler')).toBe(BOLGE_SAG)
    expect(widgetBolgesi('kpi')).toBeNull()
    expect(widgetBolgesi('bugunSerit')).toBeNull()
    expect(widgetBolgesi('yok')).toBeNull()
  })

  it('kart genişlikleri sol 2 kolonlu ızgaraya göredir', () => {
    expect(widgetSpan('nakitAkisi')).toBe(2)
    expect(widgetSpan('stokDegeri')).toBe(2)
    expect(widgetSpan('ciroHedef')).toBe(1)
    expect(widgetSpan('aylikGelirGider')).toBe(1)
    expect(widgetSpan('kategoriSatislari')).toBe(1)
    expect(widgetSpan('enCokSatanlar')).toBe(1)
    expect(widgetSpan('gorevler')).toBe(1)
    expect(Object.keys(WIDGET_SPANLARI)).toHaveLength(17)
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

  it('bilinmeyen/çift kayıtları atar, eksikleri görünür ekler ve boyutu temizler', () => {
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
    // Sabitlerden sonra SOL, ardından SAG gelir (bölge toplama).
    expect(anahtarlar[2]).toBe('nakitAkisi')
    // SAG girdi sırası korunur: akis ilk sırada kalır.
    expect(anahtarlar.slice(-5)).toEqual([
      'gorevler',
      'hizliIslemler',
      'icgoruler',
      'siparisDurumlari',
      'butceGerceklesen'
    ])
    expect(anahtarlar.filter((k) => k === 'kpi')).toHaveLength(1)
    expect(anahtarlar).not.toContain('bilinmeyen')
    expect(duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    expect(duzen.sira.find((w) => w.key === 'akis').gorunur).toBe(true)
    expect(duzen.sira.find((w) => w.key === 'gorevler').gorunur).toBe(true)
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

  it('taşıma bölge içinde çalışır, sabit bantlar taşınmaz', () => {
    const sira = rolDuzeni('ADMIN').sira

    // SOL: nakit akışı bir adım aşağı -> ciro hedefi ile yer değiştirir.
    const solYeni = bolgeSirasi(widgetTasi(sira, 'nakitAkisi', 1), BOLGE_SOL)
    expect(solYeni.slice(0, 2)).toEqual(['ciroHedef', 'nakitAkisi'])

    // SAG: görevler aşağı -> hızlı işlemler başa geçer.
    const sagYeni = bolgeSirasi(widgetTasi(sira, 'gorevler', 1), BOLGE_SAG)
    expect(sagYeni.slice(0, 2)).toEqual(['hizliIslemler', 'gorevler'])

    // Sabit bantlar taşınamaz.
    expect(widgetTasi(sira, 'kpi', 1)).toBe(sira)
    expect(widgetTasi(sira, 'bugunSerit', -1)).toBe(sira)
    // Bölge ucu: son SAG widget'ı daha aşağı gidemez.
    expect(widgetTasi(sira, 'butceGerceklesen', 1)).toBe(sira)
    expect(sira[2].key).toBe('nakitAkisi') // kaynak dizi değişmez
  })

  it('görünürlük ve sürükle-bırak yardımcıları yeni dizi döndürür', () => {
    const sira = rolDuzeni('ADMIN').sira
    expect(widgetGorunurlukDegistir(sira, 'kpi', false)[0].gorunur).toBe(false)
    expect(widgetBirak(sira, 'akis', 'kpi')).toBe(sira) // sabit hedef
    expect(widgetBirak(sira, 'kpi', 'akis')).toBe(sira) // sabit kaynak
    expect(widgetBirak(sira, 'akis', 'akis')).toBe(sira)
    // Bölgeler arası taşıma reddedilir.
    expect(widgetBirak(sira, 'akis', 'nakitAkisi')).toBe(sira)
    expect(widgetBirak(sira, 'nakitAkisi', 'gorevler')).toBe(sira)
    // Aynı bölgede taşıma çalışır.
    const yeni = widgetBirak(sira, 'akis', 'gorevler')
    expect(bolgeSirasi(yeni, BOLGE_SAG).slice(0, 2)).toEqual(['akis', 'gorevler'])
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

  it('backend tercihi varsa bölge içi sırayı ondan alır', async () => {
    dashboardAPI.tercihGetir.mockResolvedValue({
      data: {
        duzen: {
          versiyon: 1,
          sira: [
            { key: 'kpi', gorunur: true },
            { key: 'bugunSerit', gorunur: true },
            { key: 'akis', gorunur: true },
            { key: 'gorevler', gorunur: true },
            { key: 'kasaBanka', gorunur: true }
          ]
        }
      }
    })
    const wrapper = mountDashboard()
    await flushPromises()
    expect(sagSirasi(wrapper).slice(0, 2)).toEqual(['akis', 'gorevler'])
    expect(solSirasi(wrapper)[0]).toBe('kasaBanka')
    expect(solSirasi(wrapper)).toHaveLength(SOL_WIDGET_ANAHTARLARI.length)
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
            { key: 'nakitAkisi', gorunur: true },
            { key: 'gorevler', gorunur: true }
          ]
        },
        kaydedilemedi: true
      })
    )
    const wrapper = mountDashboard()
    await flushPromises()
    expect(solSirasi(wrapper)[0]).toBe('nakitAkisi')
    expect(sagSirasi(wrapper)[0]).toBe('gorevler')
    wrapper.unmount()
  })

  it('tercih yoksa rol varsayılanı ile çizilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    expect(wrapper.find('[data-test="kpi-grid"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="bugun-seridi"]').exists()).toBe(true)
    expect(solSirasi(wrapper)).toEqual(SOL_WIDGET_ANAHTARLARI)
    expect(sagSirasi(wrapper)).toEqual(SAG_WIDGET_ANAHTARLARI)
    wrapper.unmount()
  })

  it('düzen modunda gizleme ve bölge içi sıralama kaydedilir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await duzenModuAc(wrapper)

    const panel = wrapper.find('[data-test="duzen-paneli"]')
    expect(panel.exists()).toBe(true)
    expect(wrapper.find('.db-sag [data-widget="gorevler"]').classes()).toContain('dw--duzen')

    // gizle
    await wrapper.find('.db-sag [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(false)

    // yukarı taşı: canlı akış bir üst sıraya geçer (hızlı işlemler ile yer değiştirir)
    const onceki = sagSirasi(wrapper)
    await wrapper.find('.db-sag [data-widget="akis"] [aria-label*="Yukarı"]').trigger('click')
    const sonraki = sagSirasi(wrapper)
    expect(sonraki.indexOf('akis')).toBe(onceki.indexOf('akis') - 1)

    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledTimes(1)
    const gonderilen = dashboardAPI.tercihKaydet.mock.calls[0][0]
    expect(gonderilen.duzen.versiyon).toBe(1)
    expect(gonderilen.duzen.sira.find((w) => w.key === 'icgoruler').gorunur).toBe(false)
    expect(gonderilen.duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    expect(gonderilen.duzen.sira.findIndex((w) => w.key === 'akis')).toBeLessThan(
      gonderilen.duzen.sira.findIndex((w) => w.key === 'icgoruler')
    )
    expect(gonderilen.duzen.sira[0].key).toBe('kpi')
    expect(gonderilen.duzen.sira[1].key).toBe('bugunSerit')
    // Sabitlerden sonra SOL, sonra SAG.
    expect(gonderilen.duzen.sira[2].key).toBe('nakitAkisi')
    expect(
      gonderilen.duzen.sira.findIndex((w) => w.key === 'gorevler')
    ).toBeGreaterThan(gonderilen.duzen.sira.findIndex((w) => w.key === 'stokDegeri'))

    expect(wrapper.find('[data-test="duzen-paneli"]').exists()).toBe(false)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(i18n.global.t('dashboard.duzenKaydedildi'))
    wrapper.unmount()
  })

  it('sürükle-bırak ile aynı bölgede sıralama değişir, bölgeler arası değişmez', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    await duzenModuAc(wrapper)

    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }
    const solOnce = solSirasi(wrapper)

    await wrapper.find('.db-sag [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.db-sol [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()
    expect(solSirasi(wrapper)).toEqual(solOnce)

    await wrapper.find('.db-sol [data-widget="stokDegeri"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.db-sol [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()
    expect(solSirasi(wrapper)[0]).toBe('stokDegeri')
    wrapper.unmount()
  })

  it('kaydetme başarısızsa düzeni localStorage\'a yazar', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.db-sag [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
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
    await wrapper.find('.db-sag [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('düzen modundan iptal değişiklikleri geri alır', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await duzenModuAc(wrapper)
    await wrapper.find('.db-sag [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')

    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(true)
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

describe('Dashboard faz2 — kişisel notlar, kısayol ve dışa aktarma entegrasyonu', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authMock.kullanici = { id: 1, displayName: 'Ayşe Yılmaz', username: 'ayse', role: 'ADMIN' }
    authMock.isAdmin = true
    authMock.isSaha = false
    authMock.isLoggedIn = true
    dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi })
    dashboardAPI.gorevler.mockResolvedValue({ data: [] })
    dashboardAPI.grafikler.mockResolvedValue({ data: {} })
    dashboardAPI.akis.mockResolvedValue({ data: [] })
    dashboardAPI.icgoruler.mockResolvedValue({ data: [] })
    dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
    excelAPI.dashboard.mockResolvedValue({ data: new Blob(['xlsx']) })
    notAPI.getAll.mockResolvedValue({ data: { content: [], totalElements: 0 } })
    notAPI.create.mockResolvedValue({ data: { id: 1 } })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('not ikonu drawer açar; eklenen notu /api/notlar üzerinden kaydeder', async () => {
    notAPI.getAll
      .mockResolvedValueOnce({ data: { content: [], totalElements: 0 } })
      .mockResolvedValueOnce({
        data: {
          content: [
            {
              id: 1,
              baslik: 'Müşteri toplantısı',
              icerik: '',
              onemDerecesi: 'NORMAL',
              renk: 'MAVI',
              olusturmaTarihi: '2026-11-05T08:00:00'
            }
          ],
          totalElements: 1
        }
      })

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-test="not-rozet"]').exists()).toBe(false)
    await wrapper.find('[data-test="not-buton"]').trigger('click')
    await flushPromises()
    await nextTick()

    const drawer = wrapper.find('[data-test="notlar-drawer"]')
    expect(drawer.exists()).toBe(true)
    expect(drawer.attributes('aria-modal')).toBe('true')
    expect(notAPI.getAll).toHaveBeenCalledWith({
      page: 0,
      size: 10,
      sort: 'olusturmaTarihi,desc'
    })
    expect(wrapper.find('[data-test="notlar-bos"]').exists()).toBe(true)

    await wrapper.find('[data-test="not-baslik"]').setValue('Müşteri toplantısı')
    await wrapper.find('.notlar-ekle').trigger('submit')
    await flushPromises()
    await nextTick()

    expect(notAPI.create).toHaveBeenCalledWith(
      expect.objectContaining({ baslik: 'Müşteri toplantısı' })
    )
    expect(wrapper.find('.notlar-oge').text()).toContain('Müşteri toplantısı')
    // Rozet kaldırıldı: not sayısı artık üst barda gösterilmez.
    expect(wrapper.find('[data-test="not-rozet"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('kısayol düzenleme modalı seçimi kaydeder ve şeridi günceller', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.findAll('.kisayol')).toHaveLength(8)
    await wrapper.find('[data-test="kisayol-duzenle"]').trigger('click')
    await nextTick()
    expect(wrapper.find('[data-test="kisayol-ayarlari"]').exists()).toBe(true)

    await wrapper.find('[data-test="kisayol-secim-kasa"]').setValue(false)
    await wrapper.find('[data-test="kisayol-secim-ajanda"]').setValue(true)
    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-test="kisayol-ayarlari"]').exists()).toBe(false)
    const anahtarlar = wrapper.findAll('.kisayol').map((k) => k.attributes('data-kisayol'))
    expect(anahtarlar).toContain('ajanda')
    expect(anahtarlar).not.toContain('kasa')
    expect(anahtarlar).toHaveLength(8)
    expect(JSON.parse(localStorage.getItem('raspel_dashboard_kisayollar'))).toContain('ajanda')
    wrapper.unmount()
  })

  it('ADMIN olmayan kullanıcı kokpit/yedekler kısayolunu görmez', async () => {
    authMock.kullanici = { id: 2, displayName: 'Ali', username: 'ali', role: 'USER' }
    authMock.isAdmin = false

    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="kisayol-duzenle"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-test="kisayol-secim-kokpit"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="kisayol-secim-yedekler"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="kisayol-secim-raporlar"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('Excel ikonu seçili dönemle indirir, yazdır ikonu print çağırır', async () => {
    const yazdir = vi.spyOn(window, 'print').mockImplementation(() => {})
    const tikla = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="excel-indir"]').trigger('click')
    await flushPromises()
    expect(excelAPI.dashboard).toHaveBeenCalledWith({ donem: 'BU_AY' })
    expect(tikla).toHaveBeenCalled()

    await wrapper.find('[data-test="yazdir"]').trigger('click')
    expect(yazdir).toHaveBeenCalledTimes(1)

    wrapper.unmount()
    yazdir.mockRestore()
    tikla.mockRestore()
  })
})
