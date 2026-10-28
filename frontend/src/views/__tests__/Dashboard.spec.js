import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import { dashboardAPI, stokAPI } from '../../api/index.js'
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
  },
  stokAPI: { kritik: vi.fn() }
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
  kasaBanka: { kasa: 1000, banka: 4000 }
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

const anaSira = (wrapper) =>
  wrapper.findAll('.pano-ana [data-widget]').map((w) => w.attributes('data-widget'))

const raySira = (wrapper) =>
  wrapper.findAll('.pano-ray [data-widget]').map((w) => w.attributes('data-widget'))

const duzenModuAc = async (wrapper) => {
  const duzenle = wrapper.find('.hero-buton[aria-pressed="false"]')
  await duzenle.trigger('click')
  await nextTick()
}

const tercihMockla = (duzen) => {
  dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen } })
}

describe('Dashboard.vue — tek pano yüzeyi', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
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
    stokAPI.kritik.mockResolvedValue({ data: kritikStokVerisi })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('tek bir .pano yüzeyi ve üst band olarak hero render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const panolar = wrapper.findAll('.pano')
    expect(panolar).toHaveLength(1)

    const hero = wrapper.find('.pano > .dashboard-hero')
    expect(hero.exists()).toBe(true)
    expect(hero.text()).toContain('Ayşe Yılmaz')
    expect(hero.text()).toContain('RasPel A.Ş.')
    expect(hero.text()).toContain(i18n.global.t('dashboard.canli'))
    expect(wrapper.findAll('[data-test="hero-bugun"]')).toHaveLength(4)
    expect(hero.text()).toContain('15.000,00')
    // Hero kendi çerçevesini/gölgesini çizmez; pano bandıdır.
    expect(hero.classes()).not.toContain('pano')
    wrapper.unmount()
  })

  it('KPI bandını 4 hücre olarak, ayrı kart çerçevesi olmadan render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const kpi = wrapper.find('[data-test="kpi-grid"]')
    expect(kpi.classes()).toContain('pano-kpi')
    expect(kpi.element.children).toHaveLength(4)
    expect(kpi.find('.bento').exists()).toBe(false)

    expect(wrapper.find('[data-test="kpi-likidite"]').text()).toContain('120.000,00')
    expect(wrapper.find('[data-test="kpi-likidite"]').text()).toContain('4.2%')
    expect(wrapper.find('[data-test="kpi-net-cari"]').text()).toContain('90.000,00')
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('60')
    // Ciro hedefi ve kâr hedefi KPI hücrelerinde kalır.
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('500.000,00')
    expect(wrapper.find('[data-test="kpi-kar"]').text()).toContain('100.000,00')
    expect(wrapper.find('[data-test="kpi-kar"]').text()).toContain('26.6')
    wrapper.unmount()
  })

  it('gövdeyi ana içerik + sağ ray olarak kurar', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('.pano-govde').exists()).toBe(true)
    expect(anaSira(wrapper)).toEqual([
      'gorevler',
      'nakitAkisi',
      'aylikGelirGider',
      'kategoriSatislari',
      'enCokSatanlar',
      'akis'
    ])
    expect(raySira(wrapper)).toEqual(['icgoruler', 'hizliIslemler', 'kritikStok'])
    expect(wrapper.find('.pano-ray').text()).toContain(i18n.global.t('dashboard.icgoruler'))
    wrapper.unmount()
  })

  it('görev merkezi ana içeriğin ilk bölümüdür ve vadeler görev satırıdır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const gorev = wrapper.find('[data-test="gorev-merkezi"]')
    expect(gorev.exists()).toBe(true)
    expect(gorev.findAll('.gorev-kart')).toHaveLength(3)
    expect(gorev.find('.gorev-kart--kritik').text()).toContain('Vadesi geçen alacak')

    // Vadeler ayrı liste/kart değil; görev satırı olarak /tahsilat'a gider.
    expect(wrapper.find('[data-test="vade-takibi"]').exists()).toBe(false)
    const gecen = wrapper.find('[data-test="gorev-VADESI_GECEN"]')
    expect(gecen.attributes('href')).toBe('/tahsilat')
    expect(gecen.text()).toContain('12.500,00')
    expect(wrapper.find('[data-test="gorev-VADESI_YAKLASAN"]').attributes('href')).toBe('/tahsilat')

    // Kaldırılan widget'lar hiçbir yerde çizilmez.
    expect(wrapper.find('[data-widget="hedefler"]').exists()).toBe(false)
    expect(wrapper.find('[data-widget="vade"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="hedefler"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('nakit akışı bölümünün altında 3 kompakt projeksiyon çipi gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const projeksiyon = wrapper.find('[data-test="nakit-projeksiyon"]')
    expect(projeksiyon.exists()).toBe(true)
    const cipler = projeksiyon.findAll('[data-test="nakit-projeksiyon-cip"]')
    expect(cipler).toHaveLength(3)
    expect(cipler[0].text()).toContain('30 gün')
    expect(cipler[2].text()).toContain('90 gün')
    // Çipler nakit akışı bölümünün içindedir (ayrı panel yok).
    expect(wrapper.find('[data-widget="nakitAkisi"]').text()).toContain(
      i18n.global.t('dashboard.hedefNakitProjeksiyon')
    )
    wrapper.unmount()
  })

  it('sağ ray bölümlerini kompakt biçimde render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const icgoru = wrapper.find('[data-test="icgoru-karti"]')
    expect(icgoru.findAll('.icgoru-satir')).toHaveLength(2)
    expect(icgoru.find('.icgoru-satir--kritik').exists()).toBe(true)
    // Kendi kart çerçevesi yok.
    expect(icgoru.find('.icgoru-satir').classes()).not.toContain('icgoru-kart')

    const hizli = wrapper.find('[data-test="hizli-islemler"]')
    expect(hizli.exists()).toBe(true)

    const kritik = wrapper.find('[data-test="kritik-stok"]')
    expect(kritik.findAll('.kritik-satir')).toHaveLength(4)
    expect(kritik.text()).toContain('Kalem A')
    expect(kritik.text()).toContain(i18n.global.t('dashboard.tumunuGor'))
    wrapper.unmount()
  })

  it('ana grafik kartlarını ve drill-down bağlantılarını render eder', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const kartlar = wrapper.findAll('[data-test="grafik-karti"]')
    expect(kartlar).toHaveLength(4)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.grafikNakitAkisi'))
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.grafikAylikGelirGider'))

    expect(kartlar[0].find('a').attributes('href')).toBe('/kasa')
    expect(kartlar[1].find('a').attributes('href')).toBe('/raporlar')
    expect(kartlar[2].find('a').attributes('href')).toBe('/raporlar')
    expect(kartlar[3].find('a').attributes('href')).toBe('/stoklar')
    wrapper.unmount()
  })

  it('canlı akışı ve hızlı işlemleri render eder', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('raspel:hizli-ara', dinleyici)
    const wrapper = mountEt()
    await flushPromises()

    const akis = wrapper.find('[data-test="canli-akis"]')
    expect(akis.exists()).toBe(true)
    expect(akis.findAll('.akis-satir')).toHaveLength(1)
    expect(akis.text()).toContain('Tahsilat alındı')

    const hizli = wrapper.find('[data-test="hizli-islemler"]')
    const kisayollar = hizli.findAll('.islem-karti').filter((k) => !k.classes('islem-karti--ara'))
    expect(kisayollar).toHaveLength(9)
    expect(hizli.find('.islem-karti--ara').exists()).toBe(true)
    await hizli.find('.islem-karti--ara').trigger('click')
    expect(dinleyici).toHaveBeenCalledTimes(1)
    window.removeEventListener('raspel:hizli-ara', dinleyici)
    wrapper.unmount()
  })

  it('progresif yükleme: önce KPI iskeleti, sonra veri gelir', async () => {
    let coz
    dashboardAPI.ozet.mockImplementation(
      () =>
        new Promise((resolve) => {
          coz = resolve
        })
    )
    const wrapper = mountEt()
    await wrapper.vm.$nextTick()

    expect(wrapper.findAll('.kpi-hucre--iskelet')).toHaveLength(4)
    expect(wrapper.find('.dashboard-hero .bugun-skeleton').exists()).toBe(true)

    coz({ data: ozetVerisi })
    await flushPromises()

    expect(wrapper.findAll('.kpi-hucre--iskelet')).toHaveLength(0)
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

  it('a11y: canlı duyuru alanı ve etiketli butonlar korunur', async () => {
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

  it('"Tüm analizler" panonun alt bandında açılır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const analiz = wrapper.find('.pano > .pano-analizler')
    expect(analiz.exists()).toBe(true)
    expect(wrapper.findAll('[data-test="grafik-karti"]')).toHaveLength(4)

    const buton = wrapper.find('.analiz-baslik')
    expect(buton.attributes('aria-expanded')).toBe('false')
    await buton.trigger('click')
    expect(buton.attributes('aria-expanded')).toBe('true')
    expect(wrapper.findAll('[data-test="grafik-karti"]')).toHaveLength(10)
    expect(wrapper.find('a[href="/tahsilat"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('USER rolünde analizler kapalı, ray ve KPI görünür', async () => {
    authMock.kullanici.role = 'USER'
    authMock.isAdmin = false
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-widget="kpi"]').exists()).toBe(true)
    expect(wrapper.find('.analiz-baslik').exists()).toBe(false)
    expect(raySira(wrapper)).toEqual(['hizliIslemler', 'kritikStok', 'icgoruler'])
    expect(anaSira(wrapper)).toEqual(['gorevler', 'akis', 'enCokSatanlar', 'nakitAkisi'])
    wrapper.unmount()
  })

  it('tercih okuma isteğini bir kez yapar', async () => {
    const wrapper = mountEt()
    await flushPromises()
    expect(dashboardAPI.tercihGetir).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})

describe('Dashboard.vue — kişiselleştirme', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    authMock.kullanici.role = 'ADMIN'
    authMock.isAdmin = true
    dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi })
    dashboardAPI.gorevler.mockResolvedValue({ data: gorevVerisi })
    dashboardAPI.grafikler.mockResolvedValue({ data: grafikVerisi })
    dashboardAPI.akis.mockResolvedValue({ data: akisVerisi })
    dashboardAPI.icgoruler.mockResolvedValue({ data: icgoruVerisi })
    dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
    stokAPI.kritik.mockResolvedValue({ data: kritikStokVerisi })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('düzen paneli iki grup gösterir ve boyut kontrolü yoktur', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    const panel = wrapper.find('[data-test="duzen-paneli"]')
    expect(panel.exists()).toBe(true)
    expect(panel.findAll('[data-bolge]')).toHaveLength(2)
    expect(panel.find('[data-bolge="ana"]').text()).toContain(i18n.global.t('dashboard.anaIcerik'))
    expect(panel.find('[data-bolge="ray"]').text()).toContain(i18n.global.t('dashboard.sagRay'))
    expect(panel.find('.duzen-buton--boyut').exists()).toBe(false)
    expect(wrapper.find('.dw-buton--boyut').exists()).toBe(false)
    // kpi sabittir: sıra oku yok.
    expect(panel.find('[data-widget="kpi"] .duzen-sabit').exists()).toBe(true)
    wrapper.unmount()
  })

  it('düzen modunda bölge içi sıralama ve gizleme çalışır', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    expect(wrapper.find('.dw--duzen').exists()).toBe(true)

    // ana içerikte ilk bölüm görevler: yukarı devre dışı.
    const gorevler = wrapper.find('.pano-ana [data-widget="gorevler"]')
    expect(gorevler.find('[aria-label*="Yukarı"]').attributes('disabled')).toBeDefined()

    // aynı bölge içinde klavye sıralaması: akis bir üst sıraya geçer.
    const onceki = anaSira(wrapper)
    await wrapper.find('.pano-ana [data-widget="akis"] [aria-label*="Yukarı"]').trigger('click')
    await nextTick()
    expect(anaSira(wrapper).indexOf('akis')).toBe(onceki.indexOf('akis') - 1)

    // sağ rayda gizleme rayı bozmaz.
    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(false)
    expect(raySira(wrapper)).toEqual(['hizliIslemler', 'kritikStok'])
    wrapper.unmount()
  })

  it('sürükle-bırak yalnızca aynı bölge içinde çalışır', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }
    await wrapper.find('.pano-ana [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.pano-ana [data-widget="nakitAkisi"]').trigger('drop', { dataTransfer })
    await nextTick()
    expect(anaSira(wrapper)[1]).toBe('akis')

    // Bölgeler arası taşıma reddedilir.
    const ray = raySira(wrapper)
    await wrapper.find('.pano-ana [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await wrapper.find('.pano-ray [data-widget="kritikStok"]').trigger('drop', { dataTransfer })
    await nextTick()
    expect(raySira(wrapper)).toEqual(ray)
    wrapper.unmount()
  })

  it('kpi taşınamaz (panel oku yok, sürükleme yok sayılır)', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    const kpi = wrapper.find('[data-widget="kpi"]')
    expect(kpi.find('.dw-arac').exists()).toBe(false)

    const dataTransfer = { setData: vi.fn(), effectAllowed: '', dropEffect: '' }
    await wrapper.find('.pano-ana [data-widget="akis"]').trigger('dragstart', { dataTransfer })
    await kpi.trigger('drop', { dataTransfer })
    await nextTick()
    expect(anaSira(wrapper)).toContain('akis')
    expect(wrapper.findAll('.pano > [data-widget]')[0].attributes('data-widget')).toBe('kpi')
    wrapper.unmount()
  })

  it('gizle/göster ve kaydet tercihi boyut alanı olmadan PUT eder', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    await wrapper.find('[data-test="duzen-paneli"] [data-widget="icgoruler"] input[type="checkbox"]').setValue(false)
    await nextTick()
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await flushPromises()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalledTimes(1)
    const gonderilen = dashboardAPI.tercihKaydet.mock.calls[0][0]
    expect(gonderilen.duzen.versiyon).toBe(1)
    expect(gonderilen.duzen.sira.find((w) => w.key === 'icgoruler').gorunur).toBe(false)
    expect(gonderilen.duzen.sira.every((w) => !('boyut' in w))).toBe(true)
    expect(gonderilen.duzen.sira[0].key).toBe('kpi')
    expect(wrapper.find('[data-test="duzen-paneli"]').exists()).toBe(false)
    expect(wrapper.find('[aria-live="polite"]').text()).toBe(i18n.global.t('dashboard.duzenKaydedildi'))
    wrapper.unmount()
  })

  it('kaydetme başarısızsa düzeni localStorage\'a yazar', async () => {
    dashboardAPI.tercihKaydet.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountEt()
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

  it('rol varsayılanına sıfırla ve iptal taslağı geri alır', async () => {
    const wrapper = mountEt()
    await flushPromises()
    await duzenModuAc(wrapper)

    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(false)

    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    await nextTick()
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(true)

    await wrapper.find('.pano-ray [data-widget="icgoruler"] .dw-buton--gizle').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')
    expect(wrapper.find('.pano-ray [data-widget="icgoruler"]').exists()).toBe(true)
    expect(dashboardAPI.tercihKaydet).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('backend tercihi varsa onu uygular', async () => {
    tercihMockla({
      versiyon: 1,
      sira: [
        { key: 'kpi', gorunur: true },
        { key: 'akis', gorunur: true },
        { key: 'gorevler', gorunur: true }
      ]
    })
    const wrapper = mountEt()
    await flushPromises()
    expect(anaSira(wrapper)[0]).toBe('akis')
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
            { key: 'gorevler', gorunur: true }
          ]
        },
        kaydedilemedi: true
      })
    )
    const wrapper = mountEt()
    await flushPromises()
    expect(anaSira(wrapper)[0]).toBe('gorevler')
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
    expect(anaSira(wrapper)[0]).toBe('gorevler')

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
