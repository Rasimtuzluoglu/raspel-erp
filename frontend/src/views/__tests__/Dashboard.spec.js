import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import Dashboard from '../Dashboard.vue'
import { dashboardAPI, excelAPI } from '../../api/index.js'
import { __sonOlay, __bagli } from '../../composables/useDashboardCanli.js'

const { routerPush } = vi.hoisted(() => ({ routerPush: vi.fn() }))

vi.mock('../../api/index.js', () => ({
  dashboardAPI: {
    kokpit: vi.fn(),
    tercihGetir: vi.fn(),
    tercihKaydet: vi.fn()
  },
  excelAPI: {
    dashboard: vi.fn()
  },
  dovizAPI: {
    getKurlar: vi.fn().mockResolvedValue({ data: [] })
  },
  hareketAPI: {
    create: vi.fn().mockResolvedValue({ data: {} })
  },
  onaylarAPI: {
    bekleyen: vi.fn().mockResolvedValue({ data: { content: [] } }),
    karar: vi.fn().mockResolvedValue({ data: {} })
  },
  cariHesapAPI: {
    filtreli: vi.fn().mockResolvedValue({ data: [] }),
    getById: vi.fn().mockResolvedValue({ data: null })
  }
}))

vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => ({
    sirketId: 1,
    isAdmin: true,
    kullanici: { id: 1, displayName: 'Test Kullanıcı', role: 'ADMIN' }
  })
}))

vi.mock('../../composables/useDashboardCanli.js', async () => {
  const { ref } = await import('vue')
  const sonOlay = ref(null)
  const bagli = ref(true)
  return {
    CANLI_YENILEME_GECIKME_MS: 2000,
    __sonOlay: sonOlay,
    __bagli: bagli,
    useDashboardCanli: () => ({
      aktif: ref(true),
      bagli,
      sonOlay,
      yenidenBaglandi: ref(0),
      ayarla: vi.fn()
    })
  }
})

vi.mock('../../composables/useToastBildirim.js', () => ({
  useToastBildirim: () => ({
    basarili: vi.fn(),
    hata: vi.fn(),
    uyari: vi.fn(),
    bilgi: vi.fn()
  })
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: routerPush }),
  useRoute: () => ({ path: '/' })
}))

const RouterLinkStub = {
  props: ['to'],
  template: '<a :data-to="JSON.stringify(to)"><slot /></a>'
}

const kpiListesi = [
  ['netNakit', 'Net Nakit', 120000],
  ['alacak', 'Alacak', 90000],
  ['verecek', 'Verecek', 40000],
  ['netCalismaSermayesi', 'Net Çalışma Sermayesi', 170000],
  ['ciro', 'Dönem Cirosu', 300000],
  ['brutKar', 'Dönem Kârı', 80000],
  ['marj', 'Kâr Marjı', 26.6],
  ['vadesiGecen', 'Vadesi Geçen', 15000]
].map(([anahtar, etiket, deger]) => ({
  anahtar,
  etiket,
  deger,
  sparkline: [],
  kritik: anahtar === 'vadesiGecen',
  hedef: { route: '/kasa', query: {} }
}))

