import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createPinia, setActivePinia } from 'pinia'
import i18n from '../../i18n.js'
import { useTheme } from '../../composables/useTheme.js'

const {
  girisMock,
  girisSirketMock,
  giris2faMock,
  sifirlaMock,
  kurulumDurumMock,
  durumMock,
  pushMock
} = vi.hoisted(() => ({
  girisMock: vi.fn(),
  girisSirketMock: vi.fn(),
  giris2faMock: vi.fn(),
  sifirlaMock: vi.fn(),
  kurulumDurumMock: vi.fn(),
  durumMock: vi.fn(),
  pushMock: vi.fn()
}))

vi.mock('../../api/index.js', () => ({
  kullaniciAPI: {
    giris: (...args) => girisMock(...args),
    girisSirket: (...args) => girisSirketMock(...args),
    giris2fa: (...args) => giris2faMock(...args),
    sifreSifirlamaTalebi: (...args) => sifirlaMock(...args),
    cikis: vi.fn().mockResolvedValue({ data: {} }),
    ben: vi.fn().mockResolvedValue({ data: {} })
  },
  apiClient: {
    get: vi.fn().mockResolvedValue({ data: [] }),
    post: vi.fn().mockResolvedValue({ data: {} })
  },
  sirketAPI: {
    getById: vi.fn().mockResolvedValue({ data: {} })
  },
  kurulumAPI: {
    durum: (...args) => kurulumDurumMock(...args),
    baslat: vi.fn().mockResolvedValue({ data: {} })
  },
  durumAPI: {
    durum: (...args) => durumMock(...args)
  },
  passkeyAPI: {
    girisSecenekleri: vi.fn().mockResolvedValue({ data: {} }),
    giris: vi.fn().mockResolvedValue({ data: {} })
  },
  qrOturumAPI: {
    baslat: vi.fn().mockResolvedValue({ data: {} }),
    durum: vi.fn().mockResolvedValue({ data: {} }),
    tamamla: vi.fn().mockResolvedValue({ data: {} })
  }
}))

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: pushMock }),
  useRoute: () => ({ query: {} })
}))

import Giris from '../Giris.vue'

let wrapper = null

const mountGiris = async () => {
  const pinia = createPinia()
  setActivePinia(pinia)
  wrapper = mount(Giris, {
    global: {
      plugins: [pinia, i18n],
      stubs: { KurulumAdimi: true }
    }
  })
  await flushPromises()
  return wrapper
}

const acilisiAtla = async (w) => {
  const atla = w.find('[data-test="giris-acilis-atla"]')
  if (atla.exists()) await atla.trigger('click')
  await flushPromises()
}

const doldurVeGonder = async (w, { username = 'ali', password = 'gizli123' } = {}) => {
  await w.get('[data-test="giris-username"]').setValue(username)
  await w.get('[data-test="giris-password"]').setValue(password)
  await w.get('[data-test="giris-form"]').trigger('submit')
  await flushPromises()
}

const oturumYaniti = (sirketId = 1) => ({
  data: {
    id: 10,
    username: 'ali',
    displayName: 'Ali Veli',
    companyName: 'Test Şirket',
    sirketId,
    sirketAdi: 'Test Şirket',
    role: 'ADMIN'
  }
})

