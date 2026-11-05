import { describe, it, expect, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../i18n.js'

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
        plugins: [createPinia(), ToastService, i18n]
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.app-container').exists()).toBe(true)
    await flushPromises()
  }, 15000)

  it('renders login page without sidebar when not logged in', async () => {
    const App = (await import('../App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: commonStubs,
        plugins: [createPinia(), ToastService, i18n]
      }
    })
    expect(wrapper.find('.sidebar').exists()).toBe(false)
    expect(wrapper.find('.giris-sayfasi').exists()).toBe(true)
    await flushPromises()
  })

  it('çevrimdışıyken ince amber bandı gösterir, online olunca gizler', async () => {
    const onceki = Object.getOwnPropertyDescriptor(navigator, 'onLine')
    window.dispatchEvent(new Event('online'))
    Object.defineProperty(navigator, 'onLine', { configurable: true, get: () => false })

    const App = (await import('../App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: commonStubs,
        plugins: [createPinia(), ToastService, i18n]
      }
    })
    await flushPromises()

    const serit = wrapper.find('[data-test="cevrimdisi-serit"]')
    expect(serit.exists()).toBe(true)
    expect(serit.attributes('aria-live')).toBe('polite')
    expect(serit.text()).toContain(i18n.global.t('app.cevrimdisiSonVeriler'))

    Object.defineProperty(navigator, 'onLine', { configurable: true, get: () => true })
    window.dispatchEvent(new Event('online'))
    await nextTick()

    expect(wrapper.find('[data-test="cevrimdisi-serit"]').exists()).toBe(false)
    wrapper.unmount()
    if (onceki) Object.defineProperty(navigator, 'onLine', onceki)
  })
})
