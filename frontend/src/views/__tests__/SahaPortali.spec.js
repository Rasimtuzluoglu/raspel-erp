import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'

// Axios mock'u: create() her çağrıda AYRI vi.fn() döndürürse apiClient.post
// izlenemez. Bu yüzden tüm katmanlar aynı referansı paylaşır.
const getMock = vi.fn((url) => {
  if (url.includes('/siparisler')) {
    return Promise.resolve({
      data: {
        content: [
          { id: 1, siparisNo: 'SIP-1001', cariHesapAdi: 'Müşteri Ltd.', durum: 'BEKLIYOR', toplamTutar: 4500 }
        ]
      }
    })
  }
  return Promise.resolve({ data: [] })
})
const postMock = vi.fn(() => Promise.resolve({ data: { id: 1 } }))
const patchMock = vi.fn(() => Promise.resolve({ data: { id: 1 } }))
const putMock = vi.fn(() => Promise.resolve({ data: {} }))
const deleteMock = vi.fn(() => Promise.resolve({ data: {} }))

const instance = {
  get: getMock,
  post: postMock,
  patch: patchMock,
  put: putMock,
  delete: deleteMock,
  interceptors: { request: { use: vi.fn() }, response: { use: vi.fn() } }
}

vi.mock('axios', () => ({
  default: {
    get: getMock,
    post: postMock,
    patch: patchMock,
    put: putMock,
    delete: deleteMock,
    create: vi.fn(() => instance)
  }
}))

const stubs = {
  Button: true,
  InputText: true,
  Dropdown: true,
  Tag: true,
  Dialog: true,
  Textarea: true,
  TabView: {
    template: '<div class="tabview-stub"><slot /></div>'
  },
  TabPanel: {
    template: '<div class="tabpanel-stub"><slot /><slot name="header" /></div>'
  }
}

describe('SahaPortali.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    postMock.mockClear()
    postMock.mockImplementation(() => Promise.resolve({ data: { id: 1 } }))
  })

  it('renders field portal and tabs correctly', async () => {
    const SahaPortali = (await import('../SahaPortali.vue')).default
    const wrapper = mount(SahaPortali, {
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await flushPromises()
    expect(wrapper.find('.saha-portali-sayfasi').exists()).toBe(true)
    expect(wrapper.text()).toContain('Saha & Personel Mobil Portalı')
    expect(wrapper.text()).toContain('Sipariş & Teslimat')
  })

  /**
   * Çevrimdışı alınan imzalı teslimat, bağlantı gelince sunucuya gönderilir.
   * Önceden imza sessizce kayboluyordu: müşteri imzalamış, teslim edilmiş
   * görünmüyordu.
   */
  it('kuyruktaki imzali teslimat baglanti gelince gonderilir', async () => {
    postMock.mockClear()
    // 1x1 saydam PNG
    const imzaDataUrl = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=='
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      { tur: 'TESLIMAT', siparisId: 7, teslimAlanAd: 'Ali Veli', teslimNotu: 'Teslim edildi', imzaDataUrl, tarih: '2026-01-01T10:00:00.000Z' }
    ]))

    const SahaPortali = (await import('../SahaPortali.vue')).default
    mount(SahaPortali, { global: { stubs, plugins: [createPinia(), ToastService, i18n] } })
    await flushPromises()

    window.dispatchEvent(new Event('online'))
    await flushPromises()

    const cagri = postMock.mock.calls.find((c) => String(c[0]).includes('/deliveries/siparis/7/teslim'))
    expect(cagri).toBeTruthy()
    expect(cagri[1]).toBeInstanceOf(FormData)
    expect(cagri[1].get('teslimAlanAd')).toBe('Ali Veli')
    // İmza gerçek bir File olarak geri kurulmalı, yoksa sunucu bozuk görsel alır.
    expect(cagri[1].get('file')).toBeInstanceOf(File)
    expect(JSON.parse(localStorage.getItem('raspel_saha_kuyrugu'))).toEqual([])
  })

  /**
   * Kuyruk eski sürümde sadece not tutuyordu ve `tur` alanı yoktu. Eski
   * kayıtlar NOT sayılmalı, yoksa notAPI.create yerine teslim uçlarına
   * gönderilirlerdi.
   */
  it('tur alani olmayan eski kuyruk kaydi not olarak gonderilir', async () => {
    postMock.mockClear()
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      { baslik: 'Saha Ziyareti', icerik: 'Müşteri ziyaret edildi', kategori: 'SAHA_ZIYARET' }
    ]))

    const SahaPortali = (await import('../SahaPortali.vue')).default
    mount(SahaPortali, { global: { stubs, plugins: [createPinia(), ToastService, i18n] } })
    await flushPromises()

    window.dispatchEvent(new Event('online'))
    await flushPromises()

    const cagri = postMock.mock.calls.find((c) => String(c[0]).includes('/notlar'))
    expect(cagri).toBeTruthy()
    expect(cagri[1].baslik).toBe('Saha Ziyareti')
    expect(postMock.mock.calls.some((c) => String(c[0]).includes('/deliveries/'))).toBe(false)
  })

  /** Gönderilemeyen kayıt kaybolmaz ve kullanıcı bilgilendirilir. */
  it('gonderilemeyen teslimat kuyrukta kalir', async () => {
    postMock.mockClear()
    postMock.mockImplementation((url) => String(url).includes('/deliveries/')
      ? Promise.reject(new Error('server error'))
      : Promise.resolve({ data: {} }))
    localStorage.setItem('raspel_saha_kuyrugu', JSON.stringify([
      { tur: 'TESLIMAT', siparisId: 9, teslimAlanAd: 'Veli', teslimNotu: '', imzaDataUrl: 'data:image/png;base64,iVBORw0KGgo=', tarih: '2026-01-01T10:00:00.000Z' }
    ]))

    const SahaPortali = (await import('../SahaPortali.vue')).default
    mount(SahaPortali, { global: { stubs, plugins: [createPinia(), ToastService, i18n] } })
    await flushPromises()

    window.dispatchEvent(new Event('online'))
    await flushPromises()

    const kalan = JSON.parse(localStorage.getItem('raspel_saha_kuyrugu'))
    expect(kalan).toHaveLength(1)
    expect(kalan[0].tur).toBe('TESLIMAT')
    postMock.mockImplementation(() => Promise.resolve({ data: {} }))
  })
})
