import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n.js'
import { dashboardAPI } from '../../api/index.js'
import Dashboard from '../Dashboard.vue'

const { authMock } = vi.hoisted(() => ({
  authMock: {
    kullanici: { id: 1, displayName: 'Ayşe Yılmaz', username: 'ayse', role: 'ADMIN' },
    sirketAdi: 'RasPel A.Ş.',
    isAdmin: true,
    isSaha: false
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
    tercihGetir: vi.fn(),
    tercihKaydet: vi.fn()
  }
}))

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => authMock
}))

vi.mock('../../stores/dovizStore.js', () => ({
  useDovizStore: () => ({
    kurlar: [{ kod: 'USD', satisFiyati: 47.4 }],
    loading: false,
    kurlariYukle: vi.fn().mockResolvedValue([])
  })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ replace: vi.fn(), push: vi.fn() }),
  useRoute: () => ({ path: '/' })
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
  bugun: { tahsilat: 15000, odeme: 4000, siparis: 3, teslimat: 2 },
  kpi: {
    likidite: { deger: 120000, kasa: 20000, banka: 100000, trendYuzde: 4.2, sparkline: [1, 2, 3, 4] },
    netCari: { deger: 50000, alacak: 90000, borc: 40000 },
    ciro: { deger: 300000, hedef: 500000, yuzde: 60 },
    kar: { deger: 80000, hedef: 100000, marj: 26.6 }
  },
  stok: { cesit: 42, deger: 250000, kritikAdet: 3 },
  gorevSayilari: {
    onayBekleyen: 2,
    vadesiGecen: 1,
    kritikStok: 3,
    bekleyenTeslimat: 0,
    vadesiYaklasan: 2
  }
}

const gorevVerisi = [
  {
    tur: 'ONAY_IZIN',
    baslik: 'İzin onayı bekliyor',
    aciklama: '2 personel',
    adet: 2,
    tutar: 0,
    oncelik: 45,
    route: '/izinler',
    ikon: 'pi pi-calendar',
    renk: '#3b82f6'
  },
  {
    tur: 'VADESI_GECEN',
    baslik: 'Vadesi geçen alacak',
    aciklama: 'Gecikmiş',
    adet: 1,
    tutar: 12500,
    oncelik: 90,
    route: '/tahsilat',
    ikon: 'pi pi-exclamation-triangle',
    renk: '#ef4444'
  }
]

const grafikVerisi = {
  aylikGelirGider: [
    { ay: 'Eyl', gelir: 100, gider: 60 },
    { ay: 'Eki', gelir: 120, gider: 70 }
  ],
  gunlukNakitAkisi: [
    { gun: '2026-10-01', gelir: 10, gider: 5 },
    { gun: '2026-10-02', gelir: 12, gider: 7 }
  ],
  enCokSatanlar: [{ stokAd: 'Kalem A', stokKodu: 'KA', satisMiktari: 5 }],
  kategoriSatislari: [{ kategori: 'Kırtasiye', tutar: 900 }],
  alacakYaslandirma: [{ aralik: '0-30', tutar: 500 }],
  enCokBorcCariler: [{ cariAd: 'Borçlu Ltd.', tutar: 700 }],
  enCokAlacakCariler: [{ cariAd: 'Alacaklı A.Ş.', tutar: 900 }],
  kasaBanka: { kasa: 1000, banka: 4000 },
  hedef: { ciro: 500000, gerceklesen: 300000, kar: 80000, hedefKar: 100000 }
}

const akisVerisi = [
  {
    tur: 'TAHSILAT',
    baslik: 'Tahsilat alındı',
    aciklama: 'ABC Ltd.',
    tutar: 5000,
    tarih: new Date().toISOString(),
    route: '/hareketler',
    ikon: null,
    renk: null
  }
]

