import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

vi.mock('axios', () => ({
  default: {
    get: vi.fn(() => Promise.resolve({ data: [] })),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    create: vi.fn(() => ({
      get: vi.fn(() => Promise.resolve({ data: [] })),
      post: vi.fn(() => Promise.resolve({ data: {} })),
      put: vi.fn(() => Promise.resolve({ data: {} })),
      delete: vi.fn(() => Promise.resolve({ data: {} })),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  Button: true,
  DataTable: true,
  Column: true,
  Tag: true,
  SelectButton: true,
  IlkZiyaretIpuclari: true,
  'router-link': true
}

describe('KritikStok.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders page without errors', async () => {
    const KritikStok = (await import('../KritikStok.vue')).default
    const wrapper = mount(KritikStok, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.kritik-stok-container').exists()).toBe(true)
  })

  it('renders forecast view by default', async () => {
    const KritikStok = (await import('../KritikStok.vue')).default
    const wrapper = mount(KritikStok, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.text()).toContain('Kritik Stok & Akıllı Talep Tahmini')
  })
})
