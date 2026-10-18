import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import CariHesaplar from '../CariHesaplar.vue'

/**
 * FAZ 4 — Hareket & Ekstre dialogu: hizli tarih araligi + yazdirma.
 *
 * Ekstre dialogu 360 basligindan acilir; burada hizli aralik secimleri
 * (Bu Ay / Gecen Ay / Bu Yil / Tumu), tarih araligi alanlari ve "Ekstre Yazdir"
 * butonunun varligi ve temel etkilesimi sabitlenir.
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

describe('CariHesaplar — Faz4 hareket/ekstre', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('ekstre dialogunda hizli aralik secimleri vardir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    const hizli = w.find('.ekstre-hizli')
    expect(hizli.exists()).toBe(true)
    expect(hizli.findAll('button')).toHaveLength(4)
  })

  it('iki tarih alani ve ekstre yazdir butonu bulunur', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.findAll('.ekstre-tarih').length).toBe(2)
    const btn = w.findAll('button').find((b) => b.text().includes(i18n.global.t('cariHesaplar.ekstreYazdir')))
    expect(btn).toBeTruthy()
  })

  it('ekstre dialog basligi ikonlu baslik kullanir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.find('.hareket-baslik').exists()).toBe(true)
  })
})
