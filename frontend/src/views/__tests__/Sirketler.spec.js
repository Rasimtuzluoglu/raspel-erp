import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'

const postAxios = vi.fn(() => Promise.resolve({ data: {} }))
const getAxios = vi.fn(() => Promise.resolve({ data: [] }))

vi.mock('axios', () => {
  const instance = {
    get: (...args) => getAxios(...args),
    post: (...args) => postAxios(...args),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
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

describe('Sirketler.vue - şirket kodu', () => {
  beforeEach(() => {
    postAxios.mockClear()
    getAxios.mockClear()
    getAxios.mockImplementation(() => Promise.resolve({ data: [] }))
    i18n.global.locale.value = 'tr'
  })

  it('yeni şirket formunda kod alanı boş başlar', async () => {
    const wrapper = await mountSirketler()
    wrapper.vm.dialogAc()
    expect(wrapper.vm.form.kod).toBe('')
  })

  it('düzenlemede mevcut şirket kodu forma taşınır', async () => {
    const wrapper = await mountSirketler()
    wrapper.vm.dialogAc({ id: 3, ad: 'Test Şirket', kod: 'abc-holding' })
    expect(wrapper.vm.form.kod).toBe('abc-holding')
  })

  it('kaydet gövdesinde şirket kodu gönderilir', async () => {
    const wrapper = await mountSirketler()
    wrapper.vm.dialogAc()
    wrapper.vm.form.ad = 'Yeni Şirket'
    wrapper.vm.form.kod = 'yeni-kod'

    await wrapper.vm.kaydetAction()
    await flushPromises()

    expect(postAxios).toHaveBeenCalledWith(
      '/sirketler',
      expect.objectContaining({ kod: 'yeni-kod' })
    )
  })
})
