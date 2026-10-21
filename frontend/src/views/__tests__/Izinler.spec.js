import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { personelIzinAPI, personelAPI } from '../../api/index.js'
import Izinler from '../Izinler.vue'

vi.mock('../../api/index.js', () => ({
  personelIzinAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 1, personelAdi: 'Ahmet Yılmaz', izinTuru: 'YILLIK_IZIN', durum: 'BEKLEMEDE' },
            { id: 2, personelAdi: 'Ayşe Kaya', izinTuru: 'HASTA_IZNI', durum: 'ONAYLANDI' }
          ],
          totalElements: 2
        }
      })
    ),
    ozet: vi.fn(() => Promise.resolve({ data: { bekleyen: 1, onaylandi: 1, reddedildi: 0, bugunIzinli: 2 } })),
    getById: vi.fn(() =>
      Promise.resolve({
        data: { id: 1, personelAdi: 'Ahmet Yılmaz', izinTuru: 'YILLIK_IZIN', durum: 'BEKLEMEDE', gunSayisi: 3 }
      })
    ),
    durumGuncelle: vi.fn(),
    delete: vi.fn()
  },
  personelAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  excelAPI: {
    izinler: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
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
  SelectButton: true,
  Button: { template: '<button><slot /></button>' },
  InputText: true,
  Dropdown: true,
  DatePicker: true,
  SatirEylemleri: true,
  IzinDetayDrawer: true,
  SkeletonLoader: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(Izinler, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Izinler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('listeyi ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(personelIzinAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(personelAPI.getAll).toHaveBeenCalledTimes(1)
    expect(personelIzinAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Bekleyen:1')
    expect(w.text()).toContain('Onaylanan:1')
    expect(w.text()).toContain('Bugün İzinli:2')
  })
})
