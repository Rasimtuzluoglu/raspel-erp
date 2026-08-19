import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import permissionDirective from '../permission.js'
import { useAuthStore } from '../../stores/authStore.js'

describe('v-permission Directive', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('keeps element in DOM if user has permission', () => {
    const authStore = useAuthStore()
    authStore.kullanici = { role: 'ADMIN' }

    const Component = {
      template: `<div id="test-el" v-permission="'fatura:sil'">İçerik</div>`
    }

    const wrapper = mount(Component, {
      global: {
        directives: {
          permission: permissionDirective
        }
      }
    })

    expect(wrapper.find('#test-el').exists()).toBe(true)
  })

  it('removes element from DOM if user lacks permission', () => {
    const authStore = useAuthStore()
    authStore.kullanici = { role: 'USER' }
    authStore.yetkiler = ['fatura:oku']

    const Component = {
      template: `<div><div id="test-el" v-permission="'fatura:sil'">İçerik</div></div>`
    }

    const wrapper = mount(Component, {
      global: {
        directives: {
          permission: permissionDirective
        }
      }
    })

    expect(wrapper.find('#test-el').exists()).toBe(false)
  })
})
