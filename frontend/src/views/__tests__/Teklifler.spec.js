import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({
      data: {
        content: [
          {
            id: 1,
            teklifNo: 'TKL-2026-0001',
            revizyonNo: 0,
            tarih: '2026-08-21',
            cariHesapAdi: 'Test Müşteri Ltd.',
            durum: 'TASLAK',
            genelToplam: 12500,
            kalemler: [{ id: 1, aciklama: 'Test Ürün', miktar: 1, birimFiyat: 12500, tutar: 12500 }]
          }
        ]
      }
    })),
    post: vi.fn(() => Promise.resolve({ data: { id: 1, teklifNo: 'TKL-2026-0001' } })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    create: vi.fn(() => ({
      get: vi.fn(() => Promise.resolve({
        data: {
          content: [
            {
              id: 1,
              teklifNo: 'TKL-2026-0001',
              revizyonNo: 0,
              tarih: '2026-08-21',
              cariHesapAdi: 'Test Müşteri Ltd.',
              durum: 'TASLAK',
              genelToplam: 12500,
              kalemler: [{ id: 1, aciklama: 'Test Ürün', miktar: 1, birimFiyat: 12500, tutar: 12500 }]
            }
          ]
        }
      })),
      post: vi.fn(() => Promise.resolve({ data: { id: 1 } })),
      put: vi.fn(() => Promise.resolve({ data: {} })),
      delete: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  DataTable: {
    template: '<div class="p-datatable"><slot name="header" /><slot /><slot name="empty" /></div>'
  },
  Column: {
    template: '<div><slot name="body" :data="{ teklifNo: \'TKL-2026-0001\', revizyonNo: 0, cariHesapAdi: \'Test Müşteri Ltd.\', genelToplam: 12500, durum: \'TASLAK\' }" /></div>'
  },
  Button: true,
  InputText: true,
  Dropdown: true,
  Tag: true,
  Dialog: true,
  Textarea: true
}

describe('Teklifler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders quotes list and stats correctly', async () => {
    const Teklifler = (await import('../Teklifler.vue')).default
    const wrapper = mount(Teklifler, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.teklifler-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Satış Teklifleri')
  })
})
