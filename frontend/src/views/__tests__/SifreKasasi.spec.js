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
  Dialog: true, Tag: true, Message: true, Textarea: true, InputNumber: true, Skeleton: true
}

const mountView = async (admin = false) => {
  const pinia = createPinia()
  setActivePinia(pinia)
  const auth = useAuthStore()
  auth.kullanici = { id: 1, username: admin ? 'admin' : 'user', role: admin ? 'ADMIN' : 'USER' }
  const View = (await import('../SifreKasasi.vue')).default
  const w = mount(View, {
    global: { stubs, plugins: [pinia, PrimeVue, ToastService, ConfirmationService, i18n] }
  })
  await flushPromises()
  return w
}

const ornekKayit = (over = {}) => ({
  id: 1, baslik: 'Netsis', kullaniciAdi: 'muhasebe', kapsam: 'KISISEL',
  sifreGorunurlugu: 'SAHIS', kategori: 'SISTEM', durum: 'GECERLI', kalanGun: 40, arsiv: false,
  ...over
})

describe('SifreKasasi.vue', () => {
  beforeEach(() => {
    getFn.mockReset()
    postFn.mockClear()
    putFn.mockClear()
    deleteFn.mockClear()
    getFn.mockImplementation((url) => {
      if (url === '/sifre-kasa') return Promise.resolve({ data: [ornekKayit()] })
      if (url === '/sifre-kasa/ozet') return Promise.resolve({ data: { uyari: 0 } })
      if (String(url).endsWith('/sifre')) return Promise.resolve({ data: { sifre: 'CokGizli123!' } })
      return Promise.resolve({ data: [] })
    })
  })

  it('liste yuklendiginde sifre alani hic yoktur ve duz metin render edilmez', async () => {
    // NOT: DataTable test ortaminda stub'lanir; bu yuzden gizleme
    // davranisi VERI duzeyinde dogrulanir. Liste hicbir zaman sifre
    // alani tasimaz; duz metin render ciktisinda bulunmaz.
    const w = await mountView()
    expect(w.vm.kayitlar).toHaveLength(1)
    expect(w.vm.kayitlar[0].sifre).toBeUndefined()
    expect(w.text()).not.toContain('CokGizli123!')
  })

  it('goz ikonuna basinca sifre acilir', async () => {
    const w = await mountView()
    await w.vm.sifreToggle(w.vm.kayitlar[0])
    await flushPromises()
    expect(w.vm.acilanSifre).toBe('CokGizli123!')
    expect(w.vm.acilanId).toBe(1)
    // sifreAc ucu cagrildi
    expect(getFn).toHaveBeenCalledWith('/sifre-kasa/1/sifre')
  })

  it('acilan sifre tekrar tiklayinca gizlenir', async () => {
    const w = await mountView()
    await w.vm.sifreToggle(w.vm.kayitlar[0])
    await flushPromises()
    await w.vm.sifreToggle(w.vm.kayitlar[0])
    expect(w.vm.acilanSifre).toBe('')
    expect(w.vm.acilanId).toBeNull()
  })

  it('sifreyi kopyala ucu cagirir', async () => {
    const w = await mountView()
    await w.vm.sifreyiKopyala(w.vm.kayitlar[0])
    await flushPromises()
    expect(getFn).toHaveBeenCalledWith('/sifre-kasa/1/sifre')
  })

  it('yeni kayit icin baslik zorunludur', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.baslik = ''
    w.vm.form.sifre = 'X'
    await w.vm.kaydet()
    expect(postFn).not.toHaveBeenCalled()
  })

  it('yeni kayit icin sifre zorunludur', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.baslik = 'Test'
    w.vm.form.sifre = ''
    await w.vm.kaydet()
    expect(postFn).not.toHaveBeenCalled()
  })

  it('gecerli kayit POST eder ve duz metni temizler', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.baslik = 'Netsis'
    w.vm.form.sifre = 'Gizli123'
    await w.vm.kaydet()
    await flushPromises()
    expect(postFn).toHaveBeenCalledTimes(1)
    const govde = postFn.mock.calls[0][1]
    expect(govde.baslik).toBe('Netsis')
    expect(govde.sifre).toBe('Gizli123')
    expect(w.vm.form.sifre).toBe('')
  })

  it('cift gonderim engeli: yavas kayitta ikinci cagri yok sayilir', async () => {
    const w = await mountView()
    w.vm.yeniKayit()
    w.vm.form.baslik = 'Yavas'
    w.vm.form.sifre = 'X'
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

  it('duzenlemede bos sifre gonderilmez (mevcut korunur)', async () => {
    const w = await mountView()
    w.vm.duzenle(w.vm.kayitlar[0])
    w.vm.form.baslik = 'Netsis 2'
    // sifre bos
    await w.vm.kaydet()
    await flushPromises()
    expect(putFn).toHaveBeenCalledTimes(1)
    const govde = putFn.mock.calls[0][1]
    expect(govde.sifre).toBeUndefined()
  })

  it('kapsam degisince liste yeniden yuklenir', async () => {
    const w = await mountView()
    getFn.mockClear()
    await w.vm.kapsamDegistir('GLOBAL')
    await flushPromises()
    expect(getFn).toHaveBeenCalledWith('/sifre-kasa', expect.objectContaining({ params: expect.objectContaining({ kapsam: 'GLOBAL' }) }))
  })

  it('global kapsamda gorunurluk secilebilir, kisiselde SAHIS olur', async () => {
    const w = await mountView(true)
    w.vm.kapsamSec('GLOBAL')
    expect(w.vm.form.kapsam).toBe('GLOBAL')
    w.vm.form.sifreGorunurlugu = 'TUMU'
    expect(w.vm.form.sifreGorunurlugu).toBe('TUMU')
    // Kisisel'e donunce gorunurluk SAHIS'e sifirlanir.
    w.vm.kapsamSec('KISISEL')
    expect(w.vm.form.sifreGorunurlugu).toBe('SAHIS')
  })

  it('arsivleme onay istenir, onaylanmadan DELETE cagrilmaz', async () => {
    const w = await mountView()
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.arsivleOnay(w.vm.kayitlar[0])
    expect(spy).toHaveBeenCalledTimes(1)
    expect(deleteFn).not.toHaveBeenCalled()
    // Onay mesaji basligi icerir ve geri alinabilirlikten soz eder.
    expect(spy.mock.calls[0][0].message).toContain('Netsis')
  })

  it('arsivleme onaylaninca DELETE cagrilir', async () => {
    const w = await mountView()
    const spy = vi.spyOn(w.vm.confirm, 'require')
    await w.vm.arsivleOnay(w.vm.kayitlar[0])
    spy.mock.calls[0][0].accept()
    await flushPromises()
    expect(deleteFn).toHaveBeenCalledWith('/sifre-kasa/1')
  })

  it('sure durumu etiketleri dogru uretilir', async () => {
    const w = await mountView()
    expect(w.vm.sureEtiketi({ durum: 'SURESIZ' })).toBeTruthy()
    expect(w.vm.sureSeverity({ durum: 'SURESI_BITTI' })).toBe('danger')
    expect(w.vm.sureSeverity({ durum: 'SURE_YAKLASTI' })).toBe('warn')
    expect(w.vm.sureSeverity({ durum: 'GECERLI' })).toBe('success')
  })

  it('yukleme hatasinda hata durumu gosterilir', async () => {
    getFn.mockImplementation(() => Promise.reject({ response: { data: { message: 'Boom' } } }))
    const w = await mountView()
    expect(w.vm.hata).toBe('Boom')
    expect(w.vm.kayitlar).toHaveLength(0)
  })

  it('arsivdeki kayit arsivMi ile isaretlenir', async () => {
    getFn.mockImplementation((url) => {
      if (url === '/sifre-kasa') return Promise.resolve({ data: [ornekKayit({ arsiv: true })] })
      return Promise.resolve({ data: {} })
    })
    const w = await mountView()
    expect(w.vm.arsivMi(w.vm.kayitlar[0])).toBe(true)
  })
})