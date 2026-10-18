import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import CariHesaplar from '../CariHesaplar.vue'

/**
 * FAZ 5 — Ikincil dialoglar modernizasyonu.
 *
 * Adres / Riskli Cariler / Toplu Guncelle / Toplu E-posta / Acilis dialoglari
 * ortak kalibi kullanir: ikonlu baslik (`.dialog-baslik`), bilgi cipligi
 * (`.toplu-secim-bilgi`) ve tutarli genislik (maxWidth 96vw).
 */

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: {
    getAll: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    search: vi.fn(),
    filtreli: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    ozet: vi.fn().mockResolvedValue({ data: { toplamKayit: 0, alacakli: 0, borclu: 0 } }),
    riskliCariler: vi.fn().mockResolvedValue({ data: [] }),
    adresler: vi.fn().mockResolvedValue({ data: [] }),
    topluGuncelle: vi.fn(),
    topluSil: vi.fn()
  },
  hareketAPI: { getByCariHesap: vi.fn().mockResolvedValue({ data: [] }) },
  notAPI: { cariNotlari: vi.fn().mockResolvedValue({ data: [] }) },
  excelAPI: { cariHesaplar: vi.fn(), stoklar: vi.fn() },
  faturaAPI: { getByCariHesap: vi.fn().mockResolvedValue({ data: [] }) },
  stokAPI: { getAll: vi.fn().mockResolvedValue({ data: [] }) },
  kullaniciAPI: { getAll: vi.fn().mockResolvedValue({ data: { content: [] } }) },
  raporAPI: {},
  uploadAPI: { yukle: vi.fn() }
}))

const stubs = {
  PageHeader: { props: ['title'], template: '<header>{{ title }}<slot name="actions" /></header>' },
  KpiKart: true,
  Menu: true,
  Paginator: true,
  IlkZiyaretIpuclari: true,
  TabloAyarlari: true,
  SatirEylemleri: true,
  EmptyState: true,
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  Dialog: { template: '<div><slot name="header" /><slot /><slot name="footer" /></div>' },
  Divider: true,
  AutoComplete: true,
  Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  InputSwitch: true,
  IconField: { template: '<span><slot /></span>' },
  InputIcon: true,
  DatePicker: true,
  Textarea: true,
  Checkbox: true,
  Message: true,
  SelectButton: true,
  Tag: true,
  Skeleton: true,
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot /></div>' }
}

const kur = () =>
  mount(CariHesaplar, {
    global: {
      plugins: [createPinia(), PrimeVue, ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('CariHesaplar — Faz5 ikincil dialoglar', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('ikincil dialoglar ikonlu ortak baslik kullanir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.findAll('.dialog-baslik').length).toBeGreaterThanOrEqual(5)
  })

  it('adres dialogunda adres formu grid yapidadir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.find('.adres-form').exists()).toBe(true)
  })

  it('secim bilgi cipligi birden fazla dialogda kullanilir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.findAll('.toplu-secim-bilgi').length).toBeGreaterThanOrEqual(3)
  })
})
