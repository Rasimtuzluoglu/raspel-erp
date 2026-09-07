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
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  Button: { template: '<button @click="$emit(\'click\')"><slot /></button>' },
  FormField: { template: '<div><slot /></div>' },
  Select: { template: '<div><slot /></div>' },
  InputNumber: { template: '<div><slot /></div>' },
  DatePicker: { template: '<div><slot /></div>' },
  Textarea: { template: '<div><slot /></div>' }
}

describe('TahsilatGirDialog.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders payment method buttons (Nakit/Kart/Taksit)', async () => {
    const TahsilatGirDialog = (await import('../../components/TahsilatGirDialog.vue')).default
    const wrapper = mount(TahsilatGirDialog, {
      props: { visible: true, cariler: [] },
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    const text = wrapper.text()
    expect(text).toContain('Nakit')
    expect(text).toContain('Kart')
    expect(text).toContain('Taksit')
    expect(wrapper.find('.tahsilat-form').exists()).toBe(true)
  })
})
