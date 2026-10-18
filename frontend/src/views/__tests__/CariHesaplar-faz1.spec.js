import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import CariHesaplar from '../CariHesaplar.vue'

/**
 * FAZ 1 — Cari Hesaplar liste ekrani kullanislilik sozlesmesi.
 *
 * Amac: revizyonun KULLANICIYA DONUK yapi taslarini sabitlemek:
 *  - PageHeader (tek tip baslik),
 *  - KpiKart ile 3 ozet karti,
 *  - araç cubugunda arama (IconField) + filtreler + "Islemler" menu tetigi,
 *  - kucuk ekran icin kart listesi konteyneri.
 * Davranis (API cagrilari, secim, sayfalama) degismedi; bu test onu belgeler.
 */

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: {
    getAll: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    search: vi.fn(),
    filtreli: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    ozet: vi.fn().mockResolvedValue({ data: { toplamKayit: 12, alacakli: 5000, borclu: 1500 } }),
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
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}</div>' },
  Menu: { template: '<div class="islem-menu" />' },
  Paginator: true,
  IlkZiyaretIpuclari: true,
  TabloAyarlari: true,
  SatirEylemleri: true,
  EmptyState: true,
  DataTable: { template: '<div class="tablo"><slot /></div>' },
  Column: true,
  Dialog: { template: '<div><slot /></div>' },
  AutoComplete: true,
  Button: { template: '<button><slot /></button>' },
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  InputSwitch: true,
  IconField: { template: '<span class="iconfield"><slot /></span>' },
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

const kur = (hata) =>
  mount(CariHesaplar, {
    global: {
      plugins: [createPinia(), PrimeVue, ToastService, ConfirmationService, i18n],
      stubs,
      config: { errorHandler: (err) => hata.push(String(err && err.message)) }
    }
  })

describe('CariHesaplar — Faz1 liste ekrani', () => {
  beforeEach(() => setActivePinia(createPinia()))

  it('PageHeader basligi gosterilir', async () => {
    const hata = []
    const w = kur(hata)
    await new Promise((r) => setTimeout(r, 60))
    expect(hata).toEqual([])
    expect(w.find('.ph').exists()).toBe(true)
    expect(w.find('.ph').text()).toContain('Cari Hesaplar')
  })

  it('uc adet KpiKart ozet karti render edilir', async () => {
    const w = kur([])
    await new Promise((r) => setTimeout(r, 60))
    const kartlar = w.findAll('.kpi')
    expect(kartlar).toHaveLength(3)
  })

  it('arac cubugunda arama, filtreler ve islem menu tetigi vardir', async () => {
    const w = kur([])
    await new Promise((r) => setTimeout(r, 60))
    expect(w.find('.cari-filtreler').exists()).toBe(true)
    expect(w.find('.cari-arama').exists()).toBe(true)
    expect(w.find('.cari-islem-tetik').exists()).toBe(true)
    expect(w.find('.islem-menu').exists()).toBe(true)
  })

  it('kucuk ekran kart listesi konteyneri DOM\'da bulunur (CSS ile gizlenir)', async () => {
    const w = kur([])
    await new Promise((r) => setTimeout(r, 60))
    expect(w.find('.cari-kart-liste').exists()).toBe(true)
  })
})
