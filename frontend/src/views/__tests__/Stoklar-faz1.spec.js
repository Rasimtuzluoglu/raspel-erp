import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import Stoklar from '../Stoklar.vue'

/**
 * FAZ 1 — Stoklar liste ekrani kullanislilik sozlesmesi.
 * PageHeader (ikonlu) + "Islemler" menusu + 4 KPI karti (backend ozet).
 */

vi.mock('../../api/index.js', () => ({
  stokAPI: {
    filtreli: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    getAll: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    ozet: vi.fn().mockResolvedValue({
      data: { toplamUrun: 42, kritikSayi: 3, toplamDeger: 1234, kategoriSayisi: 5 }
    }),
    gruplamaDagilimi: vi.fn().mockResolvedValue({ data: { kategoriler: [], stokGruplari: [] } }),
    getHareketler: vi.fn().mockResolvedValue({ data: [] }),
    barkodOnerisi: vi.fn().mockResolvedValue({ data: {} }),
    barkodUret: vi.fn().mockResolvedValue({ data: { uretildi: 0 } })
  },
  cariHesapAPI: {
    filtreli: vi.fn().mockResolvedValue({ data: { content: [] } }),
    getAllCariHesaplar: vi.fn().mockResolvedValue({ data: [] })
  },
  depoAPI: {
    getAll: vi.fn().mockResolvedValue({ data: [] }),
    stokDagilimi: vi.fn().mockResolvedValue({ data: [] })
  },
  raporAPI: {
    stokDegerleme: vi.fn().mockResolvedValue({ data: null }),
    siparisOnerisi: vi.fn().mockResolvedValue({ data: [] })
  },
  excelAPI: { stoklar: vi.fn().mockResolvedValue({ data: new Blob() }) },
  uploadAPI: { yukle: vi.fn() }
}))

const stubs = {
  PageHeader: { props: ['title'], template: '<header class="ph">{{ title }}<slot name="actions" /></header>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}|{{ deger }}</div>' },
  Menu: { template: '<div class="islem-menu" />' },
  IlkZiyaretIpuclari: true,
  ListeDurumu: true,
  EmptyState: true,
  Toolbar: { template: '<div><slot name="start" /><slot name="end" /></div>' },
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  AutoComplete: true,
  Select: true,
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  IconField: { template: '<span><slot /></span>' },
  InputIcon: true,
  Button: { template: '<button><slot /></button>' },
  Message: true,
  Divider: true,
  Checkbox: true,
  Tag: true,
  ToggleSwitch: true
}

const kur = () =>
  mount(Stoklar, {
    global: {
      plugins: [createPinia(), PrimeVue, ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Stoklar — Faz1 liste ekrani', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('PageHeader basligi ve islem menuesu gosterilir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 80))
    expect(w.find('.ph').exists()).toBe(true)
    expect(w.find('.ph').text()).toContain('Stok')
    expect(w.find('.stok-islem-tetik').exists()).toBe(true)
    expect(w.find('.islem-menu').exists()).toBe(true)
  })

  it('dort KPI karti render edilir ve ozet degerleri basilir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 80))
    const kartlar = w.findAll('.stok-kpi .kpi')
    expect(kartlar).toHaveLength(4)
    expect(w.find('.stok-kpi').text()).toContain('42')
  })
})

describe('Stoklar — Faz2 form duzeni', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('form dialogu iki kolon bolum kartlari kullanir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 60))
    expect(w.find('.stok-form-dialog').exists()).toBe(true)
    expect(w.findAll('.stok-form-dialog .form-section').length).toBeGreaterThanOrEqual(4)
  })
})
