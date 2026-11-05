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
  onaylarAPI: {
    bekleyen: vi.fn(),
    karar: vi.fn()
  },
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

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const iki = (n) => String(n).padStart(2, '0')
const ayKodu = (tarih) =>
  `${tarih.getFullYear()}-${iki(tarih.getMonth() + 1)}-${iki(tarih.getDate())}`

const bugun = new Date()
const buAyIlk = ayKodu(new Date(bugun.getFullYear(), bugun.getMonth(), 1))
const buAySon = ayKodu(new Date(bugun.getFullYear(), bugun.getMonth() + 1, 0))

const ozetVerisi = (degisiklikler = {}) => ({
  donem: { kod: 'BU_AY', baslangic: buAyIlk, bitis: buAySon, etiket: 'Bu Ay' },
  hero: { kullaniciAd: 'Ayşe Yılmaz', sirketAd: 'RasPel A.Ş.', canli: true },
  bugun: { tahsilat: 1000, odeme: 500, siparis: 1, teslimat: 1 },
  kpi: {
    likidite: { deger: 120000, kasa: 20000, banka: 100000, sparkline: [1, 2] },
    netCari: { deger: 50000, alacak: 90000, borc: 40000 },
    ciro: { deger: 300000, hedef: 500000, yuzde: 60, onceki: 260000, degisimYuzde: 15.4 },
    kar: { deger: 80000, hedef: 100000, marj: 26.6, onceki: 90000, degisimYuzde: -11.1 }
  },
  stok: { cesit: 42, deger: 250000, kritikAdet: 3 },
  gorevSayilari: {
    onayBekleyen: 2,
    vadesiGecen: 1,
    kritikStok: 3,
    bekleyenTeslimat: 0,
    vadesiYaklasan: 0
  },
  ...degisiklikler
})

const gorevVerisi = [
  {
    tur: 'ONAY_IZIN',
    baslik: 'İzin onayı bekliyor',
    aciklama: '2 personel',
    adet: 2,
    tutar: 0,
    oncelik: 45,
    route: '/onaylar?tur=IZIN',
    ikon: 'pi pi-calendar',
    renk: '#3b82f6'
  }
]

const mountEt = () =>
  mount(Dashboard, {
    global: {
      plugins: [createPinia(), i18n],
      // Canli akis kendi 60 sn zamanlayicisini kurar; otomatik yenileme testi
      // yalnizca anasayfanin anahtarini olcsun diye sabitlenir.
      stubs: { 'router-link': RouterLinkStub, DashboardAkis: true }
    }
  })

const varsayilanMocklar = () => {
  dashboardAPI.getData.mockResolvedValue({ data: {} })
  dashboardAPI.ozet.mockResolvedValue({ data: ozetVerisi() })
  dashboardAPI.gorevler.mockResolvedValue({ data: gorevVerisi })
  dashboardAPI.grafikler.mockResolvedValue({ data: { gunlukNakitAkisi: [] } })
  dashboardAPI.akis.mockResolvedValue({ data: [] })
  dashboardAPI.icgoruler.mockResolvedValue({ data: [] })
  dashboardAPI.vadeler.mockResolvedValue({ data: [] })
  dashboardAPI.karAnalizi.mockResolvedValue({
    data: {
      donem: { kod: 'BU_AY', baslangic: buAyIlk, bitis: buAySon, etiket: 'Bu Ay' },
      ozet: { ciro: 300000, maliyet: 210000, kar: 90000, marj: 30, maliyetsizKalem: 0 },
      urunler: [
        { stokId: 1, ad: 'Ürün A', ciro: 100000, maliyet: 70000, kar: 30000, marj: 30 }
      ],
      cariler: [
        { cariId: 1, ad: 'ABC Ticaret Ltd.', ciro: 250000, maliyet: 170000, kar: 80000, marj: 32 }
      ]
    }
  })
  dashboardAPI.tercihGetir.mockResolvedValue({ data: { duzen: null } })
  dashboardAPI.tercihKaydet.mockResolvedValue({ data: { duzen: null } })
  onaylarAPI.bekleyen.mockResolvedValue({ data: [] })
  onaylarAPI.karar.mockResolvedValue({ data: { basarili: 1 } })
}

