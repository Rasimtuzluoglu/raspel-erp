import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { masrafAPI } from '../../api/index.js'
import Masraflar from '../Masraflar.vue'

vi.mock('../../api/index.js', () => ({
  masrafAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [{ id: 1, tarih: '2026-10-01', kategori: 'OFIS', aciklama: 'Kırtasiye', tutar: 750 }],
          totalElements: 1
        }
      })
    ),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: { buAyToplam: 1500, buYilToplam: 9000, buYilKdv: 1800, toplamKayit: 14 }
      })
    ),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  },
  kasaAPI: {
    getAllKasalar: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  bankaAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  cariHesapAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  excelAPI: {
    masraflar: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'subtitle', 'icon'],
    template: '<div class="ph">{{ title }}<slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>'
  },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  EmptyState: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  InputNumber: true,
  Textarea: true,
  Dropdown: true,
  DatePicker: true,
  SatirEylemleri: true,
  MasrafDetayDrawer: true,
  SkeletonLoader: true
}

const kur = () =>
  mount(Masraflar, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Masraflar.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('listeyi ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(masrafAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(masrafAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Bu Ay Toplam:1500')
    expect(w.text()).toContain('Bu Yıl Toplam:9000')
    expect(w.text()).toContain('Bu Yıl KDV:1800')
    expect(w.text()).toContain('Kayıt Sayısı:14')
  })
})
