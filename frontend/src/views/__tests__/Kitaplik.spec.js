import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import { useAuthStore } from '../../stores/authStore.js'
import i18n from '../../i18n.js'

const getFn = vi.fn()
const postFn = vi.fn(() => Promise.resolve({ data: {} }))
const putFn = vi.fn(() => Promise.resolve({ data: {} }))
const deleteFn = vi.fn(() => Promise.resolve({ data: {} }))

vi.mock('axios', () => {
  const instance = {
    get: (...a) => getFn(...a),
    post: (...a) => postFn(...a),
    put: (...a) => putFn(...a),
    delete: (...a) => deleteFn(...a),
    interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
  }
  return { default: { ...instance, create: vi.fn(() => instance) } }
})

const stubs = {
  Card: true, Button: true, InputText: true, Dropdown: true, Column: true, DataTable: true,
  Dialog: true, Tag: true, Textarea: true, Skeleton: true
}

const mountView = async (admin = false) => {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.kullanici = { id: 1, username: admin ? 'admin' : 'user', role: admin ? 'ADMIN' : 'USER' }
  const View = (await import('../Kitaplik.vue')).default
  const w = mount(View, {
    global: { stubs, plugins: [pinia, PrimeVue, ToastService, ConfirmationService, i18n] }
  })
  await flushPromises()
  return w
}

const yerImi = (over = {}) => ({
  id: 1, tur: 'YER_IMI', baslik: 'Netsis', url: 'https://netsis.local',
  kategori: 'SISTEM', kapsam: 'KISISEL', gorunurluk: 'SAHIS', arsiv: false, ...over
})

describe('Kitaplik.vue', () => {
  beforeEach(() => {
    getFn.mockReset()
    postFn.mockClear()
    putFn.mockClear()
    deleteFn.mockClear()
    getFn.mockImplementation(() => Promise.resolve({ data: [yerImi()] }))
  })

  it('liste yuklenir', async () => {
    const w = await mountView()
    expect(w.vm.kayitlar).toHaveLength(1)
    expect(getFn).toHaveBeenCalledWith('/kitaplik', expect.objectContaining({ params: expect.objectContaining({ tur: 'YER_IMI' }) }))
  })

  it('sekme degisince tur parametresi degisir', async () => {
    const w = await mountView()
    getFn.mockClear()
    await w.vm.sekmeDegistir('SNIPPET')
    await flushPromises()
    expect(getFn).toHaveBeenCalledWith('/kitaplik', expect.objectContaining({ params: expect.objectContaining({ tur: 'SNIPPET' }) }))
  })

  it('yer imi icin url zorunlu', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.tur = 'YER_IMI'
    w.vm.form.baslik = 'Test'
    w.vm.form.url = ''
    await w.vm.kaydet()
    expect(postFn).not.toHaveBeenCalled()
  })

  it('snippet icin icerik zorunlu', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.tur = 'SNIPPET'
    w.vm.form.baslik = 'Test'
    w.vm.form.icerik = ''
    await w.vm.kaydet()
    expect(postFn).not.toHaveBeenCalled()
  })

  it('yer imi kaydedince POST govdesi url icerir, icerik null', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.tur = 'YER_IMI'
    w.vm.form.baslik = 'Netsis'
    w.vm.form.url = 'https://netsis.local'
    await w.vm.kaydet()
    await flushPromises()
    const govde = postFn.mock.calls[0][1]
    expect(govde.tur).toBe('YER_IMI')
    expect(govde.url).toBe('https://netsis.local')
    expect(govde.icerik).toBeNull()
  })

  it('cift gonderim engeli: yavas kayitta ikinci cagri yok', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.tur = 'SNIPPET'
    w.vm.form.baslik = 'X'
    w.vm.form.icerik = 'kod'
    let bitti = false
    postFn.mockImplementation(() => new Promise((r) => setTimeout(() => { bitti = true; r({ data: {} }) }, 50)))
    const ilk = w.vm.kaydet()
    await w.vm.$nextTick()
    await w.vm.kaydet()
    await ilk
    await flushPromises()
    expect(postFn).toHaveBeenCalledTimes(1)
    expect(bitti).toBe(true)
  })

  it('guvenliUrl yalniz http/https kabul eder', async () => {
    const w = await mountView()
    expect(w.vm.guvenliUrl('https://x.local')).toBe('https://x.local')
    expect(w.vm.guvenliUrl('http://x.local')).toBe('http://x.local')
    expect(w.vm.guvenliUrl('javascript:alert(1)')).toBeNull()
    expect(w.vm.guvenliUrl('data:text/html,x')).toBeNull()
    expect(w.vm.guvenliUrl('')).toBeNull()
  })

  it('snippetOnizleme uzun icerigi kirpar', async () => {
    const w = await mountView()
    const uzun = 'a'.repeat(200)
    expect(w.vm.snippetOnizleme(uzun).length).toBeLessThanOrEqual(81)
  })

  it('arsivleme onay ister, onaysiz DELETE cagrilmaz', async () => {
    const w = await mountView()
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.arsivleOnay(w.vm.kayitlar[0])
    expect(spy).toHaveBeenCalledTimes(1)
    expect(deleteFn).not.toHaveBeenCalled()
    spy.mock.calls[0][0].accept()
    await flushPromises()
    expect(deleteFn).toHaveBeenCalledWith('/kitaplik/1')
  })

  it('global gorunurluk kisiselde SAHIS olur', async () => {
    const w = await mountView(true)
    w.vm.kapsamSec('GLOBAL')
    w.vm.form.gorunurluk = 'TUMU'
    expect(w.vm.form.gorunurluk).toBe('TUMU')
    w.vm.kapsamSec('KISISEL')
    expect(w.vm.form.gorunurluk).toBe('SAHIS')
  })

  it('yukleme hatasinda hata gosterilir', async () => {
    getFn.mockImplementation(() => Promise.reject({ response: { data: { message: 'Boom' } } }))
    const w = await mountView()
    expect(w.vm.hata).toBe('Boom')
    expect(w.vm.kayitlar).toHaveLength(0)
  })
})