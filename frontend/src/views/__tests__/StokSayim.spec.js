import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { stokSayimAPI } from '../../api/index.js'
import StokSayim from '../StokSayim.vue'

vi.mock('../../api/index.js', () => ({
  stokSayimAPI: {
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            {
              id: 1,
              tarih: '2026-10-01',
              stokAdi: 'Masa',
              beklenenMiktar: 10,
              sayilanMiktar: 8,
              fark: -2,
              durum: 'TAMAMLANDI'
            },
            {
              id: 2,
              tarih: '2026-10-02',
              stokAdi: 'Sandalye',
              beklenenMiktar: 5,
              sayilanMiktar: 5,
              fark: 0,
              durum: 'TASLAK'
            }
          ],
          totalElements: 2
        }
      })
    ),
    ozet: vi.fn(() =>
      Promise.resolve({ data: { toplam: 2, taslak: 1, tamamlanan: 1, iptal: 0, farkli: 1, bugun: 0 } })
    ),
    tara: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    durumGuncelle: vi.fn(),
    delete: vi.fn()
  },
  stokAPI: {
    ara: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  excelAPI: {
    stokSayim: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: { props: ['title', 'subtitle', 'icon'], template: '<div class="ph">{{ title }}<slot name="actions" /></div>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  Button: { template: '<button><slot /></button>' },
  InputNumber: true,
  InputText: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Dialog: { template: '<div><slot /><slot name="header" /><slot name="footer" /></div>' },
  DatePicker: true,
  Dropdown: true,
  Textarea: true,
  Tag: { props: ['value', 'severity'], template: '<span class="tag">{{ value }}</span>' },
  SatirEylemleri: true,
  StokSayimDetayDrawer: true,
  SkeletonLoader: true,
  EmptyState: true,
  BarcodeScannerModal: true
}

const kur = () =>
  mount(StokSayim, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('StokSayim.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('liste ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(stokSayimAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(stokSayimAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Sayım')
  })
})
