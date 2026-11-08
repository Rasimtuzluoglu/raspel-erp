import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import i18n from '../../i18n.js'
import GirisFormKarti from '../GirisFormKarti.vue'

const { girisMock, sirketMock, passkeyMock, passkeyIleGirisMock } = vi.hoisted(() => ({
  girisMock: vi.fn(),
  sirketMock: vi.fn(),
  passkeyMock: {
    girisSecenekleri: vi.fn(),
    giris: vi.fn()
  },
  passkeyIleGirisMock: vi.fn()
}))

vi.mock('../../api/index.js', () => ({
  kullaniciAPI: {
    giris: girisMock,
    girisSirket: sirketMock,
    giris2fa: vi.fn(),
    yetkilerim: vi.fn().mockResolvedValue({ data: [] }),
    sifreSifirlamaTalebi: vi.fn()
  },
  kurulumAPI: { durum: vi.fn().mockResolvedValue({ data: {} }) },
  passkeyAPI: passkeyMock
}))

vi.mock('../../utils/webauthn.js', () => ({
  passkeyDestekleniyor: () => true,
  passkeyIleGiris: passkeyIleGirisMock
}))

const mountEt = () =>
  mount(GirisFormKarti, {
    global: {
      plugins: [createPinia(), i18n],
      stubs: { KurulumAdimi: true, QrOnayPaneli: true }
    }
  })

describe('GirisFormKarti — passkey ve QR', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    sirketMock.mockResolvedValue({ data: { token: 'jwt' } })
  })

  it('passkey ile giriş yapar ve tek şirkette otomatik tamamlar', async () => {
    passkeyMock.girisSecenekleri.mockResolvedValue({
      data: { challengeId: 'c1', publicKey: { challenge: 'x' } }
    })
    passkeyIleGirisMock.mockResolvedValue({ id: 'cred' })
    passkeyMock.giris.mockResolvedValue({
      data: { girisToken: 'giris-1', sirketler: [{ id: 5, ad: 'RasPel' }] }
    })

    const w = mountEt()
    await flushPromises()
    await w.find('[data-test="passkey-giris"]').trigger('click')
    await flushPromises()

    expect(passkeyMock.girisSecenekleri).toHaveBeenCalled()
    expect(passkeyIleGirisMock).toHaveBeenCalledWith({ challenge: 'x' })
    expect(passkeyMock.giris).toHaveBeenCalledWith({
      challengeId: 'c1',
      credential: { id: 'cred' },
      sirketKodu: '',
      rememberMe: false
    })
    expect(sirketMock).toHaveBeenCalledWith({
      girisToken: 'giris-1',
      sirketId: 5,
      rememberMe: false
    })
    expect(w.emitted('giris-basarili')).toHaveLength(1)
    w.unmount()
  })

  it('passkey iptalinde hata göstermez', async () => {
    passkeyMock.girisSecenekleri.mockResolvedValue({
      data: { challengeId: 'c1', publicKey: { challenge: 'x' } }
    })
    const iptal = new Error('iptal')
    iptal.name = 'NotAllowedError'
    passkeyIleGirisMock.mockRejectedValue(iptal)

    const w = mountEt()
    await flushPromises()
    await w.find('[data-test="passkey-giris"]').trigger('click')
    await flushPromises()

    expect(w.find('[data-test="giris-hata"]').exists()).toBe(false)
    w.unmount()
  })

  it('2FA adımında QR panelini açar', async () => {
    girisMock.mockResolvedValue({
      data: { twoFactorGerekli: true, girisToken: 'giris-1' }
    })

    const w = mountEt()
    await flushPromises()
    await w.find('[data-test="giris-username"]').setValue('admin')
    await w.find('[data-test="giris-password"]').setValue('pass')
    await w.find('[data-test="giris-form"]').trigger('submit')
    await flushPromises()

    expect(w.find('[data-test="iki-faktor-form"]').exists()).toBe(true)
    await w.find('[data-test="qr-ile-onayla"]').trigger('click')
    await w.findComponent({ name: 'QrOnayPaneli' })
    expect(w.find('[data-test="qr-ile-onayla"]').exists()).toBe(false)
    w.unmount()
  })

  it('şirket kodunu açar, gösterir ve giriş isteğine ekler', async () => {
    girisMock.mockResolvedValue({
      data: { girisToken: 'giris-1', sirketler: [{ id: 5, ad: 'RasPel' }] }
    })

    const w = mountEt()
    await flushPromises()
    expect(w.find('[data-test="sirket-kodu"]').exists()).toBe(false)

    await w.find('[data-test="sirket-kodu-ac"]').trigger('click')
    await flushPromises()
    expect(w.find('[data-test="sirket-kodu"]').exists()).toBe(true)
    expect(w.find('[data-test="sirket-kodu-ac"]').attributes('aria-expanded')).toBe('true')

    await w.find('[data-test="sirket-kodu"]').setValue('abc-holding')
    expect(w.find('[data-test="sirket-kodu-cip"]').exists()).toBe(false)

    await w.find('[data-test="sirket-kodu-ac"]').trigger('click')
    expect(w.find('[data-test="sirket-kodu-cip"]').text()).toContain('abc-holding')

    await w.find('[data-test="sirket-kodu-ac"]').trigger('click')
    await w.find('[data-test="sirket-kodu-temizle"]').trigger('click')
    expect(w.find('[data-test="sirket-kodu"]').element.value).toBe('')
    await w.find('[data-test="sirket-kodu"]').setValue('abc-holding')

    await w.find('[data-test="giris-username"]').setValue('admin')
    await w.find('[data-test="giris-password"]').setValue('pass')
    await w.find('[data-test="giris-form"]').trigger('submit')
    await flushPromises()

    expect(girisMock).toHaveBeenCalledWith(expect.objectContaining({
      username: 'admin',
      sirketKodu: 'abc-holding'
    }))
    w.unmount()
  })
})
