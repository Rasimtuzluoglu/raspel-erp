import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import permissionDirective from '../permission.js'
import { useAuthStore } from '../../stores/authStore.js'

describe('v-permission Directive', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  const bilesen = (kod) => ({
    template: `<div><div id="test-el" v-permission="'${kod}'">İçerik</div></div>`
  })

  const bagla = (b) => mount(b, {
    global: {
      directives: {
        permission: permissionDirective
      }
    }
  })

  it('keeps element in DOM if user has permission', () => {
    const authStore = useAuthStore()
    authStore.kullanici = { role: 'ADMIN' }

    const wrapper = bagla(bilesen('FATURA_DELETE'))

    expect(wrapper.find('#test-el').exists()).toBe(true)
  })

  it('removes element from DOM if user lacks permission', () => {
    const authStore = useAuthStore()
    authStore.kullanici = { role: 'USER' }
    authStore.yetkiler = ['FATURA_READ']
    authStore.yetkiYuklendi = true

    const wrapper = bagla(bilesen('FATURA_DELETE'))

    expect(wrapper.find('#test-el').exists()).toBe(false)
  })

  it('keeps element while permissions are still loading', () => {
    // Yetkiler sunucudan okunana kadar hiçbir şey gizlenmez; aksi halde
    // menü ve butonlar yüklenme sırasında anlık olarak kaybolur.
    const authStore = useAuthStore()
    authStore.kullanici = { role: 'USER' }
    authStore.yetkiler = []
    authStore.yetkiYuklendi = false

    const wrapper = bagla(bilesen('FATURA_DELETE'))

    expect(wrapper.find('#test-el').exists()).toBe(true)
  })

  it('removes element after permissions load without the code', () => {
    const authStore = useAuthStore()
    authStore.kullanici = { role: 'USER' }
    authStore.yetkiler = ['CARI_READ']
    authStore.yetkiYuklendi = true

    const wrapper = bagla(bilesen('STOK_DELETE'))

    expect(wrapper.find('#test-el').exists()).toBe(false)
  })
})
