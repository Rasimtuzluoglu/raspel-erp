import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { taksitAPI } from '../../api/index.js'
import TaksitTakvimi from '../TaksitTakvimi.vue'

vi.mock('../../api/index.js', () => ({
  taksitAPI: {
    takvim: vi.fn(() => Promise.resolve({ data: [] })),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: {
          bekleyenToplam: 6000,
          gecikmisToplam: 2000,
          buAyToplam: 1500,
          odenenBuYilToplam: 9000,
          bekleyenAdet: 5,
          gecikmisAdet: 2,
          odenenBuYilAdet: 8
        }
      })
    ),
    yaklasan: vi.fn(() => Promise.resolve({ data: [] })),
    planOlustur: vi.fn(),
    sil: vi.fn(),
    planSil: vi.fn(),
    listele: vi.fn(() => Promise.resolve({ data: { content: [], totalElements: 0 } })),
    planlar: vi.fn(() => Promise.resolve({ data: [] }))
  },
  cariHesapAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  tahsilatAPI: {
    gir: vi.fn()
  },
  kasaAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  bankaAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  excelAPI: {
    taksitler: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
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
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: { template: '<div class="tab"><slot /></div>' },
  DataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  EmptyState: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputNumber: true,
  InputText: true,
  Textarea: true,
  DatePicker: true,
  Select: true,
  Dropdown: true,
  SelectButton: true,
  SatirEylemleri: true,
  SkeletonLoader: true,
  TaksitDetayDrawer: true,
  TaksitPlanDrawer: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(TaksitTakvimi, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('TaksitTakvimi.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('takvim, yaklaşan liste ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(taksitAPI.takvim).toHaveBeenCalledTimes(1)
    expect(taksitAPI.listele).toHaveBeenCalledTimes(1)
    expect(taksitAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Bekleyen:6000')
    expect(w.text()).toContain('Gecikmiş:2000')
    expect(w.text()).toContain('Bu Ay Vadesi Gelen:1500')
    expect(w.text()).toContain('Bu Yıl Ödenen:9000')
  })
})
