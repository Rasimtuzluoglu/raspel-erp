import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'

describe('App.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('can be mounted without errors', async () => {
    const App = (await import('../App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: {
          'router-link': true,
          'router-view': true,
          'Button': true,
          'Toast': true,
          'ConfirmDialog': true,
          'Dialog': true,
          'InputText': true,
          'Tag': true,
          'QuickSearch': true,
          'PasswordChangeModal': true
        },
        plugins: [createPinia(), ToastService]
      }
    })
    expect(wrapper.exists()).toBe(true)
    expect(wrapper.find('.app-container').exists()).toBe(true)
  })

  it('renders login page without sidebar when not logged in', async () => {
    const App = (await import('../App.vue')).default
    const wrapper = mount(App, {
      global: {
        stubs: {
          'router-link': true,
          'router-view': true,
          'Button': true,
          'Toast': true,
          'ConfirmDialog': true,
          'Dialog': true,
          'InputText': true,
          'Tag': true,
          'QuickSearch': true,
          'PasswordChangeModal': true
        },
        plugins: [createPinia(), ToastService]
      }
    })
    expect(wrapper.find('.sidebar').exists()).toBe(false)
    expect(wrapper.find('.giris-sayfasi').exists()).toBe(true)
  })
})