describe('Dashboard — dönem seçici', () => {
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

  it('cihazda saklanan dönemi ilk yüklemede kullanır', async () => {
    localStorage.setItem('raspel_dashboard_donem', 'GECEN_AY')
    const wrapper = mountEt()
    await flushPromises()

    expect(dashboardAPI.ozet).toHaveBeenCalledWith({ donem: 'GECEN_AY' })
    expect(wrapper.find('[data-test="donem-secici"]').element.value).toBe('GECEN_AY')
    wrapper.unmount()
  })

  it('dönem değişince store yeniden çağrılır ve tercih saklanır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(dashboardAPI.ozet).toHaveBeenCalledWith({ donem: 'BU_AY' })
    dashboardAPI.ozet.mockClear()

    await wrapper.find('[data-test="donem-secici"]').setValue('BU_YIL')
    await flushPromises()

    expect(dashboardAPI.ozet).toHaveBeenCalledTimes(1)
    expect(dashboardAPI.ozet).toHaveBeenCalledWith({ donem: 'BU_YIL' })
    expect(localStorage.getItem('raspel_dashboard_donem')).toBe('BU_YIL')
    wrapper.unmount()
  })

  it('geçersiz kayıtlı değer BU_AY varsayılanına düşer', async () => {
    localStorage.setItem('raspel_dashboard_donem', 'GECERSIZ')
    const wrapper = mountEt()
    await flushPromises()
    expect(dashboardAPI.ozet).toHaveBeenCalledWith({ donem: 'BU_AY' })
    wrapper.unmount()
  })

  it('KPI trend rozetlerini ve hedef bağlantısını özetten besler', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="kpi-trend-ciro"]').text()).toContain('▲')
    expect(wrapper.find('[data-test="kpi-trend-kar"]').text()).toContain('▼')
    // Hedef dolu oldugunda "Hedef belirle" baglantisi gorunmez.
    expect(wrapper.find('[data-test="kpi-hedef-baglanti"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('hedef yoksa KPI kartı yönetici kokpitine bağlanır', async () => {
    dashboardAPI.ozet.mockResolvedValue({
      data: ozetVerisi({
        kpi: {
          ...ozetVerisi().kpi,
          ciro: { deger: 300000, hedef: null, yuzde: null, degisimYuzde: null },
          kar: { deger: 80000, hedef: null, marj: 26.6, degisimYuzde: null }
        }
      })
    })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="kpi-ciro"]').attributes('href')).toBe('/yonetici-kokpiti')
    expect(wrapper.findAll('[data-test="kpi-hedef-baglanti"]')).toHaveLength(2)
    wrapper.unmount()
  })
})

