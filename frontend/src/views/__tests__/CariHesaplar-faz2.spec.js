import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import CariHesaplar from '../CariHesaplar.vue'

/**
 * FAZ 2 — Yeni/Duzenle cari formu: iki kolon + bolum kartlari.
 *
 * Sozlesme: form govdesi iki kolona bolunur (`.form-kolon`), her bolum bir
 * karttir (`.form-section`) ve "Ek Bilgiler" tam genislik alir
 * (`.form-section-genis`). Alan adlari ve davranis degismedi.
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
  PageHeader: { props: ['title'], template: '<header class="ph">{{ title }}<slot name="actions" /></header>' },
  KpiKart: true,
  Menu: true,
  Paginator: true,
  IlkZiyaretIpuclari: true,
  TabloAyarlari: true,
  SatirEylemleri: true,
  EmptyState: true,
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  AutoComplete: true,
  Button: { template: '<button><slot /></button>' },
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

describe('CariHesaplar — Faz2 form duzeni', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('form iki kolona bolunur', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.findAll('.dialog-form .form-kolon')).toHaveLength(2)
  })

  it('her bolum bir karttir ve Ek Bilgiler tam genislik alir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    const bolumler = w.findAll('.dialog-form .form-section')
    expect(bolumler.length).toBeGreaterThanOrEqual(5)
    expect(w.find('.dialog-form .form-section-genis').exists()).toBe(true)
  })

  it('temel alanlar korunur (ad, iban, kredi, temsilci)', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    const html = w.find('.dialog-form').html()
    expect(html).toContain('id="ad"')
    expect(html).toContain('id="iban"')
    expect(html).toContain('id="krediLimiti"')
    expect(html).toContain('id="temsilciId"')
  })
})
