import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { subeAPI } from '../../api/index.js'
import Subeler from '../Subeler.vue'

vi.mock('../../api/index.js', () => ({
  subeAPI: {
    getAll: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            {
              id: 1,
              ad: 'Merkez Şube',
              yetkili: 'Ahmet',
              telefon: '0212 000 00 00',
              adres: 'İstanbul',
              aktif: true
            },
            {
              id: 2,
              ad: 'Ankara Şube',
              yetkili: 'Ayşe',
              telefon: '0312 000 00 00',
              adres: 'Ankara',
              aktif: false
            }
          ]
        }
      })
    ),
    ozet: vi.fn(() =>
      Promise.resolve({ data: { toplam: 2, aktif: 1, pasif: 1, depo: 3 } })
    ),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    depolar: vi.fn(() => Promise.resolve({ data: [] }))
  },
  excelAPI: {
    subeler: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: { props: ['title', 'subtitle', 'icon'], template: '<div class="ph">{{ title }}<slot name="actions" /></div>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  EmptyState: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  Textarea: true,
  InputSwitch: true,
  SatirEylemleri: true,
  SubeDetayDrawer: true,
  SkeletonLoader: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(Subeler, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Subeler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('listeyi ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(subeAPI.getAll).toHaveBeenCalledTimes(1)
    expect(subeAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Şube')
  })
})
