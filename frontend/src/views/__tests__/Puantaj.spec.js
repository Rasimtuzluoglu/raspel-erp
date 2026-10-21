import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { puantajAPI } from '../../api/index.js'
import Puantaj from '../Puantaj.vue'

vi.mock('../../api/index.js', () => ({
  puantajAPI: {
    ozet: vi.fn(() => Promise.resolve({ data: { toplam: 9, geldi: 5, gelmedi: 2, izinli: 1, mazeretli: 1 } })),
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 1, personelId: 7, personelAdi: 'Ahmet Yılmaz', tarih: '2026-10-01', durum: 'GELDI' }
          ],
          totalElements: 1
        }
      })
    ),
    getByPersonel: vi.fn(() => Promise.resolve({ data: [] })),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  },
  personelAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [{ id: 7, ad: 'Ahmet', soyad: 'Yılmaz' }] } }))
  },
  excelAPI: {
    puantaj: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
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
  Toolbar: { template: '<div><slot name="start" /><slot name="end" /></div>' },
  DataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  EmptyState: true,
  Message: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  Textarea: true,
  Dropdown: true,
  DatePicker: true,
  SatirEylemleri: true,
  PuantajDetayDrawer: true,
  SkeletonLoader: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(Puantaj, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Puantaj.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('personel, puantaj listesi ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(puantajAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(puantajAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Geldi:5')
    expect(w.text()).toContain('Gelmedi:2')
    expect(w.text()).toContain('Mazeretli:1')
  })
})
