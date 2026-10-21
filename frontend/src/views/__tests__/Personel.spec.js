import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { personelAPI } from '../../api/index.js'
import Personel from '../Personel.vue'

vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRouter: () => ({ push: vi.fn(), replace: vi.fn() }),
    useRoute: () => ({ query: {} })
  }
})

vi.mock('../../api/index.js', () => ({
  personelAPI: {
    getAll: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 1, ad: 'Ahmet', soyad: 'Yılmaz', departman: 'IT', aktif: true },
            { id: 2, ad: 'Ayşe', soyad: 'Kaya', departman: 'Muhasebe', aktif: false }
          ]
        }
      })
    ),
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 1, ad: 'Ahmet', soyad: 'Yılmaz', departman: 'IT', aktif: true },
            { id: 2, ad: 'Ayşe', soyad: 'Kaya', departman: 'Muhasebe', aktif: false }
          ],
          totalElements: 2
        }
      })
    ),
    departmanlar: vi.fn(() => Promise.resolve({ data: ['IT', 'Muhasebe'] })),
    detay: vi.fn(() =>
      Promise.resolve({
        data: {
          personel: { id: 1, ad: 'Ahmet', soyad: 'Yılmaz', departman: 'IT', aktif: true },
          sonIzinler: [],
          bugunIzinli: false,
          bekleyenIzin: 0,
          puantajKayitSayisi: 0,
          geldi: 0,
          gelmedi: 0,
          izinli: 0,
          mazeretli: 0
        }
      })
    ),
    ozet: vi.fn(() => Promise.resolve({ data: { toplam: 2, aktif: 1, pasif: 1, bekleyenIzin: 3 } })),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  },
  personelIzinAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    create: vi.fn()
  },
  kullaniciAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } }))
  },
  excelAPI: {
    personel: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'subtitle', 'icon'],
    template: '<div class="ph">{{ title }}<slot name="actions" /></div>'
  },
  IlkZiyaretIpuclari: true,
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>'
  },
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot /></div>' },
  Column: true,
  EmptyState: true,
  SatirEylemleri: true,
  PersonelDetayDrawer: true,
  SkeletonLoader: true,
  Toolbar: { template: '<div><slot name="start" /><slot name="end" /></div>' },
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot /></div>' },
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  InputNumber: true,
  Textarea: true,
  InputSwitch: true,
  DatePicker: true,
  Dropdown: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(Personel, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Personel.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('listeyi, departmanları ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(personelAPI.filtreli).toHaveBeenCalledTimes(1)
    expect(personelAPI.departmanlar).toHaveBeenCalledTimes(1)
    expect(personelAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Personel')
    expect(w.text()).toContain('Toplam Personel:2')
    expect(w.text()).toContain('Bekleyen İzin:3')
    expect(w.text()).toContain('Personel')
  })
})
