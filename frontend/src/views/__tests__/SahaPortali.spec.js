import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn((url) => {
      if (url.includes('/siparisler')) {
        return Promise.resolve({
          data: {
            content: [
              { id: 1, siparisNo: 'SIP-1001', cariHesapAdi: 'Müşteri Ltd.', durum: 'BEKLIYOR', toplamTutar: 4500 }
            ]
          }
        })
      }
      return Promise.resolve({ data: [] })
    }),
    post: vi.fn(() => Promise.resolve({ data: { id: 1 } })),
    patch: vi.fn(() => Promise.resolve({ data: { id: 1 } })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    create: vi.fn(() => ({
      get: vi.fn((url) => {
        if (url.includes('/siparisler')) {
          return Promise.resolve({
            data: {
              content: [
                { id: 1, siparisNo: 'SIP-1001', cariHesapAdi: 'Müşteri Ltd.', durum: 'BEKLIYOR', toplamTutar: 4500 }
              ]
            }
          })
        }
        return Promise.resolve({ data: [] })
      }),
      post: vi.fn(() => Promise.resolve({ data: { id: 1 } })),
      patch: vi.fn(() => Promise.resolve({ data: { id: 1 } })),
      put: vi.fn(() => Promise.resolve({ data: {} })),
      delete: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Button: true,
  InputText: true,
  Dropdown: true,
  Tag: true,
  Dialog: true,
  Textarea: true,
  TabView: {
    template: '<div class="tabview-stub"><slot /></div>'
  },
  TabPanel: {
    template: '<div class="tabpanel-stub"><slot /><slot name="header" /></div>'
  }
}

describe('SahaPortali.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders field portal and tabs correctly', async () => {
    const SahaPortali = (await import('../SahaPortali.vue')).default
    const wrapper = mount(SahaPortali, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.saha-portali-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Saha & Personel Mobil Portalı')
    expect(wrapper.text()).toContain('Sipariş & Teslimat')
  })
})
