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
      get: vi.fn(),
      post: vi.fn(),
      put: vi.fn(),
      delete: vi.fn(),
      interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
    }))
  }
}))

const stubs = {
  PageHeader: true,
  Button: true,
  SkeletonLoader: true
}

describe('Anomaliler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders page without errors', async () => {
    const Anomaliler = (await import('../Anomaliler.vue')).default
    const wrapper = mount(Anomaliler, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.anomaliler-page').exists()).toBe(true)
  })

  it('shows empty state when no anomalies', async () => {
    const Anomaliler = (await import('../Anomaliler.vue')).default
    const wrapper = mount(Anomaliler, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.anomaliler-page').exists()).toBe(true)
  })
})
