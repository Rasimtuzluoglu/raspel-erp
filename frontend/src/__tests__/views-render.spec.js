import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import CariHesaplar from '../views/CariHesaplar.vue'
import Stoklar from '../views/Stoklar.vue'

vi.mock('../api/index.js', () => ({
  cariHesapAPI: {
    getAll: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    create: vi.fn(), update: vi.fn(), delete: vi.fn(), search: vi.fn(),
    filtreli: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    ozet: vi.fn().mockResolvedValue({ data: { toplamKayit: 0, alacakli: 0, borclu: 0 } })
  },
  hareketAPI: { getByCariHesap: vi.fn().mockResolvedValue({ data: [] }) },
  notAPI: { cariNotlari: vi.fn().mockResolvedValue({ data: [] }) },
  excelAPI: { cariHesaplar: vi.fn(), stoklar: vi.fn() },
  stokAPI: {
    getAll: vi.fn().mockResolvedValue({ data: [] }),
    create: vi.fn().mockResolvedValue({ data: {} }),
    update: vi.fn(), delete: vi.fn(),
    filtreli: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    hareketEkle: vi.fn(), topluFiyat: vi.fn(), topluOlustur: vi.fn()
  },
  kategoriAPI: { getAll: vi.fn().mockResolvedValue({ data: [] }) },
  tedarikciAPI: { getAll: vi.fn().mockResolvedValue({ data: [] }) },
  bildirimAPI: { getAll: vi.fn().mockResolvedValue({ data: [] }) }
}))

const ortakStubs = {
  IlkZiyaretIpuclari: true,
  Toolbar: { template: '<div><slot name="start" /><slot name="end" /></div>' },
  TabloAyarlari: true,
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  EmptyState: true,
  Dialog: { template: '<div><slot /></div>' },
  AutoComplete: true, Dropdown: true, InputText: true, Button: true, DatePicker: true,
  Textarea: true, InputNumber: true, Checkbox: true, Message: true, SelectButton: true, InputSwitch: true,
  Tag: true, Skeleton: true, Avatar: true, Badge: true, Card: true, TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot /></div>' }
}

const calistir = (Bilesen, hataYakalandi) =>
  mount(Bilesen, {
    global: {
      plugins: [createPinia(), PrimeVue, ToastService, ConfirmationService],
      stubs: ortakStubs,
      config: { errorHandler: (err) => hataYakalandi.push(String(err && err.message)) }
    }
  })

describe('View crash tarama', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('CariHesaplar render edilebiliyor', async () => {
    const hataYakalandi = []
    const wrapper = calistir(CariHesaplar, hataYakalandi)
    await new Promise((r) => setTimeout(r, 80))
    expect(hataYakalandi).toEqual([])
    expect(wrapper.text()).toContain('Cari Hesaplar')
  })

  it('Stoklar render edilebiliyor', async () => {
    const hataYakalandi = []
    const wrapper = calistir(Stoklar, hataYakalandi)
    await new Promise((r) => setTimeout(r, 80))
    expect(hataYakalandi).toEqual([])
    expect(wrapper.text()).toContain('Stok')
  })
})
