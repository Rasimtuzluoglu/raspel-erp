import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import { useAuthStore } from '../../stores/authStore.js'
import i18n from '../../i18n.js'

const deleteAxios = vi.fn(() => Promise.resolve({ data: {} }))
const getAxios = vi.fn(() => Promise.resolve({ data: {} }))

vi.mock('axios', () => {
  const instance = {
    get: (...a) => getAxios(...a),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: (...a) => deleteAxios(...a),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
  }
  return { default: { ...instance, create: vi.fn(() => instance) } }
})

const stubs = { Card: true, Button: true, InputText: true, Dropdown: true, Tag: true, IlkZiyaretIpuclari: true }

const mountHesap = async () => {
  const HesapAyarlari = (await import('../HesapAyarlari.vue')).default
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.kullanici = { id: 1, username: 'admin', role: 'ADMIN' }
  const wrapper = mount(HesapAyarlari, {
    global: { stubs, plugins: [pinia, PrimeVue, ToastService, ConfirmationService, i18n] }
  })
  await flushPromises()
  return wrapper
}

describe('HesapAyarlari.vue - yikici ayar islemleri onayi', () => {
  beforeEach(() => {
    deleteAxios.mockClear()
    getAxios.mockClear()
    getAxios.mockImplementation(() => Promise.resolve({ data: {} }))
  })

  it('token silmede onay verilmeden API cagrilmaz', async () => {
    const w = await mountHesap()
    await w.vm.tokenSil({ id: 3, ad: 'CI Token' })
    await flushPromises()
    expect(deleteAxios).not.toHaveBeenCalled()
  })

  it('token silme onayinda token adi ve geri alinamaz uyarisi yer alir', async () => {
    const w = await mountHesap()
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.tokenSil({ id: 3, ad: 'CI Token' })
    const msg = spy.mock.calls[0][0].message
    expect(msg).toContain('CI Token')
    expect(msg.toLowerCase()).toContain('geri')
  })

  it('token onay reddedilirse tokenlar listesi korunur', async () => {
    const w = await mountHesap()
    w.vm.tokenlar = [{ id: 3, ad: 'CI Token' }]
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.tokenSil({ id: 3, ad: 'CI Token' })
    spy.mock.calls[0][0].reject?.()
    await flushPromises()
    expect(deleteAxios).not.toHaveBeenCalled()
    expect(w.vm.tokenlar).toHaveLength(1)
  })

  it('AI yapilandirmasini silmeden onay istenir', async () => {
    const w = await mountHesap()
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.aiConfigSil()
    await flushPromises()
    expect(deleteAxios).not.toHaveBeenCalled()
    expect(spy).toHaveBeenCalledTimes(1)
  })

  it('AI yapilandirmasi onaylaninca API cagrilir', async () => {
    const w = await mountHesap()
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.aiConfigSil()
    spy.mock.calls[0][0].accept()
    await flushPromises()
    expect(deleteAxios).toHaveBeenCalled()
  })
})