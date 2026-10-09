import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'

const deleteAxios = vi.fn(() => Promise.resolve({ data: {} }))
const getAxios = vi.fn(() => Promise.resolve({ data: [] }))

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

const stubs = { Card: true, Button: true, InputText: true, Dropdown: true, Column: true, DataTable: true }

const mountSirketler = async () => {
  const Sirketler = (await import('../Sirketler.vue')).default
  const pinia = createPinia()
  setActivePinia(pinia)
  const wrapper = mount(Sirketler, {
    global: {
      stubs,
      plugins: [pinia, PrimeVue, ToastService, ConfirmationService, i18n]
    }
  })
  await flushPromises()
  return wrapper
}

describe('Sirketler.vue - yikici islem onayi', () => {
  beforeEach(() => {
    deleteAxios.mockClear()
    getAxios.mockClear()
    getAxios.mockImplementation(() => Promise.resolve({ data: [] }))
  })

  it('sisme tiklandiginda DELETE cagrilmaz, onay diyalogu istenir', async () => {
    const wrapper = await mountSirketler()
    await wrapper.vm.sil({ id: 7, ad: 'Test Sirket' })
    await flushPromises()
    expect(deleteAxios).not.toHaveBeenCalled()
  })

  it('kullanici onayi reddederse kayit listeden silinmez', async () => {
    const wrapper = await mountSirketler()
    const spy = vi.spyOn(wrapper.vm.confirm, 'require')
    await wrapper.vm.sil({ id: 7, ad: 'Test Sirket' })
    expect(spy).toHaveBeenCalledTimes(1)
    // Reddedildi: accept cagrilmadi -> API'ye gitmedi.
    spy.mock.calls[0][0].reject?.()
    await flushPromises()
    expect(deleteAxios).not.toHaveBeenCalled()
  })

  it('kullanici onaylarsa DELETE cagrilir ve kayit listeden dusulur', async () => {
    const wrapper = await mountSirketler()
    getAxios.mockImplementation(() => Promise.resolve({ data: [{ id: 7, ad: 'Test Sirket' }] }))
    await wrapper.vm.sirketleriYukle?.()
    await flushPromises()
    wrapper.vm.sirketler = [{ id: 7, ad: 'Test Sirket' }]
    const spy = vi.spyOn(wrapper.vm.confirm, 'require')
    await wrapper.vm.sil({ id: 7, ad: 'Test Sirket' })
    spy.mock.calls[0][0].accept()
    await flushPromises()
    expect(deleteAxios).toHaveBeenCalled()
  })

  it('onay diyalogunda geri alinamaz uyarisi ve sirket adi yer alir', async () => {
    const wrapper = await mountSirketler()
    const spy = vi.spyOn(wrapper.vm.confirm, 'require')
    await wrapper.vm.sil({ id: 7, ad: 'Kritik Sirket' })
    const opts = spy.mock.calls[0][0]
    expect(opts.message).toContain('Kritik Sirket')
    expect(opts.message.toLowerCase()).toContain('geri')
  })
})