describe('Dashboard — bildirim zili ve yardımcı paneller', () => {
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

  it('zil rozetini özet görev sayılarından hesaplar', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="bildirim-rozet"]').text()).toBe('6')
    await wrapper.find('[data-test="bildirim-zili"]').trigger('click')
    expect(wrapper.find('[data-test="bildirim-onayBekleyen"]').attributes('href')).toBe('/onaylar')
    expect(wrapper.find('[data-test="bildirim-vadesiGecen"]').attributes('href')).toBe('/tahsilat')
    expect(wrapper.find('[data-test="bildirim-kritikStok"]').attributes('href')).toBe('/stoklar?kritik=1')
    wrapper.unmount()
  })

  it('canli mod anahtari tercihi cihaza yazar ve 60 sn yedek zamanlayici kurar', async () => {
    const sayac = vi.spyOn(globalThis, 'setInterval')
    const temizle = vi.spyOn(globalThis, 'clearInterval')
    const wrapper = mountEt()
    await flushPromises()

    const altmisSn = () => sayac.mock.calls.filter(([, ms]) => ms === 60000).length
    const taban = altmisSn()

    const anahtar = wrapper.find('[data-test="oto-yenileme"]')
    // Canli mod varsayilan olarak aciktir.
    expect(anahtar.attributes('aria-checked')).toBe('true')

    await anahtar.trigger('click')
    await nextTick()
    expect(localStorage.getItem('raspel_dashboard_canli')).toBe('0')
    expect(anahtar.attributes('aria-checked')).toBe('false')
    expect(temizle).toHaveBeenCalled()

    await anahtar.trigger('click')
    await nextTick()
    expect(localStorage.getItem('raspel_dashboard_canli')).toBe('1')
    expect(anahtar.attributes('aria-checked')).toBe('true')
    expect(altmisSn()).toBe(taban + 1)
    wrapper.unmount()
    sayac.mockRestore()
    temizle.mockRestore()
  })

  it('cihazda acik canli tercih varsa yuklemede zamanlayici kurulur', async () => {
    const sayac = vi.spyOn(globalThis, 'setInterval')
    const altmisSn = () => sayac.mock.calls.filter(([, ms]) => ms === 60000).length

    localStorage.setItem('raspel_dashboard_canli', '0')
    const temel = mountEt()
    await flushPromises()
    const taban = altmisSn()
    temel.unmount()

    localStorage.setItem('raspel_dashboard_canli', '1')
    const wrapper = mountEt()
    await flushPromises()

    expect(altmisSn()).toBe(taban + 1)
    expect(wrapper.find('[data-test="oto-yenileme"]').attributes('aria-checked')).toBe('true')
    wrapper.unmount()
    sayac.mockRestore()
  })

  it('görev satırındaki hızlı onay drawer\'ı açar ve türü eşler', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const buton = wrapper.find('[data-test="gorev-hizli-onay-ONAY_IZIN"]')
    expect(buton.exists()).toBe(true)
    expect(buton.text()).toContain(i18n.global.t('dashboard.hizliOnay'))

    await buton.trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="onay-drawer"]').exists()).toBe(true)
    expect(onaylarAPI.bekleyen).toHaveBeenCalledWith({ tur: 'IZIN', page: 0, size: 50 })
    wrapper.unmount()
  })

  it('yetkisiz kullanıcıda hızlı onay butonu görünmez', async () => {
    authMock.isAdmin = false
    authMock.kullanici.role = 'USER'
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="gorev-hizli-onay-ONAY_IZIN"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('muhasebe rolü hızlı onay butonunu görür', async () => {
    authMock.isAdmin = false
    authMock.kullanici.role = 'MUHASEBE'
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="gorev-hizli-onay-ONAY_IZIN"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('takvim ikonu vade takvimini açar ve ay aralığını yükler', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="vade-takvimi-buton"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="vade-takvimi"]').exists()).toBe(true)
    expect(dashboardAPI.vadeler).toHaveBeenCalledWith({ baslangic: buAyIlk, bitis: buAySon })
    wrapper.unmount()
  })

  it('kâr analizi ikonu drawer\'ı açar ve seçili dönemi aktarır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="kar-analizi-buton"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="kar-analizi-drawer"]').exists()).toBe(true)
    expect(dashboardAPI.karAnalizi).toHaveBeenCalledWith({ donem: 'BU_AY' })
    expect(wrapper.find('[data-test="kar-analizi-urun-tablo"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('drawer açıkken dönem değişince kâr analizi yeni dönemle yenilenir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="kar-analizi-buton"]').trigger('click')
    await flushPromises()
    dashboardAPI.karAnalizi.mockClear()

    await wrapper.find('[data-test="donem-secici"]').setValue('BU_YIL')
    await flushPromises()

    expect(dashboardAPI.karAnalizi).toHaveBeenCalledWith({ donem: 'BU_YIL' })
    wrapper.unmount()
  })
})
