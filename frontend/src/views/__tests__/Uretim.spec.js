import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { uretimAPI, stokAPI } from '../../api/index.js'
import Uretim from '../Uretim.vue'

vi.mock('../../api/index.js', () => ({
  uretimAPI: {
    receteler: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            id: 1,
            ad: 'Masa Reçetesi',
            urunId: 10,
            urunAd: 'Masa',
            aktif: true,
            revizyon: 2,
            fireOrani: 2,
            kalemler: [{ id: 1, hammaddeId: 1, hammaddeAd: 'MDF', miktar: 3, birim: 'adet', fireOrani: 1 }]
          }
        ]
      })
    ),
    emirlerFiltreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            {
              id: 1,
              urunId: 10,
              urunAd: 'Masa',
              miktar: 5,
              durum: 'TASLAK',
              oncelik: 'NORMAL',
              planlananBaslangic: '2026-09-01',
              planlananBitis: '2026-09-10',
              uretilenMiktar: null,
              fireMiktar: 0,
              hammaddeMaliyeti: 0,
              iscilikMaliyeti: 0,
              toplamMaliyet: 0,
              aciklama: ''
            },
            { id: 2, urunAd: 'Sandalye', miktar: 10, durum: 'URETIMDE', oncelik: 'YUKSEK', toplamMaliyet: 1500 }
          ],
          totalElements: 2
        }
      })
    ),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: {
          taslak: 1,
          uretimde: 1,
          tamamlandi: 3,
          iptal: 0,
          geciken: 1,
          buAyTamamlanan: 2,
          fireOrani: 2.5,
          ortalamaSureSaat: 4.25
        }
      })
    ),
    emirOlustur: vi.fn(() => Promise.resolve({ data: {} })),
    emirBaslat: vi.fn(() => Promise.resolve({ data: {} })),
    emirTamamla: vi.fn(() => Promise.resolve({ data: {} })),
    emirIptal: vi.fn(() => Promise.resolve({ data: {} })),
    emirGecmis: vi.fn(() =>
      Promise.resolve({
        data: [{ id: 1, yeniDurum: 'TASLAK', aciklama: 'Emir olusturuldu', olusturmaTarihi: '2026-09-01T10:00:00' }]
      })
    ),
    satinalmaTalebi: vi.fn(() => Promise.resolve({ data: {} })),
    ihtiyac: vi.fn(() =>
      Promise.resolve({
        data: {
          urunId: 10,
          urunAd: 'Masa',
          miktar: 5,
          fireOrani: 2,
          toplamMaliyet: 300,
          yeterli: false,
          kalemler: [{ hammaddeId: 1, hammaddeAd: 'MDF', gerekli: 15, mevcut: 5, eksik: 10, birimFiyat: 20, tutar: 300 }]
        }
      })
    ),
    receteOlustur: vi.fn(() => Promise.resolve({ data: {} })),
    receteGuncelle: vi.fn(() => Promise.resolve({ data: {} })),
    receteSil: vi.fn(() => Promise.resolve({ data: {} })),
    hatlar: vi.fn(() =>
      Promise.resolve({
        data: [
          { id: 1, ad: 'Hat A', kapasite: 4, aktif: true, aktifEmir: 2, gecikenEmir: 1, dolulukYuzde: 50 },
          { id: 2, ad: 'Hat B', kapasite: 2, aktif: false, aktifEmir: 0, gecikenEmir: 0, dolulukYuzde: 0 }
        ]
      })
    ),
    hatOlustur: vi.fn(() => Promise.resolve({ data: {} })),
    hatGuncelle: vi.fn(() => Promise.resolve({ data: {} })),
    hatSil: vi.fn(() => Promise.resolve({ data: {} })),
    emirHatAta: vi.fn(() => Promise.resolve({ data: {} })),
    analiz: vi.fn(() =>
      Promise.resolve({
        data: {
          toplamUretilen: 14,
          toplamFire: 1,
          fireOrani: 6.67,
          toplamMaliyet: 280,
          birimMaliyet: 20,
          ortalamaSureSaat: 4.5,
          tamamlananEmir: 2,
          urunler: [
            { urunId: 10, urunAd: 'Masa', emirSayisi: 2, uretilen: 14, fire: 1, fireOrani: 6.67, toplamMaliyet: 280, birimMaliyet: 20 }
          ],
          trend: [{ donem: '2026-09', uretilen: 14, fire: 1, maliyet: 280 }]
        }
      })
    ),
    takvim: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            id: 1,
            urunAd: 'Masa',
            durum: 'URETIMDE',
            hatAd: 'Hat A',
            planlananBaslangic: '2026-09-03',
            planlananBitis: '2026-09-08'
          }
        ]
      })
    )
  },
  stokAPI: {
    getAll: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 10, ad: 'Masa' },
            { id: 1, ad: 'MDF' }
          ]
        }
      })
    )
  },
  excelAPI: {
    uretimEmirleri: vi.fn(() => Promise.resolve({ data: new Blob([]) })),
    uretimReceteler: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: { props: ['title'], template: '<div class="page-header-stub">{{ title }}<slot name="actions" /></div>' },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi-stub">{{ baslik }}:{{ deger }}</div>'
  },
  Button: true,
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  DatePicker: true,
  Textarea: true,
  InputSwitch: true,
  ToggleSwitch: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Tag: true,
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { props: ['header'], template: '<div><span class="tab-stub">{{ header }}</span><slot /></div>' },
  Column: true,
  SatirEylemleri: true,
  EmptyState: true,
  SkeletonLoader: { props: ['count', 'height'], template: '<div class="skeleton-stub" />' },
  UretimEmriDetayDrawer: true
}

