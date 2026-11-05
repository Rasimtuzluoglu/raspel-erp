import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../i18n.js'
import PwaKurulum from '../components/PwaKurulum.vue'

const IOS_UA =
  'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1'
const ANDROID_UA =
  'Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36'

const userAgentAyarla = (ua) => {
  Object.defineProperty(navigator, 'userAgent', {
    configurable: true,
    get: () => ua
  })
}

const standaloneAyarla = (deger) => {
  window.matchMedia = vi.fn().mockImplementation((sorgu) => ({
    matches: deger && String(sorgu).includes('standalone'),
    media: sorgu,
    onchange: null,
    addListener: vi.fn(),
    removeListener: vi.fn(),
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn()
  }))
}

const mountEt = () =>
  mount(PwaKurulum, {
    global: { plugins: [i18n] }
  })

const kurulumOlayi = (sonuc = 'accepted') => {
  const olay = new Event('beforeinstallprompt')
  olay.prompt = vi.fn().mockResolvedValue(undefined)
  olay.userChoice = Promise.resolve({ outcome: sonuc, platform: 'web' })
  return olay
}

describe('PwaKurulum', () => {
  beforeEach(() => {
    localStorage.clear()
    userAgentAyarla(ANDROID_UA)
    standaloneAyarla(false)
    delete navigator.standalone
    window.dispatchEvent(new Event('online'))
  })

  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
  })

  it('beforeinstallprompt öncesinde görünmez, sonrasında Ekle/Kapat bandı çıkar', async () => {
    const wrapper = mountEt()
    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)

    const olay = kurulumOlayi()
    window.dispatchEvent(olay)
    await flushPromises()

    const bant = wrapper.find('[data-test="pwa-kurulum"]')
    expect(bant.exists()).toBe(true)
    expect(bant.attributes('aria-live')).toBe('polite')
    expect(bant.text()).toContain(i18n.global.t('pwa.kurulumBaslik'))
    expect(wrapper.find('[data-test="pwa-ekle"]').text()).toBe(i18n.global.t('pwa.ekle'))
    expect(wrapper.find('[data-test="pwa-kapat"]').exists()).toBe(true)
    expect(olay.preventDefault).toBeDefined()
    wrapper.unmount()
  })

  it('Ekle prompt() çağırır, userChoice sonucunu bekler ve bandı gizler', async () => {
    const wrapper = mountEt()
    const olay = kurulumOlayi('accepted')
    window.dispatchEvent(olay)
    await flushPromises()

    await wrapper.find('[data-test="pwa-ekle"]').trigger('click')
    await flushPromises()

    expect(olay.prompt).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('userChoice reddedilse de bant gizlenir (olay yeniden kullanılamaz)', async () => {
    const wrapper = mountEt()
    const olay = kurulumOlayi('dismissed')
    window.dispatchEvent(olay)
    await flushPromises()

    await wrapper.find('[data-test="pwa-ekle"]').trigger('click')
    await flushPromises()

    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('Kapat tercihi 30 gün saklanır; yeniden açılışta bant gösterilmez', async () => {
    const wrapper = mountEt()
    window.dispatchEvent(kurulumOlayi())
    await flushPromises()

    await wrapper.find('[data-test="pwa-kapat"]').trigger('click')
    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)

    const kayit = Number(localStorage.getItem('raspel_pwa_kapatma'))
    expect(Number.isFinite(kayit)).toBe(true)
    expect(Date.now() - kayit).toBeLessThan(60 * 1000)
    wrapper.unmount()

    const yeni = mountEt()
    window.dispatchEvent(kurulumOlayi())
    await flushPromises()
    expect(yeni.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    yeni.unmount()
  })

  it('30 günden eski kapatma tercihi yok sayılır', async () => {
    localStorage.setItem('raspel_pwa_kapatma', String(Date.now() - 31 * 24 * 60 * 60 * 1000))
    const wrapper = mountEt()
    window.dispatchEvent(kurulumOlayi())
    await flushPromises()

    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('appinstalled olayında gizlenir ve tercih yazılır', async () => {
    const wrapper = mountEt()
    window.dispatchEvent(kurulumOlayi())
    await flushPromises()
    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(true)

    window.dispatchEvent(new Event('appinstalled'))
    await flushPromises()

    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    expect(localStorage.getItem('raspel_pwa_kapatma')).toBeTruthy()
    wrapper.unmount()
  })

  it('standalone modda hiç gösterilmez', async () => {
    standaloneAyarla(true)
    const wrapper = mountEt()
    window.dispatchEvent(kurulumOlayi())
    await flushPromises()

    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('navigator.standalone (iOS) true ise gösterilmez', async () => {
    userAgentAyarla(IOS_UA)
    Object.defineProperty(navigator, 'standalone', {
      configurable: true,
      get: () => true
    })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('iOS Safari\'de "Paylaş → Ana Ekrana Ekle" yönlendirmesini tek sefer gösterir', async () => {
    userAgentAyarla(IOS_UA)
    const wrapper = mountEt()
    await flushPromises()

    const bant = wrapper.find('[data-test="pwa-kurulum"]')
    expect(bant.exists()).toBe(true)
    expect(bant.text()).toContain(i18n.global.t('pwa.iosYonlendirme'))
    // iOS'ta prompt() yoktur; yalnızca kapatma sunulur.
    expect(wrapper.find('[data-test="pwa-ekle"]').exists()).toBe(false)
    expect(localStorage.getItem('raspel_pwa_ios')).toBeTruthy()
    wrapper.unmount()

    const yeni = mountEt()
    await flushPromises()
    expect(yeni.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    yeni.unmount()
  })

  it('iOS dışı tarayıcıda iOS yönlendirmesi gösterilmez', async () => {
    const wrapper = mountEt()
    await flushPromises()
    expect(wrapper.find('[data-test="pwa-kurulum"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
