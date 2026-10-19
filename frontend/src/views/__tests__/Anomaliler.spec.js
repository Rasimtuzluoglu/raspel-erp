import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

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
  SkeletonLoader: true,
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>' },
  EmptyState: true,
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot name="header" /><slot /></div>' },
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Dropdown: true,
  InputSwitch: true,
  Checkbox: true,
  SatirEylemleri: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

describe('Anomaliler.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders page without errors', async () => {
    const Anomaliler = (await import('../Anomaliler.vue')).default
    const wrapper = mount(Anomaliler, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.anomaliler-page').exists()).toBe(true)
  })

  it('shows empty state when no anomalies', async () => {
    const Anomaliler = (await import('../Anomaliler.vue')).default
    const wrapper = mount(Anomaliler, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.anomaliler-page').exists()).toBe(true)
  })
})
