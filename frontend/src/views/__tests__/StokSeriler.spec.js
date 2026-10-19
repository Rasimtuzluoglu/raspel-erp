import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { stokSeriAPI } from '../../api/index.js'
import StokSeriler from '../StokSeriler.vue'

vi.mock('../../api/index.js', () => ({
  stokSeriAPI: {
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            {
              id: 1,
              stokAdi: 'Masa',
              seriNo: 'SN-1',
              lotNo: 'L-1',
              miktar: 10,
              kalanMiktar: 8,
              depoId: 1,
              sonKullanmaTarihi: '2026-12-01',
              durum: 'STOKTA'
            }
          ],
          totalElements: 1
        }
      })
    ),
    ozet: vi.fn(() =>
      Promise.resolve({ data: { toplam: 1, stokta: 1, tuketildi: 0, sktYaklasan: 0, sktGecmis: 0 } })
    ),
    sonKullanma: vi.fn(() => Promise.resolve({ data: [] }))
  },
  stokAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [{ id: 1, ad: 'Masa' }] } }))
  },
  depoAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: [] }))
  },
  excelAPI: {
    stokSeri: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: { props: ['title', 'subtitle', 'icon'], template: '<div class="ph">{{ title }}<slot name="actions" /></div>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="header" /><slot name="footer" /></div>' },
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  DatePicker: true,
  Tag: { props: ['value', 'severity'], template: '<span class="tag">{{ value }}</span>' },
  SatirEylemleri: true,
  StokSeriDetayDrawer: true,
  SkeletonLoader: true,
  EmptyState: true
}

const kur = () =>
  mount(StokSeriler, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('StokSeriler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('listeyi ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(stokSeriAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(stokSeriAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Seri/Lot')
  })
})
