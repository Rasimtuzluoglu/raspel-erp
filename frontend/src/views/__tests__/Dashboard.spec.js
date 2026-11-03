import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import { dashboardAPI, stokAPI } from '../../api/index.js'
import Dashboard from '../Dashboard.vue'
import { SAG_WIDGET_ANAHTARLARI, SOL_WIDGET_ANAHTARLARI } from '../../utils/dashboardDuzen.js'

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
  },
  stokAPI: { kritik: vi.fn() }
}))

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => authMock
}))

vi.mock('../../stores/dovizStore.js', () => ({
  useDovizStore: () => ({
    kurlar: [
      { kod: 'USD', satisFiyati: 47.4 },
      { kod: 'EUR', satisFiyati: 51.3 },
      { kod: 'GBP', satisFiyati: 60.55 },
      { kod: 'SAR', satisFiyati: 12.65 },
      { kod: 'GAU', satisFiyati: 4230 },
      { kod: 'BTC', satisFiyati: 0 }
    ],
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
  stok: { cesit: 42, deger: 250000, kritikAdet: 5 },
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
    tur: 'ONAY_SIPARIS',
    baslik: 'Sipariş onayı bekliyor',
    aciklama: '4 sipariş',
    adet: 4,
    tutar: 0,
    oncelik: 80,
    route: '/onaylar?tur=SIPARIS',
    ikon: 'pi pi-shopping-cart',
    renk: '#f59e0b'
  },
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
    route: null,
    ikon: 'pi pi-exclamation-triangle',
    renk: '#ef4444'
  },
  {
    tur: 'VADESI_YAKLASAN',
    baslik: 'Vadesi yaklaşan alacak',
    aciklama: '7 gün içinde',
    adet: 2,
    tutar: 8000,
    oncelik: 60,
    route: null,
    ikon: 'pi pi-clock',
    renk: '#f59e0b'
  }
]

// Grafik son 30 gunu bugune gore tamamlar; mock tarihleri de bugune gore
// uretilir ki 7 gunluk dilimde veri kalsin.
const gunlukNakit = Array.from({ length: 12 }, (_, i) => {
  const bugun = new Date()
  const tarih = new Date(bugun.getFullYear(), bugun.getMonth(), bugun.getDate() - (11 - i))
  const gun = `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, '0')}-${String(tarih.getDate()).padStart(2, '0')}`
  return {
    gun,
    gelir: i === 0 ? 0 : 10 + i,
    gider: i === 0 ? 0 : 5 + i
  }
})

const grafikVerisi = {
  aylikGelirGider: [
    { ay: '2026-09', gelir: 100, gider: 60 },
    { ay: '2026-10', gelir: 120, gider: 70 }
  ],
  gunlukNakitAkisi: gunlukNakit,
  enCokSatanlar: [{ stokAd: 'Kalem A', stokKodu: 'KA', satisMiktari: 5 }],
  kategoriSatislari: [{ kategori: 'Kırtasiye', tutar: 900 }],
  alacakYaslandirma: [{ aralik: '0-30', tutar: 500 }],
  enCokBorcCariler: [{ cariAd: 'Borçlu Ltd.', tutar: 700 }],
  enCokAlacakCariler: [{ cariAd: 'Alacaklı A.Ş.', tutar: 900 }],
  kasaBanka: { kasa: 1000, banka: 4000 },
  siparisDurumlari: [
    { durum: 'TEKLIF', adet: 2 },
    { durum: 'SIPARIS', adet: 5 },
    { durum: 'YOLDA', adet: 1 }
  ],
  butceGerceklesen: [
    { kategori: 'Pazarlama', butce: 10000, gerceklesen: 7000 },
    { kategori: 'Kira', butce: 5000, gerceklesen: 5200 }
  ],
  stokDegeri: {
    toplamDeger: 1250000,
    kritikAdet: 7,
    kategoriler: [
      { kategori: 'Elektronik', deger: 900000, adet: 4 },
      { kategori: 'Kırtasiye', deger: 350000, adet: 12 }
    ]
  }
}

const akisVerisi = Array.from({ length: 7 }, (_, i) => ({
  tur: 'TAHSILAT',
  baslik: `Tahsilat ${i + 1}`,
  aciklama: 'ABC Ltd.',
  tutar: 5000,
  tarih: new Date().toISOString(),
  route: '/hareketler',
  ikon: null,
  renk: null
}))

const icgoruVerisi = [
  { seviye: 'KRITIK', baslik: 'Geciken alacak arttı', mesaj: '30 günü aşan alacak var.', route: '/tahsilat', ikon: null },
  { seviye: 'WARN', baslik: 'Kritik stok', mesaj: '3 ürün minimumun altında.', route: '/stoklar', ikon: null },
  { seviye: 'INFO', baslik: 'Nakit akışı', mesaj: 'Bu hafta pozitif.', route: '/kasa', ikon: null },
  { seviye: 'INFO', baslik: 'Dördüncü', mesaj: 'Gösterilmez.', route: '/kasa', ikon: null }
]

const kritikStokVerisi = [
  { id: 1, stokKodu: 'KA-1', ad: 'Kalem A', miktar: 1, minMiktar: 10 },
  { id: 2, stokKodu: 'KA-2', ad: 'Kalem B', miktar: 2, minMiktar: 10 },
  { id: 3, stokKodu: 'KA-3', ad: 'Kalem C', miktar: 3, minMiktar: 10 },
  { id: 4, stokKodu: 'KA-4', ad: 'Kalem D', miktar: 4, minMiktar: 10 },
  { id: 5, stokKodu: 'KA-5', ad: 'Kalem E', miktar: 5, minMiktar: 10 }
]

const mountEt = () =>
  mount(Dashboard, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

const solSira = (wrapper) =>
  wrapper.findAll('.db-sol > [data-widget]').map((w) => w.attributes('data-widget'))

const sagSira = (wrapper) =>
  wrapper.findAll('.db-sag > [data-widget]').map((w) => w.attributes('data-widget'))

const duzenModuAc = async (wrapper) => {
  await wrapper.find('.ust-buton[aria-pressed="false"]').trigger('click')
  await nextTick()
}

const tercihMockla = (duzen) => {
  dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen } })
}

