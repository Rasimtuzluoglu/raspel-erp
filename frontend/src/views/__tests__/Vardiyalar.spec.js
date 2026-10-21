import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { vardiyaAPI } from '../../api/index.js'
import Vardiyalar from '../Vardiyalar.vue'

vi.mock('../../api/index.js', () => ({
  vardiyaAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 1, personelId: 1, personelAdi: 'Ahmet Yılmaz', tarih: '2026-10-01', tur: 'SABAH' }
          ],
          totalElements: 1
        }
      })
    ),
    ozet: vi.fn(() => Promise.resolve({ data: { toplam: 30, bugun: 4, buHafta: 11, personelSayisi: 7 } })),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  },
  personelAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [{ id: 1, ad: 'Ahmet', soyad: 'Yılmaz' }] } }))
  },
  excelAPI: {
    vardiyalar: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
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
  DataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  EmptyState: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  Dropdown: true,
  DatePicker: true,
  SatirEylemleri: true,
  VardiyaDetayDrawer: true,
  SkeletonLoader: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(Vardiyalar, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Vardiyalar.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('listeyi ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(vardiyaAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(vardiyaAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Vardiya:30')
    expect(w.text()).toContain('Bugün:4')
    expect(w.text()).toContain('Bu Hafta:11')
    expect(w.text()).toContain('Vardiyalı Personel:7')
  })
})
