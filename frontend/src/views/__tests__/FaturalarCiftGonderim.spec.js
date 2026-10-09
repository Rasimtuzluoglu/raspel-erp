import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'

const postAxios = vi.fn(() => Promise.resolve({ data: { id: 1 } }))

vi.mock('axios', () => {
  const instance = {
    get: vi.fn(() => Promise.resolve({ data: [] })),
    post: (...a) => postAxios(...a),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
  }
  return { default: { ...instance, create: vi.fn(() => instance) } }
})

const mountFaturalar = async () => {
  const Faturalar = (await import('../Faturalar.vue')).default
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(Faturalar, {
    global: {
      stubs: { DataTable: true, Column: true, Dialog: true, Button: true, InputText: true, InputNumber: true, Dropdown: true, DatePicker: true, Card: true, Message: true, Tag: true, Textarea: true, TabView: true, TabPanel: true, Select: true, Checkbox: true, IconField: true, InputIcon: true, Skeleton: true, ProgressBar: true },
      plugins: [pinia, PrimeVue, ToastService, ConfirmationService, i18n]
    }
  })
  await flushPromises()
  return wrapper
}

const gecerliForm = (w) => {
  w.vm.form = {
    tur: 'SATIS',
    cariHesapId: 3,
    tarih: '2026-01-15',
    aciklama: '',
    kalemler: [{ aciklama: 'Ürün', adet: 1, birimFiyat: 100 }],
    teslimEden: '',
    teslimDurumu: 'BEKLIYOR',
    teslimNotu: '',
    depoId: null,
    paraBirimi: 'TRY',
    driverId: null,
    teslimatAdresi: null,
    beklenenTeslimTarihi: null
  }
  w.vm.editingId = null
}

describe('Faturalar.vue - cift gonderim engeli', () => {
  beforeEach(() => {
    postAxios.mockClear()
    postAxios.mockImplementation(() => Promise.resolve({ data: { id: 1 } }))
  })

  it('ilk kaydettikten sonra saving true iken ikinci cagri yok sayilir', async () => {
    const w = await mountFaturalar()
    gecerliForm(w)
    let bitti = false
    postAxios.mockImplementation(() => new Promise((r) => setTimeout(() => { bitti = true; r({ data: { id: 1 } }) }, 40)))
    const ilk = w.vm.saveFatura()
    await w.vm.$nextTick()
    // Kaydetme ucu hala beklemede; ikinci tiklama geldi.
    await w.vm.saveFatura()
    await ilk
    await flushPromises()
    expect(postAxios).toHaveBeenCalledTimes(1)
    expect(bitti).toBe(true)
  })

  it('kaydetme bittikten sonra ikinci kayit tekrar yapilabilir', async () => {
    const w = await mountFaturalar()
    gecerliForm(w)
    await w.vm.saveFatura()
    await flushPromises()
    gecerliForm(w)
    await w.vm.saveFatura()
    await flushPromises()
    expect(postAxios).toHaveBeenCalledTimes(2)
  })

  it('dogrulama hatasinda kayit denenmez', async () => {
    const w = await mountFaturalar()
    w.vm.form = { tur: '', cariHesapId: null, kalemler: [] }
    w.vm.editingId = null
    await w.vm.saveFatura()
    await flushPromises()
    expect(postAxios).not.toHaveBeenCalled()
  })
})