const icgoruVerisi = [
  { seviye: 'KRITIK', baslik: 'Geciken alacak arttı', mesaj: '30 günü aşan alacak var.', route: '/tahsilat', ikon: null },
  { seviye: 'WARN', baslik: 'Kritik stok', mesaj: '3 ürün minimumun altında.', route: '/stoklar', ikon: null }
]

const mountEt = () =>
  mount(Dashboard, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('Dashboard.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    authMock.kullanici.role = 'ADMIN'
    authMock.isAdmin = true
    authMock.isSaha = false
    dashboardAPI.getData.mockResolvedValue({ data: {} })
    dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi })
    dashboardAPI.gorevler.mockResolvedValue({ data: gorevVerisi })
    dashboardAPI.grafikler.mockResolvedValue({ data: grafikVerisi })
    dashboardAPI.akis.mockResolvedValue({ data: akisVerisi })
    dashboardAPI.icgoruler.mockResolvedValue({ data: icgoruVerisi })
    dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('hero: kullanıcı, şirket, canlı rozet ve bugün şeridini render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const hero = wrapper.find('.dashboard-hero')
    expect(hero.exists()).toBe(true)
    expect(hero.text()).toContain('Ayşe Yılmaz')
    expect(hero.text()).toContain('RasPel A.Ş.')
    expect(hero.text()).toContain(i18n.global.t('dashboard.canli'))
    expect(wrapper.findAll('[data-test="hero-bugun"]')).toHaveLength(4)
    expect(hero.text()).toContain('15.000,00')
    wrapper.unmount()
  })

  it('bento KPI kartlarını ve deltaları render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="kpi-grid"]').exists()).toBe(true)
    const likidite = wrapper.find('[data-test="kpi-likidite"]')
    expect(likidite.text()).toContain('120.000,00')
    expect(likidite.text()).toContain('4.2%')
    expect(wrapper.find('[data-test="kpi-net-cari"]').text()).toContain('90.000,00')
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('60')
    expect(wrapper.find('[data-test="kpi-kar"]').text()).toContain('26.6')
    wrapper.unmount()
  })

  it('görev merkezi ve içgörüleri render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const gorev = wrapper.find('[data-test="gorev-merkezi"]')
    expect(gorev.exists()).toBe(true)
    expect(gorev.findAll('.gorev-kart')).toHaveLength(2)
    expect(gorev.find('.gorev-kart--kritik').text()).toContain('Vadesi geçen alacak')

    const icgoru = wrapper.find('[data-test="icgoru-karti"]')
    expect(icgoru.findAll('.icgoru-satir')).toHaveLength(2)
    expect(icgoru.find('.icgoru-satir--kritik').exists()).toBe(true)
    wrapper.unmount()
  })

  it('hedefleri (ciro halkası, kâr çubuğu, 30/60/90 projeksiyonu) render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const hedefler = wrapper.find('[data-test="hedefler"]')
    expect(hedefler.exists()).toBe(true)
    expect(hedefler.findAll('.projeksiyon-kart')).toHaveLength(3)
    expect(hedefler.text()).toContain('30 gün')
    expect(hedefler.text()).toContain('90 gün')
    expect(hedefler.text()).toContain('%60')
    wrapper.unmount()
  })

  it('ana grafik kartlarını ve drill-down bağlantılarını render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const kartlar = wrapper.findAll('[data-test="grafik-karti"]')
    expect(kartlar).toHaveLength(4)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.grafikNakitAkisi'))
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.grafikAylikGelirGider'))

    const nakit = kartlar[0]
    expect(nakit.find('a').attributes('href')).toBe('/kasa')
    expect(kartlar[1].find('a').attributes('href')).toBe('/raporlar')
    expect(kartlar[2].find('a').attributes('href')).toBe('/raporlar')
    expect(kartlar[3].find('a').attributes('href')).toBe('/stoklar')
    wrapper.unmount()
  })

  it('canlı akışı render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const akis = wrapper.find('[data-test="canli-akis"]')
    expect(akis.exists()).toBe(true)
    expect(akis.findAll('.akis-satir')).toHaveLength(1)
    expect(akis.text()).toContain('Tahsilat alındı')
    wrapper.unmount()
  })

  it('hızlı işlemler 8 kısayol + arama butonu içerir; arama olayı yayar', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('raspel:hizli-ara', dinleyici)
    const wrapper = mountEt()
    await flushPromises()

    const hizli = wrapper.find('[data-test="hizli-islemler"]')
    // 8 hızlı işlem + (ADMIN için) yönetici kokpiti + arama butonu
    const kisayollar = hizli.findAll('.islem-karti').filter((k) => !k.classes('islem-karti--ara'))
    expect(kisayollar).toHaveLength(9)
    expect(hizli.find('.islem-karti--ara').exists()).toBe(true)
    await hizli.find('.islem-karti--ara').trigger('click')
    expect(dinleyici).toHaveBeenCalledTimes(1)
    window.removeEventListener('raspel:hizli-ara', dinleyici)
    wrapper.unmount()
  })

  it('progresif yükleme: önce iskelet, sonra veri gelir', async () => {
    let coz
    dashboardAPI.ozet.mockImplementation(
      () =>
        new Promise((resolve) => {
          coz = resolve
        })
    )
    const wrapper = mountEt()
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('.bento--skeleton')).toHaveLength(4)
    expect(wrapper.find('.dashboard-hero .bugun-skeleton').exists()).toBe(true)

    coz({ data: ozetVerisi })
    await flushPromises()

    expect(wrapper.findAll('.bento--skeleton')).toHaveLength(0)
    expect(wrapper.find('[data-test="kpi-likidite"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('boş görev listesinde boş durum gösterir', async () => {
    dashboardAPI.gorevler.mockResolvedValue({ data: [] })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="gorev-merkezi"] .gorev-durum--bos').exists()).toBe(true)
    wrapper.unmount()
  })

  it('a11y: canlı duyuru alanı ve etiketli butonlar', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const duyuru = wrapper.find('[aria-live="polite"]')
    expect(duyuru.exists()).toBe(true)
    expect(duyuru.attributes('role')).toBe('status')

    const yenile = wrapper.find(`[aria-label="${i18n.global.t('dashboard.yenile')}"]`)
    expect(yenile.exists()).toBe(true)
    expect(wrapper.find('.hero-buton--ara').attributes('aria-label')).toBe(
      i18n.global.t('dashboard.heroAra')
    )
    wrapper.unmount()
  })

  it('mobil kırılım sınıfları: 12 kolon grid ve geniş widget', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('.dashboard-grid').exists()).toBe(true)
    const kpiWidget = wrapper.find('[data-widget="kpi"]')
    expect(kpiWidget.classes()).toContain('dw--2')
    expect(wrapper.find('[data-widget="gorevler"]').classes()).toContain('dw--1')
    wrapper.unmount()
  })

  it('"Tüm analizler" açılınca genişletilmiş grafikleri gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.findAll('[data-test="grafik-karti"]')).toHaveLength(4)
    const buton = wrapper.find('.analiz-baslik')
    expect(buton.attributes('aria-expanded')).toBe('false')
    await buton.trigger('click')
    expect(buton.attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('[data-test="grafik-karti"]')).toHaveLength(10)
    expect(wrapper.find('a[href="/tahsilat"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('USER rolünde hedefler/vade gizli, analizler kapalı', async () => {
    authMock.kullanici.role = 'USER'
    authMock.isAdmin = false
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-widget="hedefler"]').exists()).toBe(false)
    expect(wrapper.find('[data-widget="vade"]').exists()).toBe(false)
    expect(wrapper.find('[data-widget="kpi"]').exists()).toBe(true)
    expect(wrapper.find('.analiz-baslik').exists()).toBe(false)
    wrapper.unmount()
  })

  it('tercih okuma isteğini bir kez yapar', async () => {
    const wrapper = mountEt()
    await flushPromises()
    expect(dashboardAPI.tercihGetir).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})
