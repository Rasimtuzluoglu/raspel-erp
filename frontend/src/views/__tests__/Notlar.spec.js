import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

const notlarMock = [
  {
    id: 1,
    baslik: 'Test Notu',
    icerik: 'Icerik',
    onemDerecesi: 'YUKSEK',
    renk: 'MAVI',
    olusturmaTarihi: '2026-08-02T10:00:00'
  },
  {
    id: 2,
    baslik: 'Ikinci Not',
    icerik: 'Deneme',
    onemDerecesi: 'NORMAL',
    renk: 'YESIL',
    olusturmaTarihi: '2026-08-01T10:00:00'
  }
]

vi.mock('../../stores/notStore.js', () => ({
  useNotStore: () => ({
    notlar: notlarMock,
    loading: false,
    getAllNotlar: vi.fn(),
    addNot: vi.fn(),
    updateNot: vi.fn(),
    deleteNot: vi.fn()
  })
}))

const stubs = {
  PageHeader: true,
  Button: true,
  SkeletonLoader: true,
  Dialog: true,
  InputText: true,
  Textarea: true,
  Dropdown: true,
  FormField: true
}

describe('Notlar.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders notes list from store', async () => {
    const Notlar = (await import('../Notlar.vue')).default
    const wrapper = mount(Notlar, {
      global: { stubs, plugins: [createPinia(), ToastService] }
    })
    await flushPromises()
    expect(wrapper.find('.notlar-page').exists()).toBe(true)
    expect(wrapper.text()).toContain('Test Notu')
    expect(wrapper.text()).toContain('Ikinci Not')
  })
})