describe('Giris.vue', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    useTheme().applyMode('dark')
    i18n.global.locale.value = 'tr'
    document.documentElement.setAttribute('lang', 'tr')
    pushMock.mockReset()
    girisMock.mockReset()
    girisSirketMock.mockReset()
    giris2faMock.mockReset()
    sifirlaMock.mockReset()
    kurulumDurumMock.mockReset()
    durumMock.mockReset()

    kurulumDurumMock.mockResolvedValue({ data: { kurulumGerekli: false } })
    durumMock.mockResolvedValue({
      data: { durum: 'UP', surum: '1.36.3', zaman: '2026-11-05T10:00:00Z' }
    })
    sifirlaMock.mockResolvedValue({ data: {} })
    // Dashboard chunk'ının test sırasında önden yüklenmesini engelle.
    window.requestIdleCallback = vi.fn()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
  })

  it('açılış ekranı atlanabilir ve form görünür olur', async () => {
    const w = await mountGiris()
    expect(w.find('[data-test="giris-acilis"]').exists()).toBe(true)

    await acilisiAtla(w)

    expect(w.find('[data-test="giris-acilis"]').exists()).toBe(false)
    expect(w.find('[data-test="giris-form"]').exists()).toBe(true)
    expect(w.find('[data-test="giris-username"]').exists()).toBe(true)
    expect(w.find('[data-test="giris-password"]').exists()).toBe(true)
    expect(w.find('[data-test="giris-buton"]').exists()).toBe(true)
  })

  it('şirket kodu girildiğinde giriş gövdesine eklenir', async () => {
    girisMock.mockResolvedValue({
      data: { girisToken: 't1', sirketler: [{ id: 1, ad: 'Test Şirket' }] }
    })
    girisSirketMock.mockResolvedValue(oturumYaniti(1))

    const w = await mountGiris()
    await acilisiAtla(w)

    await w.get('[data-test="sirket-kodu-ac"]').trigger('click')
    await w.get('[data-test="sirket-kodu"]').setValue('abc-holding')
    await doldurVeGonder(w)

    expect(girisMock).toHaveBeenCalledWith(
      expect.objectContaining({
        username: 'ali',
        sirketKodu: 'abc-holding'
      })
    )
  })

  it('tek şirket dönerse şirket seçimi atlanır ve yönlendirilir', async () => {
    girisMock.mockResolvedValue({
      data: { girisToken: 't1', sirketler: [{ id: 7, ad: 'Tek Şirket' }] }
    })
    girisSirketMock.mockResolvedValue(oturumYaniti(7))

    const w = await mountGiris()
    await acilisiAtla(w)
    await doldurVeGonder(w)

    expect(girisSirketMock).toHaveBeenCalledWith(
      expect.objectContaining({ girisToken: 't1', sirketId: 7 })
    )
    expect(w.find('[data-test="sirket-secim"]').exists()).toBe(false)
    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('SW yenilemesinde açılış animasyonunu tekrar göstermez', async () => {
    sessionStorage.setItem('raspel_sw_yenileme', '1')
    const w = await mountGiris()

    expect(w.find('[data-test="giris-acilis"]').exists()).toBe(false)
    expect(sessionStorage.getItem('raspel_sw_yenileme')).toBeNull()
  })

  it('2FA gerektiğinde kod adımı gösterilir ve doğrulanır', async () => {
    girisMock.mockResolvedValue({
      data: { twoFactorGerekli: true, girisToken: 'tfa' }
    })
    giris2faMock.mockResolvedValue({
      data: { girisToken: 'tfa2', sirketler: [{ id: 1, ad: 'Test Şirket' }] }
    })
    girisSirketMock.mockResolvedValue(oturumYaniti(1))

    const w = await mountGiris()
    await acilisiAtla(w)
    await doldurVeGonder(w)

    const kod = w.get('[data-test="iki-faktor-kod"]')
    expect(kod.exists()).toBe(true)
    await kod.setValue('123456')
    await w.get('[data-test="iki-faktor-form"]').trigger('submit')
    await flushPromises()

    expect(giris2faMock).toHaveBeenCalledWith(
      expect.objectContaining({ girisToken: 'tfa', code: '123456' })
    )
    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('çok şirket dönerse şirket seçimi gösterilir ve seçilen şirketle giriş yapılır', async () => {
    girisMock.mockResolvedValue({
      data: {
        girisToken: 't2',
        sirketler: [
          { id: 1, ad: 'Birinci Şirket' },
          { id: 2, ad: 'İkinci Şirket' }
        ]
      }
    })
    girisSirketMock.mockResolvedValue(oturumYaniti(2))

    const w = await mountGiris()
    await acilisiAtla(w)
    await doldurVeGonder(w)

    const secim = w.get('[data-test="sirket-secim"]')
    expect(secim.exists()).toBe(true)
    const kartlar = secim.findAll('.sirket-kart')
    expect(kartlar).toHaveLength(2)

    await kartlar[1].trigger('click')
    await flushPromises()

    expect(girisSirketMock).toHaveBeenCalledWith(
      expect.objectContaining({ girisToken: 't2', sirketId: 2 })
    )
    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('sunucu hatası role=alert ile gösterilir', async () => {
    girisMock.mockRejectedValue({
      response: { data: { message: 'Kullanıcı adı veya şifre hatalı' } }
    })

    const w = await mountGiris()
    await acilisiAtla(w)
    await doldurVeGonder(w)

    const uyari = w.get('[role="alert"]')
    expect(uyari.text()).toContain('Kullanıcı adı veya şifre hatalı')
  })

  it('açık/koyu tema seçici temayı değiştirir ve tercihi saklar', async () => {
    const w = await mountGiris()
    await acilisiAtla(w)

    const acik = w.get('[data-test="giris-tema-light"]')
    const koyu = w.get('[data-test="giris-tema-dark"]')
    expect(koyu.attributes('aria-pressed')).toBe('true')

    await acik.trigger('click')
    expect(document.documentElement.getAttribute('data-theme')).toBe('light')
    expect(localStorage.getItem('raspel_erp_theme')).toBe('light')
    expect(acik.attributes('aria-pressed')).toBe('true')
    expect(koyu.attributes('aria-pressed')).toBe('false')

    await koyu.trigger('click')
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark')
    expect(localStorage.getItem('raspel_erp_theme')).toBe('dark')
  })

  it('dil değiştirici TR/EN çalışır', async () => {
    const w = await mountGiris()
    await acilisiAtla(w)

    await w.get('[data-test="giris-dil-en"]').trigger('click')

    await vi.waitFor(() => {
      expect(i18n.global.locale.value).toBe('en')
    })
    await nextTick()
    expect(document.documentElement.getAttribute('lang')).toBe('en')
    expect(w.text()).toContain('Welcome back')
  })

  it('alt bilgide durum, destek ve KVKK bağlantıları bulunur', async () => {
    const w = await mountGiris()
    await acilisiAtla(w)
    await flushPromises()

    const durum = w.get('[data-test="giris-durum"]')
    expect(durum.attributes('href')).toBe('/durum')
    expect(durum.classes()).toContain('gab-durum--up')
    expect(durum.text()).toContain('çalışıyor')

    expect(w.get('[data-test="giris-destek"]').attributes('href')).toBe('mailto:destek@raspel.com.tr')
    expect(w.get('[data-test="giris-kvkk"]').attributes('href')).toBe('/kullanim-sartlari')
    expect(w.get('[data-test="giris-dil"]').exists()).toBe(true)
  })

  it('durum servisi ulaşılamazsa durum bağlantısı sessizce gizlenir', async () => {
    durumMock.mockRejectedValue(new Error('ağ hatası'))

    const w = await mountGiris()
    await acilisiAtla(w)
    await flushPromises()

    expect(w.find('[data-test="giris-durum"]').exists()).toBe(false)
  })
})
