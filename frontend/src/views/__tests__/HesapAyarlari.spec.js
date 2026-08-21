import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: { provider: 'OPENAI', model: 'gpt-4o', durum: 'YAPILANDIRILMADI' } })),
    post: vi.fn(() => Promise.resolve({ data: { status: 'SUCCESS' } })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    create: vi.fn(() => ({
      get: vi.fn(() => Promise.resolve({ data: { provider: 'OPENAI', model: 'gpt-4o', durum: 'YAPILANDIRILMADI' } })),
      post: vi.fn(() => Promise.resolve({ data: { status: 'SUCCESS' } })),
      put: vi.fn(() => Promise.resolve({ data: {} })),
      delete: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Card: true,
  Button: true,
  InputText: true,
  Dropdown: true,
  Tag: true,
  IlkZiyaretIpuclari: true
}

describe('HesapAyarlari.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders settings page without errors', async () => {
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.hesap-ayarlari').exists()).toBe(true)
  })

  it('renders AI Settings card and inputs', async () => {
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.ai-ayar-kart').exists()).toBe(true)
  })
})
