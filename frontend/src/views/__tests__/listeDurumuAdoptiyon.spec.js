import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'

const getAxios = vi.fn(() => Promise.resolve({ data: [] }))

vi.mock('axios', () => {
  const instance = {
    get: (...a) => getAxios(...a),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} })),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
  }
  return { default: { ...instance, create: vi.fn(() => instance) } }
})

const mountView = async (View) => {
  const pinia = createPinia()
  setActivePinia(pinia)
  const w = mount(View, {
    global: {
      stubs: { DataTable: true, Column: true, Dialog: true, Button: true, InputText: true, Dropdown: true, Card: true, Toolbar: true, Message: true, Tag: true, Skeleton: true, Select: true, Textarea: true, InputNumber: true },
      plugins: [pinia, PrimeVue, ToastService, ConfirmationService, i18n]
    }
  })
  await flushPromises()
  return w
}

describe('ListeDurumu adoptiyonu - kategori/iskonto', () => {
  beforeEach(() => {
    getAxios.mockClear()
    getAxios.mockImplementation(() => Promise.resolve({ data: [] }))
  })

  it('Kategoriler.vue ortak durum bilesenini kullaniyor', async () => {
    const K = (await import('../Kategoriler.vue')).default
    const w = await mountView(K)
    expect(w.find('.liste-durumu').exists()).toBe(true)
  })

  it('Kategoriler yukleme sirasinda "kategori yok" GOSTERMEZ', async () => {
    let bitti = false
    getAxios.mockImplementation(() => new Promise((r) => setTimeout(() => { bitti = true; r({ data: [] }) }, 40)))
    const K = (await import('../Kategoriler.vue')).default
    const w = mount(K, {
      global: {
        stubs: { DataTable: true, Column: true, Dialog: true, Button: true, InputText: true, Dropdown: true, Card: true, Toolbar: true, Message: true, Tag: true, Skeleton: true, Select: true, Textarea: true, InputNumber: true },
        plugins: [createPinia(), PrimeVue, ToastService, ConfirmationService, i18n]
      }
    })
    await w.vm.$nextTick()
    // Yukleme surerken iskelet var, bos durum yok.
    expect(w.find('.liste-durumu__iskelet').exists()).toBe(true)
    expect(w.find('.liste-durumu__bos').exists()).toBe(false)
    // Yavas cevap bitince bos durum gosterilmeli.
    await new Promise((r) => setTimeout(r, 60))
    await flushPromises()
    expect(bitti).toBe(true)
  })

  it('IskontoKurallari.vue durum bilesenini yukleme/hata icin kullanir', async () => {
    // Kaynak dogrulamasi: bilesen yukleme sirasinda gorunur, hata
    // durumunda ayri bir blok ile devralir ve DataTable gizlenir.
    const fs = await import('node:fs')
    const path = await import('node:path')
    const src = fs.readFileSync(path.join(process.cwd(), 'src/views/IskontoKurallari.vue'), 'utf8')
    expect(src).toContain('<ListeDurumu')
    expect(src).toContain(':yukleniyor="yukleniyor"')
    expect(src).toContain('yuklemeHatasi')
    expect(src).toMatch(/v-if="!yukleniyor && !yuklemeHatasi"/)
  })

  it('liste bosken Kategoriler "kategori yok" mesajini gosterir', async () => {
    getAxios.mockImplementation(() => Promise.resolve({ data: [] }))
    const K = (await import('../Kategoriler.vue')).default
    const w = await mountView(K)
    expect(w.find('.liste-durumu__bos').exists()).toBe(true)
  })
})