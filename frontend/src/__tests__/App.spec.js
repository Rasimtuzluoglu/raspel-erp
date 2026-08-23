import { describe, it, expect, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

describe('App.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.removeItem('raspel_erp_auth')
  })

  const commonStubs = {
    'router-link': true,
    'router-view': true,
    Button: true,
    Toast: true,
    ConfirmDialog: true,
    Dialog: true,
    InputText: true,
    Tag: true,
    QuickSearch: true,
    PasswordChangeModal: true,
    HesapMakinesi: true,
    DovizCevirici: true,
    KdvHesaplayici: true,
    TaksitHesaplayici: true,
    KarMarjiHesaplayici: true,
    IbanDogrulayici: true,
    TcKimlikDogrulayici: true
  }

  it('can be mounted without errors', async () => {
    const App = (await import('../App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: commonStubs,
        plugins: [createPinia(), ToastService]
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.app-container').exists()).toBe(true)
    await flushPromises()
  })

  it('renders login page without sidebar when not logged in', async () => {
    const App = (await import('../App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: commonStubs,
        plugins: [createPinia(), ToastService]
      }
    })
    expect(wrapper.find('.sidebar').exists()).toBe(false)
    expect(wrapper.find('.giris-sayfasi').exists()).toBe(true)
    await flushPromises()
  })
})
