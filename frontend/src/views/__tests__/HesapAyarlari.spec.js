import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import { useAuthStore } from '../../stores/authStore.js'
import i18n from '../../i18n.js'
import Card from 'primevue/card'

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
  Button: true,
  InputText: true,
  Dropdown: true,
  Tag: true,
  ToggleSwitch: true,
  Message: true,
  IlkZiyaretIpuclari: true
}

describe('HesapAyarlari.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders settings page without errors', async () => {
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.hesap-ayarlari').exists()).toBe(true)
  })

  it('renders AI Settings card and inputs (admin)', async () => {
    // Entegrasyonlar/sistem bölümleri yalnızca ADMIN için görünür.
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.kullanici = { id: 1, username: 'admin', role: 'ADMIN' }
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, plugins: [pinia, ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    // Entegrasyonlar 5. bolum (index 4).
    wrapper.vm.aktifBolum = 4
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.ai-ayar-kart').exists()).toBe(true)
  })

  it('sol dikey menu 8 bolum sunar; secim aktif sekmeyi degistirir', async () => {
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    const ogeler = wrapper.findAll('.ayarlar-nav-oge')
    expect(ogeler.length).toBe(8)

    await ogeler[4].trigger('click')
    await wrapper.vm.$nextTick()
    expect(wrapper.vm.aktifBolum).toBe(4)
  })

  it('bildirim tercihleri tek liste konteynerinde satirlar halinde gosterilir', async () => {
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, components: { Card }, plugins: [createPinia(), ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.aktifBolum = 3
    await flushPromises()
    await wrapper.vm.$nextTick()
    const liste = wrapper.find('.tercih-liste')
    expect(liste.exists()).toBe(true)
    expect(wrapper.findAll('.tercih-satir').length).toBe(10)
  })

  it('token listesi kart konteynerinde render edilir (admin)', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.kullanici = { id: 1, username: 'admin', role: 'ADMIN' }
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, components: { Card }, plugins: [pinia, ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.aktifBolum = 4
    wrapper.vm.tokenlar = [
      { id: 1, ad: 'CI Token', olusturmaTarihi: '2026-01-01T10:00:00' },
      { id: 2, ad: 'Bot', olusturmaTarihi: null }
    ]
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.token-liste').exists()).toBe(true)
    expect(wrapper.findAll('.token-satir').length).toBe(2)
    expect(wrapper.find('.token-bos').exists()).toBe(false)
  })

  it('token yoksa ikonlu bos durum gosterilir (admin)', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.kullanici = { id: 1, username: 'admin', role: 'ADMIN' }
    const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
    const wrapper = mount(HesapAyarlari, {
      global: { stubs, components: { Card }, plugins: [pinia, ToastService, ConfirmationService, i18n] }
    })
    await flushPromises()
    wrapper.vm.aktifBolum = 4
    wrapper.vm.tokenlar = []
    wrapper.vm.yeniToken = ''
    await flushPromises()
    await wrapper.vm.$nextTick()
    expect(wrapper.find('.token-liste').exists()).toBe(true)
    expect(wrapper.find('.token-bos').exists()).toBe(true)
  })
})
