import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: [] })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    create: vi.fn(() => ({
      get: vi.fn(() => Promise.resolve({ data: [] })),
      post: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Button: true,
  InputText: true,
  SelectButton: true
}

describe('Sohbet.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders chat page and AI Assistant header', async () => {
    const Sohbet = (await import('../Sohbet.vue')).default
    const wrapper = mount(Sohbet, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.sohbet-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Yapay Zeka (AI) Asistanı')
  })

  it('renders quick question chips in AI mode', async () => {
    const Sohbet = (await import('../Sohbet.vue')).default
    const wrapper = mount(Sohbet, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.findAll('.oneri-cip').length).toBeGreaterThan(0)
  })
})