const kokpitVerisi = () => ({
  donem: { kod: 'BU_AY', etiket: 'Bu Ay' },
  ozet: kpiListesi,
  kritikDurum: [
    { anahtar: 'vadesiGecen', etiket: 'Vadesi Geçen', seviye: 'KRITIK', tutar: 15000, adet: 3, hedef: { route: '/tahsilat', query: {} } }
  ],
  nakit: {
    kasa: 20000,
    banka: 100000,
    toplam: 120000,
    trendYuzde: 4.2,
    sparkline: [1, 2, 3],
    gunlukAkis: [{ net: 100 }],
    projeksiyon: { tahminiBitisBakiyesi: 140000, toplamBeklenenGiris: 5000, toplamBeklenenCikis: 3000, gunSayisi: 30, gunluk: [{ tarih: '2026-11-06', giris: 100, cikis: 50, net: 50, kumulatif: 120000 }, { tarih: '2026-11-07', giris: 0, cikis: 0, net: 0, kumulatif: 120000 }] },
    hedef: { route: '/kasa', query: {} }
  },
  alacakVerecek: {
    alacakToplam: 60000,
    verecekToplam: 20000,
    net: 40000,
    tahsilatOrani: 70,
    yaslanma: [],
    borclular: [],
    tedarikciler: [],
    hedef: { route: '/cari-hesaplar', query: {} }
  },
  takvim: {
    toplamGiris: 100,
    toplamCikis: 50,
    net: 50,
    gunler: [
      { tarih: '2026-11-06', giris: 100, cikis: 50, net: 50, kumulatif: 50, girisAdet: 1, cikisAdet: 1 },
      { tarih: '2026-11-07', giris: 0, cikis: 0, net: 0, kumulatif: 50, girisAdet: 0, cikisAdet: 0 }
    ]
  },
  karMarj: {
    ciro: 100000,
    maliyet: 70000,
    kar: 30000,
    marj: 30,
    trend: [],
    urunler: [],
    cariler: [],
    maliyetsizKalem: 0,
    hedef: { route: '/raporlar', query: {} }
  },
  faturaSiparis: {
    durumlar: [],
    vadesiGecenTutar: 15000,
    vadesiGecenAdet: 3,
    vadesiGecenFaturalar: [],
    tahsilatOrani: 70,
    siparisDurumlari: [],
    hedef: { route: '/tahsilat', query: {} }
  },
  icgoruler: [
    {
      seviye: 'WARN',
      baslik: 'Vadesi geçen faturalar var',
      mesaj: '3 faturanın vadesi geçmiş; tahsilat önerilir.',
      route: '/tahsilat',
      ikon: 'pi pi-exclamation-triangle'
    }
  ]
})

const mountEdilenler = []

const mountDashboard = () => {
  const wrapper = mount(Dashboard, {
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })
  mountEdilenler.push(wrapper)
  return wrapper
}

