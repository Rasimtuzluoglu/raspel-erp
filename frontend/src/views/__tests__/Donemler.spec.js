import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { donemAPI, sirketAPI } from '../../api/index.js'
import Donemler from '../Donemler.vue'

vi.mock('../../api/index.js', () => ({
  sirketAPI: {
    getAktif: vi.fn(() => Promise.resolve({ data: [{ id: 1, ad: 'Şirket A' }] }))
  },
  donemAPI: {
    getBySirket: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            id: 1,
            ad: '2026 Yılı',
            baslangic: '2026-01-01',
            bitis: '2026-12-31',
            aktif: true,
            kilitli: false
          },
          {
            id: 2,
            ad: '2025 Yılı',
            baslangic: '2025-01-01',
            bitis: '2025-12-31',
            aktif: false,
            kilitli: true
          }
        ]
      })
    ),
    kapanislar: vi.fn(() =>
      Promise.resolve({ data: [{ id: 1, yil: 2025, kapanisTarihi: '2026-01-02T10:00:00', ozet: 'Kapanış' }] })
    )
  },
  excelAPI: {
    donemler: vi.fn(() => Promise.resolve({ data: new Blob([]) })),
    donemKapanislari: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: { props: ['title', 'subtitle', 'icon'], template: '<div class="ph">{{ title }}<slot name="actions" /></div>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>' },
  EmptyState: true,
  SatirEylemleri: { props: ['items'], template: '<div class="eylemler"><span v-for="(i, x) in items" :key="x">{{ i.etiket }}</span></div>' },
  Dropdown: true,
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  Message: { template: '<div><slot /></div>' },
  InputNumber: true,
  Textarea: true,
  InputText: true,
  DatePicker: true,
  InputSwitch: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  DonemDetayDrawer: true,
  SkeletonLoader: true
}

const kur = () =>
  mount(Donemler, {
    global: {
      plugins: [createPinia(), ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('Donemler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('şirketleri, dönemleri ve kapanışları yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(sirketAPI.getAktif).toHaveBeenCalledTimes(1)
    expect(donemAPI.getBySirket).toHaveBeenCalledWith(1)
    expect(donemAPI.kapanislar).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Dönem')
  })

  it('güncel dönemi işaretler', async () => {
    const w = kur()
    await flushPromises()
    w.vm.donemler = [
      { id: 99, ad: 'Güncel Dönem', baslangic: '2000-01-01', bitis: '2099-12-31', aktif: true, kilitli: false }
    ]
    await w.vm.$nextTick()
    expect(w.vm.guncelDonem?.id).toBe(99)
  })

  it('kilitli/kilitli olmayan dönem eylemlerini üretir', async () => {
    const w = kur()
    await flushPromises()
    const acik = { id: 1, aktif: true, kilitli: false }
    const kilitli = { id: 2, aktif: false, kilitli: true }
    const acikEtiketler = w.vm.donemAksiyonlari(acik).map((i) => i.etiket)
    expect(acikEtiketler).toContain('Kilitle')
    const kilitliEtiketler = w.vm.donemAksiyonlari(kilitli).map((i) => i.etiket)
    expect(kilitliEtiketler).toContain('Kilidi Aç')
  })

  it('yıl sonu kapanış önizlemesi etkilenecek dönemleri hesaplar', async () => {
    const w = kur()
    await flushPromises()
    w.vm.kapanisForm.yil = 2026
    await w.vm.$nextTick()
    expect(w.vm.kapanisOnizleme.adet).toBe(1)
    expect(w.vm.kapanisYapilabilir).toBe(true)
  })
})