describe('Uretim.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
    window.URL.createObjectURL = vi.fn(() => 'blob:uretim')
    window.URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  it('sayfayi hatasiz render eder ve verileri yukler', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.uretim-sayfasi').exists()).toBe(true)
    expect(uretimAPI.receteler).toHaveBeenCalledTimes(1)
    expect(uretimAPI.emirlerFiltreli).toHaveBeenCalledTimes(1)
    expect(uretimAPI.ozet).toHaveBeenCalledTimes(1)
    expect(stokAPI.getAll).toHaveBeenCalledTimes(1)
  })

  it('ozet KPI degerlerini gosterir', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    const metin = wrapper.text()
    expect(metin).toContain('Bu Ay Tamamlanan:2')
    expect(metin).toContain('Geciken:1')
    expect(metin).toContain('Üretimde:1')
  })

  it('sekmeleri render eder', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    const metin = wrapper.text()
    expect(metin).toContain('Emirler')
    expect(metin).toContain('Reçeteler')
    expect(metin).toContain('Planlama')
    expect(metin).toContain('Rapor')
  })

  it('reçete kalemini baz miktara göre ölçekler (1 birim -> N birim)', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    const k = { hammaddeId: 1, miktar: 3, fireOrani: 0 }
    wrapper.vm.receteForm.bazMiktar = 1
    wrapper.vm.receteForm.fireOrani = 0
    wrapper.vm.onizlemeMiktar = 10
    expect(wrapper.vm.kalemHesaplanan(k)).toBe(30) // 10/1 * 3
    wrapper.vm.receteForm.bazMiktar = 2
    expect(wrapper.vm.kalemHesaplanan(k)).toBe(15) // 10/2 * 3
  })

  it('kaydederken bazMiktar ve bazBirim gönderir', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.receteForm.ad = 'Kola'
    wrapper.vm.receteForm.urunId = 10
    wrapper.vm.receteForm.bazMiktar = 2
    wrapper.vm.receteForm.bazBirim = 'litre'
    wrapper.vm.receteForm.kalemler = [{ hammaddeId: 1, miktar: 3, birim: 'kg', fireOrani: 0 }]
    await wrapper.vm.receteKaydet()
    expect(uretimAPI.receteOlustur).toHaveBeenCalledWith(
      expect.objectContaining({ bazMiktar: 2, bazBirim: 'litre' })
    )
  })

  it('son açık sekmeyi localStorage’dan geri yükler ve değişince saklar', async () => {
    localStorage.setItem('raspel_uretim_tab', '3')
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.vm.aktifSekme).toBe(3)
    wrapper.vm.aktifSekme = 1
    await flushPromises()
    expect(localStorage.getItem('raspel_uretim_tab')).toBe('1')
  })

  it('emir filtrelerini localStorage’dan geri yükler ve değişince saklar', async () => {
    localStorage.setItem(
      'raspel_uretim_emir_filtre',
      JSON.stringify({
        q: 'masa',
        durum: 'URETIMDE',
        oncelik: 'YUKSEK',
        geciken: true,
        buAy: false,
        baslangic: '2026-09-01',
        bitis: '2026-09-30'
      })
    )
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.vm.emirQ).toBe('masa')
    expect(wrapper.vm.emirDurum).toBe('URETIMDE')
    expect(wrapper.vm.emirOncelik).toBe('YUKSEK')
    expect(wrapper.vm.emirGeciken).toBe(true)
    expect(wrapper.vm.emirBaslangic.getFullYear()).toBe(2026)
    expect(uretimAPI.emirlerFiltreli).toHaveBeenCalledWith(
      expect.objectContaining({ q: 'masa', durum: 'URETIMDE', geciken: true, baslangic: '2026-09-01' })
    )

    wrapper.vm.emirQ = 'sandalye'
    await flushPromises()
    expect(JSON.parse(localStorage.getItem('raspel_uretim_emir_filtre')).q).toBe('sandalye')
  })

  it('yüklenirken iskelet gösterir', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    expect(wrapper.vm.kpiIskelet).toBe(true)
    await flushPromises()
    expect(wrapper.vm.kpiIskelet).toBe(false)
    expect(wrapper.find('.kpi-satiri').exists()).toBe(true)
  })

  it('liste sonucunu ekran okuyucuya duyurur ve çipler aria-pressed taşır', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    const canli = wrapper.find('.sr-only[aria-live="polite"]')
    expect(canli.exists()).toBe(true)
    expect(canli.text()).toContain('2 emirden 2 tanesi')
    const cipler = wrapper.findAll('.emir-cip')
    expect(cipler.length).toBeGreaterThan(0)
    expect(cipler[0].attributes('aria-pressed')).toBe('true')
    expect(wrapper.find('.emir-cipler').attributes('role')).toBe('group')
  })

  it('reçete araması yapar ve Excel’e q/aktif parametrelerini gönderir', async () => {
    const { excelAPI } = await import('../../api/index.js')
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.receteQ = 'masa'
    await flushPromises()
    expect(wrapper.vm.filtreliReceteler).toHaveLength(1)

    wrapper.vm.receteDurum = 'AKTIF'
    await wrapper.vm.receteExcelIndir()
    expect(excelAPI.uretimReceteler).toHaveBeenCalledWith({ q: 'masa', aktif: true })
  })

  it('hatları yükler, filtreye hatId gönderir ve doluluğu gösterir', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(uretimAPI.hatlar).toHaveBeenCalled()
    expect(wrapper.vm.hatlar).toHaveLength(2)
    expect(wrapper.text()).toContain('Hat A')
    expect(wrapper.text()).toContain('50%')

    vi.clearAllMocks()
    wrapper.vm.emirHat = 1
    wrapper.vm.emirleriYukle()
    await flushPromises()
    expect(uretimAPI.emirlerFiltreli).toHaveBeenLastCalledWith(expect.objectContaining({ hatId: 1 }))
  })

  it('hat atamasını API’ye iletir ve listeyi yeniler', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.hatAtaDialogAc({ id: 2, urunAd: 'Sandalye', hatId: null })
    wrapper.vm.hatAtaSecim = 1
    await wrapper.vm.hatAtaKaydet()
    expect(uretimAPI.emirHatAta).toHaveBeenCalledWith(2, 1)
    expect(uretimAPI.hatlar).toHaveBeenCalled()
  })

  it('hat formunu düzenleme için doldurur ve kaydeder', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.hatDialogAc(wrapper.vm.hatlar[0])
    expect(wrapper.vm.hatForm.id).toBe(1)
    expect(wrapper.vm.hatForm.ad).toBe('Hat A')
    wrapper.vm.hatForm.kapasite = 6
    await wrapper.vm.hatKaydet()
    expect(uretimAPI.hatGuncelle).toHaveBeenCalledWith(1, expect.objectContaining({ ad: 'Hat A', kapasite: 6 }))
  })

  it('analiz sekmesi açılınca dönem analizini yükler', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(uretimAPI.analiz).not.toHaveBeenCalled()

    wrapper.vm.aktifSekme = 5
    await flushPromises()

    expect(uretimAPI.analiz).toHaveBeenCalledTimes(1)
    expect(uretimAPI.analiz).toHaveBeenCalledWith(
      expect.objectContaining({ baslangic: expect.any(String), bitis: expect.any(String) })
    )
    expect(wrapper.text()).toContain('Aylık Seyir')
    expect(wrapper.text()).toContain('Masa')
    expect(wrapper.vm.barYuzde(14)).toBe(100)
    expect(wrapper.vm.donemEtiket('2026-09')).toContain('2026')
  })

  it('takvim sekmesi Gantt çubuklarını üretir ve ay gezinmesi API’yi çağırır', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()

    wrapper.vm.takvimAy = new Date(2026, 8, 1)
    wrapper.vm.aktifSekme = 6
    await flushPromises()

    expect(uretimAPI.takvim).toHaveBeenCalledWith(
      expect.objectContaining({ baslangic: '2026-09-01', bitis: '2026-09-30' })
    )
    expect(wrapper.vm.takvimSatirlari).toHaveLength(1)
    expect(wrapper.vm.takvimSatirlari[0].bar.gunSayisi).toBe(6)
    expect(wrapper.vm.takvimSatirlari[0].bar.left).toBeCloseTo((2 / 30) * 100, 5)
    expect(wrapper.text()).toContain('6g')

    vi.clearAllMocks()
    wrapper.vm.takvimAyDegistir(1)
    await flushPromises()
    expect(uretimAPI.takvim).toHaveBeenCalledWith(
      expect.objectContaining({ baslangic: '2026-10-01' })
    )
  })
})
