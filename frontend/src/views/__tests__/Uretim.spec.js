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
    emirler: vi.fn(() =>
      Promise.resolve({
        data: [
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
        ]
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
    receteSil: vi.fn(() => Promise.resolve({ data: {} }))
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
  Tag: true,
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { props: ['header'], template: '<div><span class="tab-stub">{{ header }}</span><slot /></div>' },
  Column: true,
  SatirEylemleri: true,
  EmptyState: true
}

describe('Uretim.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('sayfayi hatasiz render eder ve verileri yukler', async () => {
    const wrapper = mount(Uretim, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.uretim-sayfasi').exists()).toBe(true)
    expect(uretimAPI.receteler).toHaveBeenCalledTimes(1)
    expect(uretimAPI.emirler).toHaveBeenCalledTimes(1)
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
})