const varsayilanMocklar = () => {
  dashboardAPI.getData.mockResolvedValue({ data: {} })
  dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi })
  dashboardAPI.gorevler.mockResolvedValue({ data: gorevVerisi })
  dashboardAPI.grafikler.mockResolvedValue({ data: grafikVerisi })
  dashboardAPI.akis.mockResolvedValue({ data: akisVerisi })
  dashboardAPI.icgoruler.mockResolvedValue({ data: icgoruVerisi })
  dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
  dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
  stokAPI.kritik.mockResolvedValue({ data: kritikStokVerisi })
}

const SOL_SIRASI = [
  'nakitAkisi',
  'ciroHedef',
  'aylikGelirGider',
  'alacakYaslandirma',
  'kasaBanka',
  'kategoriSatislari',
  'enCokSatanlar',
  'enCokBorcCariler',
  'enCokAlacakCariler',
  'stokDegeri'
]

const SAG_SIRASI = ['gorevler', 'hizliIslemler', 'akis', 'icgoruler', 'siparisDurumlari', 'butceGerceklesen']

describe('Dashboard.vue — finans masası yerleşimi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authMock.kullanici.role = 'ADMIN'
    authMock.isAdmin = true
    authMock.isSaha = false
    varsayilanMocklar()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('sade üst bar kullanır: selamlama + şirket/tarih, dev saat ve ticker yok', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const ust = wrapper.find('.ust-bar')
    expect(ust.exists()).toBe(true)
    expect(ust.text()).toContain('Ayşe Yılmaz')
    expect(ust.text()).toContain('RasPel A.Ş.')
    expect(ust.text()).toContain('2026')
    expect(ust.text()).toContain(i18n.global.t('dashboard.canli'))
    expect(ust.find('.ust-buton--ara').exists()).toBe(true)
    expect(ust.text()).toContain('Ctrl K')

    expect(wrapper.find('.pano').exists()).toBe(false)
    expect(wrapper.find('.dashboard-hero').exists()).toBe(false)
    expect(wrapper.find('.hero-saat').exists()).toBe(false)
    expect(wrapper.find('[data-test="hero-bugun"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('piyasa şeridinde en fazla 4 para birimi gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const serit = wrapper.find('[data-test="piyasa-seridi"]')
    expect(serit.exists()).toBe(true)
    const kalemler = serit.findAll('.piyasa-kalem')
    expect(kalemler).toHaveLength(4)
    expect(kalemler.map((k) => k.find('.piyasa-kod').text())).toEqual(['USD', 'EUR', 'GBP', 'BTC'])
    // BTC kuru 0 olduğu için "—" gösterilir (yapay değer üretilmez).
    expect(kalemler[3].text()).toContain('—')
    expect(serit.text()).not.toContain('SAR')
    wrapper.unmount()
  })

  it('üstte 8 işlemli tek satır kısayol şeridi render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const serit = wrapper.find('[data-test="kisayol-seridi"]')
    expect(serit.exists()).toBe(true)
    expect(serit.element.tagName).toBe('NAV')
    expect(serit.attributes('aria-label')).toBe(i18n.global.t('dashboard.kisayolSeridi'))
    expect(serit.classes()).toContain('kisayol-serit')

    const baglantilar = serit.findAll('a')
    expect(baglantilar).toHaveLength(8)
    expect(baglantilar.map((a) => a.attributes('href'))).toEqual([
      '/faturalar',
      '/hareketler',
      '/hareketler',
      '/hizli-satis',
      '/cari-hesaplar',
      '/kasa',
      '/stoklar',
      '/teklifler'
    ])
    expect(baglantilar[0].text()).toContain(i18n.global.t('dashboard.kisayolYeniFatura'))
    expect(baglantilar[0].find('.kisayol-ikon i').exists()).toBe(true)
    // Kısayol şeridi KPI bandından önce gelir.
    const kpi = wrapper.find('[data-test="kpi-grid"]')
    expect(serit.element.compareDocumentPosition(kpi.element)).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
    wrapper.unmount()
  })

  it('9 KPI kartını 5 + 4 düzeninde render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const kpi = wrapper.find('[data-test="kpi-grid"]')
    expect(kpi.exists()).toBe(true)
    expect(kpi.findAll('.kpi-kart')).toHaveLength(9)

    expect(wrapper.find('[data-test="kpi-deger-likidite"]').text()).toContain('120.000,00')
    expect(wrapper.find('[data-test="kpi-deger-likidite"]').text()).toContain('20.000,00')
    expect(wrapper.find('[data-test="kpi-deger-alacak"]').text()).toContain('90.000,00')
    expect(wrapper.find('[data-test="kpi-deger-borc"]').text()).toContain('40.000,00')
    expect(wrapper.find('[data-test="kpi-deger-ciro"]').text()).toContain('300.000,00')
    expect(wrapper.find('[data-test="kpi-deger-kar"]').text()).toContain('+')
    expect(wrapper.find('[data-test="kpi-deger-kar"]').text()).toContain('80.000,00')
    expect(wrapper.find('[data-test="kpi-deger-tahsilat"]').text()).toContain('15.000,00')
    expect(wrapper.find('[data-test="kpi-deger-odeme"]').text()).toContain('4.000,00')
    expect(wrapper.find('[data-test="kpi-deger-bekleyen-siparis"]').text()).toBe('4')
    expect(wrapper.find('[data-test="kpi-deger-stok-degeri"]').text()).toContain('1.250.000,00')
    expect(wrapper.find('[data-test="kpi-stok-degeri"]').text()).toContain('7')
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('%60')
    expect(wrapper.find('[data-test="kpi-kar"]').text()).toContain('26.6')
    // Likidite kartında yüzde "trend" yoktur; son 7 gün sparkline'ı gösterilir.
    expect(wrapper.find('[data-test="kpi-likidite"] .kpi-spark').exists()).toBe(true)
    expect(wrapper.find('.kpi-delta').exists()).toBe(false)
    wrapper.unmount()
  })

  it('bugün şeridini KPI kartlarının hemen altında 4 hücre olarak render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const serit = wrapper.find('[data-test="bugun-seridi"]')
    expect(serit.exists()).toBe(true)
    expect(serit.element.children).toHaveLength(4)
    expect(serit.text()).toContain('15.000,00')
    expect(serit.text()).toContain('4.000,00')

    const kpi = wrapper.find('[data-test="kpi-grid"]')
    const govde = wrapper.find('[data-test="dashboard-govde"]')
    expect(serit.element.compareDocumentPosition(kpi.element)).toBe(Node.DOCUMENT_POSITION_PRECEDING)
    expect(serit.element.compareDocumentPosition(govde.element)).toBe(Node.DOCUMENT_POSITION_FOLLOWING)
    wrapper.unmount()
  })

  it('gövdeyi iki bölgeye ayırır: sol grafik duvarı (10) + sağ aksiyon paneli (6)', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const govde = wrapper.find('[data-test="dashboard-govde"]')
    expect(govde.exists()).toBe(true)

    const sol = wrapper.find('.db-sol')
    const sag = wrapper.find('.db-sag')
    expect(sol.attributes('data-bolge')).toBe('SOL')
    expect(sag.attributes('data-bolge')).toBe('SAG')
    expect(sol.attributes('aria-label')).toBe(i18n.global.t('dashboard.bolgeGrafikler'))
    expect(sag.attributes('aria-label')).toBe(i18n.global.t('dashboard.bolgeAksiyon'))

    expect(solSira(wrapper)).toEqual(SOL_SIRASI)
    expect(sagSira(wrapper)).toEqual(SAG_SIRASI)

    // Sabit span: nakit ve stok tam satır, diğer grafikler tek kolon.
    expect(wrapper.find('.db-sol [data-widget="nakitAkisi"]').classes()).toContain('db-kart-alan--2')
    expect(wrapper.find('.db-sol [data-widget="stokDegeri"]').classes()).toContain('db-kart-alan--2')
    expect(wrapper.find('.db-sol [data-widget="kategoriSatislari"]').classes()).toContain('db-kart-alan--1')
    expect(wrapper.find('.db-sag [data-widget="gorevler"]').classes()).toContain('db-kart-alan--1')
    wrapper.unmount()
  })

  it('11 grafik kartı + nakit akışı kartını aynı başlık diliyle render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    // Nakit akışı ayrı bileşendir; `grafik-karti` sayısı 11'dir.
    expect(wrapper.findAll('[data-test="grafik-karti"]')).toHaveLength(11)
    expect(wrapper.find('.db-sol .dw').exists()).toBe(true)
    expect(wrapper.find('[data-widget="nakitAkisi"] [data-test="nakit-karti"]').exists()).toBe(true)

    const beklenenBasliklar = [
      ['ciroHedef', 'dashboard.grafikCiroHedef'],
      ['aylikGelirGider', 'dashboard.grafikAylikGelirGider'],
      ['alacakYaslandirma', 'dashboard.grafikYaslandirma'],
      ['kasaBanka', 'dashboard.grafikKasaBanka'],
      ['kategoriSatislari', 'dashboard.grafikKategori'],
      ['enCokSatanlar', 'dashboard.grafikEnCokSatan'],
      ['enCokBorcCariler', 'dashboard.grafikBorcCariler'],
      ['enCokAlacakCariler', 'dashboard.grafikAlacakCariler'],
      ['siparisDurumlari', 'dashboard.grafikSiparisDurumlari'],
      ['butceGerceklesen', 'dashboard.grafikButce'],
      ['stokDegeri', 'dashboard.grafikStokDegeri']
    ]
    for (const [key, anahtar] of beklenenBasliklar) {
      const kart = wrapper.find(`[data-widget="${key}"]`)
      expect(kart.exists(), key).toBe(true)
      expect(kart.find('.db-kart-baslik').text()).toContain(i18n.global.t(anahtar))
    }

    expect(wrapper.find('[data-widget="kategoriSatislari"] .stub-doughnut').exists()).toBe(true)
    expect(wrapper.find('[data-widget="kasaBanka"] .stub-doughnut').exists()).toBe(true)
    expect(wrapper.find('[data-widget="enCokSatanlar"] .stub-bar').exists()).toBe(true)
    wrapper.unmount()
  })

  it('yeni grafik verilerini (sipariş, bütçe, stok) doğru işler', async () => {
    const wrapper = mountEt()
    await flushPromises()

    // Sipariş durumları: adede göre azalan, durum etiketleri çeviriden.
    const siparis = wrapper
      .find('[data-widget="siparisDurumlari"]')
      .findComponent({ name: 'Bar' })
      .props('data')
    expect(siparis.labels).toEqual([
      i18n.global.t('dashboard.siparisDurumSiparis'),
      i18n.global.t('dashboard.siparisDurumTeklif'),
      i18n.global.t('dashboard.siparisDurumYolda')
    ])
    expect(siparis.datasets[0].data).toEqual([5, 2, 1])

    // Bütçe / gerçekleşen: iki veri seti, kategori ekseni.
    const butce = wrapper
      .find('[data-widget="butceGerceklesen"]')
      .findComponent({ name: 'Bar' })
      .props('data')
    expect(butce.labels).toEqual(['Pazarlama', 'Kira'])
    expect(butce.datasets).toHaveLength(2)
    expect(butce.datasets[0].label).toBe(i18n.global.t('dashboard.butceEtiket'))
    expect(butce.datasets[0].data).toEqual([10000, 5000])
    expect(butce.datasets[1].label).toBe(i18n.global.t('dashboard.gerceklesenEtiket'))
    expect(butce.datasets[1].data).toEqual([7000, 5200])

    // Stok değeri: kategori bazlı bar + kart altı toplam/kritik özeti.
    const stok = wrapper
      .find('[data-widget="stokDegeri"]')
      .findComponent({ name: 'Bar' })
      .props('data')
    expect(stok.labels).toEqual(['Elektronik', 'Kırtasiye'])
    expect(stok.datasets[0].data).toEqual([900000, 350000])
    const stokKart = wrapper.find('[data-widget="stokDegeri"]')
    expect(stokKart.text()).toContain(i18n.global.t('dashboard.stokDegeriToplam'))
    expect(stokKart.text()).toContain('1.250.000,00')
    expect(stokKart.text()).toContain(i18n.global.t('dashboard.stokDegeriKritik'))
    expect(stokKart.find('.stok-ozet').text()).toContain('7')
    wrapper.unmount()
  })

  it('boş grafik verisinde sahte değer üretmez; 11 kompakt boş durum gösterir', async () => {
    dashboardAPI.grafikler.mockResolvedValue({
      data: {
        ...grafikVerisi,
        gunlukNakitAkisi: [],
        aylikGelirGider: [],
        enCokSatanlar: [],
        kategoriSatislari: [],
        alacakYaslandirma: [],
        enCokBorcCariler: [],
        enCokAlacakCariler: [],
        kasaBanka: { kasa: 0, banka: 0 },
        siparisDurumlari: [],
        butceGerceklesen: [],
        stokDegeri: { toplamDeger: 0, kritikAdet: 0, kategoriler: [] }
      }
    })
    const wrapper = mountEt()
    await flushPromises()

    const bosDurumlar = wrapper.findAll('[data-test="grafik-karti"] .grafik-bos')
    expect(bosDurumlar).toHaveLength(11)
    for (const bos of bosDurumlar) {
      expect(bos.attributes('style') || '').toContain('120px')
    }
    expect(wrapper.findAll('.stub-bar')).toHaveLength(0)
    expect(wrapper.findAll('.stub-doughnut')).toHaveLength(0)
    expect(wrapper.find('[data-widget="nakitAkisi"] [data-test="nakit-bos"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('nakit akışı verisi varken 160px grafik + gerçekleşen etiketi gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const kart = wrapper.find('[data-widget="nakitAkisi"]')
    expect(kart.find('[data-test="nakit-grafik"]').exists()).toBe(true)
    expect(kart.find('.stub-line').exists()).toBe(true)
    expect(kart.text()).toContain(i18n.global.t('dashboard.nakitGerceklesen'))
    expect(kart.find('[data-test="nakit-bos"]').exists()).toBe(false)

    const stil = kart.find('[data-test="nakit-grafik"]').attributes('style') || ''
    expect(stil).toContain('160px')
    const cipler = kart.findAll('[data-test="nakit-projeksiyon-cip"]')
    expect(cipler).toHaveLength(3)
    expect(cipler[0].text()).toContain(i18n.global.t('dashboard.tahmini'))
    expect(cipler[0].text()).toContain('30 gün')
    expect(cipler[2].text()).toContain('90 gün')
    wrapper.unmount()
  })

  it('nakit akışı boşsa grafik çizmez, kompakt boş durum gösterir', async () => {
    dashboardAPI.grafikler.mockResolvedValue({
      data: { ...grafikVerisi, gunlukNakitAkisi: [] }
    })
    const wrapper = mountEt()
    await flushPromises()

    const kart = wrapper.find('[data-widget="nakitAkisi"]')
    expect(kart.find('.stub-line').exists()).toBe(false)
    expect(kart.find('[data-test="nakit-grafik"]').exists()).toBe(false)
    const bos = kart.find('[data-test="nakit-bos"]')
    expect(bos.exists()).toBe(true)
    expect(bos.text()).toContain(i18n.global.t('dashboard.nakitBosBaslik'))
    expect(bos.text()).toContain(i18n.global.t('dashboard.nakitBosAlt'))
    expect(bos.find('a').attributes('href')).toBe('/kasa')
    expect(bos.attributes('style') || '').not.toContain('160px')
    expect(kart.findAll('[data-test="nakit-projeksiyon-cip"]')).toHaveLength(3)
    wrapper.unmount()
  })

  it('nakit akışında 7/30 gün geçişi veriyi dilimler', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const nakitKart = wrapper.find('[data-widget="nakitAkisi"]')
    const otuz = nakitKart.findComponent({ name: 'Line' }).props('data')
    expect(otuz.labels).toHaveLength(30)

    await wrapper.find('[data-test="nakit-segment-7"]').trigger('click')
    await nextTick()

    const yedi = wrapper.find('[data-widget="nakitAkisi"]').findComponent({ name: 'Line' }).props('data')
    expect(yedi.labels).toHaveLength(7)
    expect(wrapper.find('[data-test="nakit-segment-7"]').classes()).toContain('nakit-segment-buton--aktif')
    wrapper.unmount()
  })

  it('sağ panelde 6 bölümü spec sırasıyla gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(sagSira(wrapper)).toEqual(SAG_SIRASI)
    expect(wrapper.find('.db-sag [data-widget="gorevler"]').text()).toContain(
      i18n.global.t('dashboard.bugunYapilacaklar')
    )
    expect(wrapper.find('.db-sag [data-widget="hizliIslemler"]').text()).toContain(
      i18n.global.t('dashboard.hizliIslemler')
    )
    expect(wrapper.find('.db-sag [data-widget="akis"]').text()).toContain(
      i18n.global.t('dashboard.akisBaslik')
    )
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').text()).toContain(
      i18n.global.t('dashboard.icgoruler')
    )
    expect(wrapper.find('.db-sag [data-widget="siparisDurumlari"]').exists()).toBe(true)
    expect(wrapper.find('.db-sag [data-widget="butceGerceklesen"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('görev satırlarında boş kolon yoktur ve aksiyon bağlantıları doğrudur', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const gorev = wrapper.find('[data-test="gorev-merkezi"]')
    expect(gorev.findAll('.gorev-satir')).toHaveLength(4)

    const gecen = wrapper.find('[data-test="gorev-VADESI_GECEN"]')
    expect(gecen.element.children).toHaveLength(4)
    expect(gecen.find('.gorev-eylem').attributes('href')).toBe('/tahsilat')
    expect(gecen.find('.gorev-eylem').text()).toContain(i18n.global.t('dashboard.gorevTahsilatLink'))
    expect(gecen.text()).toContain('12.500,00')

    const onay = wrapper.find('[data-test="gorev-ONAY_IZIN"]')
    expect(onay.find('.gorev-eylem').attributes('href')).toBe('/onaylar?tur=IZIN')
    wrapper.unmount()
  })

  it('içgörüleri en fazla 3 satır olarak gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const kart = wrapper.find('[data-widget="icgoruler"]')
    expect(kart.findAll('.icgoru-satir')).toHaveLength(3)
    expect(kart.text()).not.toContain('Dördüncü')
    expect(kart.find('.icgoru-satir--kritik').exists()).toBe(true)
    wrapper.unmount()
  })

  it('hızlı işlemler yalnız mevcut rotaları 6 kısayolla sunar', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const hizli = wrapper.find('[data-test="hizli-islemler"]')
    const baglantilar = hizli.findAll('a').map((a) => a.attributes('href'))
    expect(baglantilar).toEqual([
      '/satinalma',
      '/onaylar',
      '/raporlar',
      '/yonetici-kokpiti',
      '/ajanda',
      '/yedekler'
    ])
    expect(hizli.findAll('.islem-karti')).toHaveLength(6)
    wrapper.unmount()
  })

  it('hızlı işlemlerde yönetici kokpiti ve yedekler yalnız ADMIN için görünür', async () => {
    authMock.isAdmin = false
    const wrapper = mountEt()
    await flushPromises()

    const hizli = wrapper.find('[data-test="hizli-islemler"]')
    expect(hizli.find('a[href="/yonetici-kokpiti"]').exists()).toBe(false)
    expect(hizli.find('a[href="/yedekler"]').exists()).toBe(false)
    expect(hizli.findAll('a')).toHaveLength(4)
    expect(hizli.findAll('a').map((a) => a.attributes('href'))).toEqual([
      '/satinalma',
      '/onaylar',
      '/raporlar',
      '/ajanda'
    ])
    wrapper.unmount()
  })

  it('canlı akışta son 5 olayı gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const akis = wrapper.find('[data-test="canli-akis"]')
    expect(akis.findAll('.akis-satir')).toHaveLength(5)
    expect(akis.text()).toContain('Tahsilat 1')
    expect(akis.text()).not.toContain('Tahsilat 6')
    wrapper.unmount()
  })

  it('kritik stok ayrı kart olarak tekrarlanmaz (görev listesinde kalır)', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-widget="kritikStok"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('null/eksik KPI değerlerinde "—" gösterir, sıfır uydurmaz', async () => {
    dashboardAPI.ozet.mockResolvedValue({
      data: {
        ...ozetVerisi,
        kpi: {
          likidite: { deger: null, kasa: null, banka: undefined, trendYuzde: null },
          netCari: { alacak: null, borc: null },
          ciro: { deger: null, hedef: null, yuzde: null },
          kar: { deger: null, hedef: null, marj: null }
        }
      }
    })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="kpi-deger-likidite"]').text()).toBe('—')
    expect(wrapper.find('[data-test="kpi-deger-alacak"]').text()).toBe('—')
    expect(wrapper.find('[data-test="kpi-deger-borc"]').text()).toBe('—')
    expect(wrapper.find('[data-test="kpi-deger-ciro"]').text()).toBe('—')
    expect(wrapper.find('[data-test="kpi-deger-kar"]').text()).toBe('—')
    expect(wrapper.findAll('.kpi-hedef-yok')).toHaveLength(2)
    expect(wrapper.find('[data-test="kpi-likidite"] .kpi-delta').exists()).toBe(false)
    wrapper.unmount()
  })

  it('boş görev listesinde "Bekleyen iş yok" gösterir ve bekleyen sipariş 0 kalır', async () => {
    dashboardAPI.gorevler.mockResolvedValue({ data: [] })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="gorev-merkezi"] .gorev-durum--bos').exists()).toBe(true)
    expect(wrapper.find('.gorev-durum--bos').text()).toContain(i18n.global.t('dashboard.gorevBos'))
    expect(wrapper.find('[data-test="kpi-deger-bekleyen-siparis"]').text()).toBe('0')
    wrapper.unmount()
  })

  it('görev hatasında "Yeniden dene" ile tekrar yükler', async () => {
    dashboardAPI.gorevler.mockRejectedValueOnce(new Error('kapalı'))
    const wrapper = mountEt()
    await flushPromises()

    const hata = wrapper.find('.gorev-durum--hata')
    expect(hata.exists()).toBe(true)
    expect(hata.text()).toContain(i18n.global.t('dashboard.gorevHata'))

    const once = dashboardAPI.gorevler.mock.calls.length
    dashboardAPI.gorevler.mockResolvedValueOnce({ data: gorevVerisi })
    await hata.find('.gorev-dene').trigger('click')
    await flushPromises()
    expect(dashboardAPI.gorevler.mock.calls.length).toBe(once + 1)
    expect(wrapper.findAll('.gorev-satir')).toHaveLength(4)
    wrapper.unmount()
  })

  it('progresif yükleme: önce 9 KPI iskeleti, sonra veri gelir', async () => {
    let coz
    dashboardAPI.ozet.mockImplementation(
      () =>
        new Promise((resolve) => {
          coz = resolve
        })
    )
    const wrapper = mountEt()
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('.kpi-kart--iskelet')).toHaveLength(9)

    coz({ data: ozetVerisi })
    await flushPromises()

    expect(wrapper.findAll('.kpi-kart--iskelet')).toHaveLength(0)
    expect(wrapper.find('[data-test="kpi-deger-likidite"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('a11y: canlı duyuru, bölge etiketleri ve etiketli butonlar korunur', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const duyuru = wrapper.find('[aria-live="polite"]')
    expect(duyuru.exists()).toBe(true)
    expect(duyuru.attributes('role')).toBe('status')

    const yenile = wrapper.find(`[aria-label="${i18n.global.t('dashboard.yenile')}"]`)
    expect(yenile.exists()).toBe(true)
    expect(wrapper.find('.ust-buton--ara').attributes('aria-label')).toBe(
      i18n.global.t('dashboard.heroAra')
    )
    expect(wrapper.find('.nakit-segment').attributes('role')).toBe('group')
    expect(wrapper.find('.kisayol-serit').attributes('aria-label')).toBe(
      i18n.global.t('dashboard.kisayolSeridi')
    )
    expect(wrapper.find('.db-sol').attributes('role')).toBe('group')
    expect(wrapper.find('[data-test="kpi-grid"]').attributes('role')).toBe('group')
    wrapper.unmount()
  })

  it('tercih okuma isteğini bir kez yapar', async () => {
    const wrapper = mountEt()
    await flushPromises()
    expect(dashboardAPI.tercihGetir).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('1280/390 sınıf yapısı: iki bölge, sabit span sınıfları ve tek kolon kuralları', async () => {
    const wrapper = mountEt()
    await flushPromises()

    // Yerleşim inline stil ile değil sınıflarla kurulur (medya sorguları CSS'te).
    const govde = wrapper.find('.db-govde')
    expect(govde.attributes('style')).toBeUndefined()
    expect(wrapper.find('.db-sol').attributes('style')).toBeUndefined()
    expect(wrapper.find('.db-sag').attributes('style')).toBeUndefined()
    expect(wrapper.findAll('.db-sol > [data-widget]')).toHaveLength(10)
    expect(wrapper.findAll('.db-sag > [data-widget]')).toHaveLength(6)
    expect(wrapper.find('.db-sol [data-widget="nakitAkisi"]').classes()).toContain('db-kart-alan--2')
    expect(wrapper.find('.db-sol [data-widget="stokDegeri"]').classes()).toContain('db-kart-alan--2')
    wrapper.unmount()
  })
})

describe('Dashboard.vue — kişiselleştirme', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authMock.kullanici.role = 'ADMIN'
    authMock.isAdmin = true
    authMock.isSaha = false
    varsayilanMocklar()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('düzen paneli sabit bantları ve iki bölge grubunu gösterir, boyut kontrolü yoktur', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    const panel = wrapper.find('[data-test="duzen-paneli"]')
    expect(panel.exists()).toBe(true)
    expect(panel.findAll('[data-widget]')).toHaveLength(18)
    expect(panel.find('[data-bolge="SABIT"]').findAll('[data-widget]')).toHaveLength(2)
    expect(panel.find('[data-bolge="SOL"]').findAll('[data-widget]')).toHaveLength(10)
    expect(panel.find('[data-bolge="SAG"]').findAll('[data-widget]')).toHaveLength(6)
    expect(panel.find('[data-bolge="SOL"] .duzen-grup-baslik').text()).toBe(
      i18n.global.t('dashboard.bolgeGrafikler')
    )
    expect(panel.find('[data-bolge="SAG"] .duzen-grup-baslik').text()).toBe(
      i18n.global.t('dashboard.bolgeAksiyon')
    )
    expect(panel.find('.duzen-buton--boyut').exists()).toBe(false)
    expect(panel.text()).not.toContain('1x')
    expect(panel.text()).not.toContain('2x')
    expect(panel.find('[data-widget="kpi"] .duzen-sabit').exists()).toBe(true)
    expect(panel.find('[data-widget="bugunSerit"] .duzen-sabit').exists()).toBe(true)
    wrapper.unmount()
  })

  it('düzen modunda aç/kapa: kesikli çerçeve ve araç çubuğu', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    expect(wrapper.find('.db-sol [data-widget="nakitAkisi"]').classes()).toContain('dw--duzen')
    expect(wrapper.find('.db-sol [data-widget="nakitAkisi"] .dw-arac').exists()).toBe(true)
    expect(wrapper.find('.db-sag [data-widget="gorevler"] .dw-arac').exists()).toBe(true)
    expect(wrapper.find('[data-widget="kpi"] .dw-arac').exists()).toBe(false)
    wrapper.unmount()
  })

  it('bölge içinde sıralama ve gizleme çalışır', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    expect(solSira(wrapper)[0]).toBe('nakitAkisi')
    expect(
      wrapper.find('.db-sol [data-widget="nakitAkisi"] [aria-label*="Yukarı"]').attributes('disabled')
    ).toBeDefined()

    await wrapper.find('.db-sol [data-widget="nakitAkisi"] [aria-label*="Aşağı"]').trigger('click')
    await nextTick()
    expect(solSira(wrapper).slice(0, 2)).toEqual(['ciroHedef', 'nakitAkisi'])

    await wrapper.find('.db-sol [data-widget="kasaBanka"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.db-sol [data-widget="kasaBanka"]').exists()).toBe(false)
    expect(solSira(wrapper)).not.toContain('kasaBanka')
    wrapper.unmount()
  })

  it('bölgeler arası sürükle-bırak reddedilir, bölge içi kabul edilir', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    const solOnce = solSira(wrapper)
    const sagOnce = sagSira(wrapper)
    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }

    // SAG -> SOL: taşınmaz.
    await wrapper.find('.db-sag [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.db-sol [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()
    expect(solSira(wrapper)).toEqual(solOnce)
    expect(sagSira(wrapper)).toEqual(sagOnce)

    // SOL -> SOL: stok değeri başa taşınır.
    await wrapper.find('.db-sol [data-widget="stokDegeri"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.db-sol [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()
    expect(solSira(wrapper)[0]).toBe('stokDegeri')
    wrapper.unmount()
  })

  it('kaydet tercihi iki bölgeli, boyut alanı olmadan PUT eder', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    await wrapper.find('[data-test="duzen-paneli"] [data-widget="icgoruler"] input[type="checkbox"]').setValue(false)
    await nextTick()
    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledTimes(1)
    const gonderilen = dashboardAPI.tercihKaydet.mock.calls[0][0]
    expect(gonderilen.duzen.versiyon).toBe(1)
    expect(gonderilen.duzen.sira.find((w) => w.key === 'icgoruler').gorunur).toBe(false)
    expect(gonderilen.duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    expect(gonderilen.duzen.sira[0].key).toBe('kpi')
    expect(gonderilen.duzen.sira[1].key).toBe('bugunSerit')
    // Sabitlerden sonra önce grafik duvarı, sonra aksiyon paneli gelir.
    expect(gonderilen.duzen.sira[2].key).toBe('nakitAkisi')
    expect(gonderilen.duzen.sira.find((w) => w.key === 'gorevler').gorunur).toBe(true)
    expect(wrapper.find('[data-test="duzen-paneli"]').exists()).toBe(false)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(i18n.global.t('dashboard.duzenKaydedildi'))
    wrapper.unmount()
  })

  it('kaydetme başarısızsa düzeni localStorage\'a yazar', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountEt()
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

  it('rol varsayılanına sıfırla ve iptal taslağı geri alır', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    await wrapper.find('.db-sag [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    await nextTick()
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(true)

    await wrapper.find('.db-sag [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')
    expect(wrapper.find('.db-sag [data-widget="icgoruler"]').exists()).toBe(true)
    expect(dashboardAPI.tercihKaydet).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('backend tercihi varsa bölge içi sırayı ondan alır', async () => {
    tercihMockla({
      versiyon: 1,
      sira: [
        { key: 'kpi', gorunur: true },
        { key: 'bugunSerit', gorunur: true },
        { key: 'akis', gorunur: true },
        { key: 'gorevler', gorunur: true },
        { key: 'kasaBanka', gorunur: true }
      ]
    })
    const wrapper = mountEt()
    await flushPromises()
    expect(sagSira(wrapper).slice(0, 2)).toEqual(['akis', 'gorevler'])
    expect(solSira(wrapper)[0]).toBe('kasaBanka')
    expect(solSira(wrapper)).toHaveLength(SOL_WIDGET_ANAHTARLARI.length)
    expect(wrapper.find('[data-test="bugun-seridi"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('backend boşsa kaydedilememiş yerel düzeni kullanır', async () => {
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
    const wrapper = mountEt()
    await flushPromises()
    expect(solSira(wrapper)[0]).toBe('nakitAkisi')
    expect(sagSira(wrapper)[0]).toBe('gorevler')
    wrapper.unmount()
  })

  it('eski kayıtlardaki hedefler/vade ve boyut alanını temizler', async () => {
    tercihMockla({
      versiyon: 1,
      sira: [
        { key: 'hedefler', boyut: 2, gorunur: true },
        { key: 'kpi', boyut: 2, gorunur: true },
        { key: 'vade', boyut: 1, gorunur: true },
        { key: 'gorevler', boyut: 1, gorunur: true }
      ]
    })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-widget="hedefler"]').exists()).toBe(false)
    expect(wrapper.find('[data-widget="vade"]').exists()).toBe(false)
    expect(sagSira(wrapper)[0]).toBe('gorevler')
    expect(sagSira(wrapper)).toHaveLength(SAG_WIDGET_ANAHTARLARI.length)

    await duzenModuAc(wrapper)
    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()
    const gonderilen = dashboardAPI.tercihKaydet.mock.calls[0][0]
    expect(gonderilen.duzen.sira.map((w) => w.key)).not.toContain('hedefler')
    expect(gonderilen.duzen.sira.map((w) => w.key)).not.toContain('vade')
    expect(gonderilen.duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    wrapper.unmount()
  })
})
