import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { useAuthStore } from '../../stores/authStore.js'

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
  SelectButton: true,
  Dropdown: true,
  MultiSelect: true,
  InputSwitch: true,
  Tag: true,
  Dialog: true
}

const mountSohbet = async (rol = 'ADMIN') => {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.kullanici = { id: 1, role: rol, displayName: 'Test Kullanıcı' }
  auth.sirketId = 1
  const Sohbet = (await import('../Sohbet.vue')).default
  const wrapper = mount(Sohbet, {
    global: { stubs, plugins: [pinia, ToastService, ConfirmationService, i18n] }
  })
  await flushPromises()
  return wrapper
}

describe('Sohbet.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders chat page and AI Assistant header', async () => {
    const wrapper = await mountSohbet('ADMIN')
    expect(wrapper.find('.sohbet-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Yapay Zeka (AI) Asistanı')
  })

  it('renders quick question chips in AI mode', async () => {
    const wrapper = await mountSohbet('ADMIN')
    expect(wrapper.findAll('.oneri-cip').length).toBeGreaterThan(0)
  })

  // Faz 8: AI finansal veri okuyabildigi icin DRIVER rolune kapali.
  it('DRIVER rolunde AI sekmesi ve hizli sorular gorunmez', async () => {
    const wrapper = await mountSohbet('DRIVER')
    expect(wrapper.text()).toContain('Ekip Sohbeti')
    expect(wrapper.text()).not.toContain('Yapay Zeka (AI) Asistanı')
    expect(wrapper.findAll('.oneri-cip').length).toBe(0)
  })
})