describe('Dashboard kokpiti', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    __sonOlay.value = null
    __bagli.value = true
    dashboardAPI.kokpit.mockResolvedValue({ data: kokpitVerisi() })
    dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
    dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
    excelAPI.dashboard.mockResolvedValue({ data: new Blob(['x']) })
  })

  afterEach(() => {
    mountEdilenler.splice(0).forEach((wrapper) => wrapper.unmount())
    vi.useRealTimers()
    vi.restoreAllMocks()
  })

  it('dashboardKokpitStoreIleYuklenir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    expect(dashboardAPI.kokpit).toHaveBeenCalledWith({ donem: 'BU_AY' })
    expect(wrapper.find('[data-test="finans-ozet-bandi"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kritik-durum-seridi"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-bolum-nakit"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-bolum-alacakVerecek"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-bolum-karMarj"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-bolum-faturaSiparis"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-bolumu"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-takvimi"]').exists()).toBe(true)
  })

  it('donemDegisinceYeniIstekGider', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    dashboardAPI.kokpit.mockClear()

    await wrapper.find('[data-test="donem-GECEN_AY"]').trigger('click')
    await flushPromises()

    expect(dashboardAPI.kokpit).toHaveBeenCalledWith({ donem: 'GECEN_AY' })
    expect(localStorage.getItem('raspel_dashboard_donem')).toBe('GECEN_AY')
  })

  it('bolumDetayRouterPushCagirir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="nakit-detay"]').trigger('click')

    expect(routerPush).toHaveBeenCalledWith({ path: '/kasa', query: {} })
  })

  it('bolumHataTekBolumuBozmaz', async () => {
    const veri = kokpitVerisi()
    veri.nakit = null
    dashboardAPI.kokpit.mockResolvedValue({ data: veri })

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-test="kokpit-bolum-nakit"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-bolum-karMarj"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-bolumu"]').text()).toContain('0,00')
  })

  it('yetkisizKarBolumuGizlenir', async () => {
    const veri = kokpitVerisi()
    veri.karMarj = null
    dashboardAPI.kokpit.mockResolvedValue({ data: veri })

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-test="kokpit-bolum-karMarj"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="kokpit-bolum-nakit"]').exists()).toBe(true)
  })

  it('veriHatasiKokpitHatasiniGosterir', async () => {
    dashboardAPI.kokpit.mockRejectedValueOnce(new Error('API down'))

    const wrapper = mountDashboard()
    await flushPromises()

    expect(wrapper.find('[data-test="kokpit-hata"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('API down')
  })

  it('excelIndirApiyiCagirir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="kokpit-excel"]').trigger('click')
    await flushPromises()

    expect(excelAPI.dashboard).toHaveBeenCalledWith({ donem: 'BU_AY' })
  })

  it('ozellestirmePaneliBolumGizlerVeKaydeder', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="kokpit-ozellestir"]').trigger('click')
    expect(wrapper.find('[data-test="kokpit-ozellestirme"]').exists()).toBe(true)

    await wrapper.find('[data-test="ozellestirme-gorunurluk-karMarj"]').setValue(false)
    expect(wrapper.find('[data-test="kokpit-bolum-karMarj"]').exists()).toBe(false)

    await wrapper.find('[data-test="ozellestirme-kaydet"]').trigger('click')
    await flushPromises()

    expect(dashboardAPI.tercihKaydet).toHaveBeenCalled()
    expect(wrapper.find('[data-test="kokpit-ozellestirme"]').exists()).toBe(false)
  })

  it('stompOlayiKokpitiYeniler', async () => {
    vi.useFakeTimers()
    mountDashboard()
    await flushPromises()
    dashboardAPI.kokpit.mockClear()

    __sonOlay.value = { tur: 'FATURA', mesaj: 'Yeni fatura', zaman: new Date().toISOString() }
    await nextTick()
    await vi.advanceTimersByTimeAsync(2000)

    expect(dashboardAPI.kokpit).toHaveBeenCalledTimes(1)
  })

  it('kisaArdisikOlaylarTekIstekAtar', async () => {
    vi.useFakeTimers()
    mountDashboard()
    await flushPromises()
    dashboardAPI.kokpit.mockClear()

    __sonOlay.value = { tur: 'FATURA' }
    await nextTick()
    __sonOlay.value = { tur: 'SIPARIS' }
    await nextTick()
    __sonOlay.value = { tur: 'TAHSILAT' }
    await nextTick()
    await vi.advanceTimersByTimeAsync(2000)

    expect(dashboardAPI.kokpit).toHaveBeenCalledTimes(1)
  })

  it('baglantiKopukkenKokpitHataVermez', async () => {
    const wrapper = mountDashboard()
    await flushPromises()
    __bagli.value = false
    await nextTick()

    expect(wrapper.find('[data-test="canli-durum"]').text()).toContain('Bağlantı yok')
    expect(wrapper.find('[data-test="nakit-bolumu"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-hata"]').exists()).toBe(false)
  })

  it('paraBirimiDegisinceTutarlarDonusturulur', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="para-birimi"]').setValue('USD')
    await nextTick()

    expect(localStorage.getItem('raspel_dashboard_para_birimi')).toBe('USD')
    expect(wrapper.find('[data-test="finans-ozet-bandi"]').text()).toContain('$')
  })

  it('hizliIslemBarindanTahsilatDialogAcar', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="hizli-tahsilat"]').trigger('click')
    expect(wrapper.find('[data-test="hizli-tahsilat-katman"]').exists()).toBe(true)
  })

  it('kritikOgedenTahsilatDialogAcar', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="kritik-oge-vadesiGecen"]').trigger('click')
    expect(wrapper.find('[data-test="hizli-tahsilat-katman"]').exists()).toBe(true)
  })

  it('hizliOnayDialogAcar', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    await wrapper.find('[data-test="hizli-onaylar"]').trigger('click')
    expect(wrapper.find('[data-test="kokpit-onay-katman"]').exists()).toBe(true)
  })

  it('yoneticiIcgoruSeridiniGosterir', async () => {
    const wrapper = mountDashboard()
    await flushPromises()

    const serit = wrapper.find('[data-test="kokpit-icgoruler"]')
    expect(serit.exists()).toBe(true)
    expect(serit.text()).toContain('Vadesi geçen faturalar var')
    expect(wrapper.find('[data-test="kokpit-icgoru-0"]').exists()).toBe(true)
  })
})
