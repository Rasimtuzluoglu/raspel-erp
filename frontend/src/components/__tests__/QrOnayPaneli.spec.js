import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import QrOnayPaneli from '../QrOnayPaneli.vue'

const { qrMock } = vi.hoisted(() => ({
  qrMock: {
    baslat: vi.fn(),
    durum: vi.fn(),
    onayla: vi.fn(),
    reddet: vi.fn(),
    tamamla: vi.fn(),
    gorselUrl: vi.fn(() => '/api/kullanicilar/qr-oturum/t1/gorsel')
  }
}))

vi.mock('../../api/index.js', () => ({ qrOturumAPI: qrMock }))

const mountEt = () =>
  mount(QrOnayPaneli, {
    props: { girisToken: 'giris-1', rememberMe: false },
    global: { plugins: [i18n] }
  })

describe('QrOnayPaneli', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    qrMock.baslat.mockResolvedValue({ data: { token: 't1', expiresIn: 120 } })
    qrMock.durum.mockResolvedValue({ data: { durum: 'BEKLIYOR' } })
    qrMock.tamamla.mockResolvedValue({ data: { girisToken: 'giris-2', sirketler: [] } })
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.clearAllMocks()
  })

  it('QR görselini ve bekliyor durumunu gösterir', async () => {
    const w = mountEt()
    await flushPromises()

    expect(qrMock.baslat).toHaveBeenCalledWith('giris-1')
    expect(w.find('[data-test="qr-gorsel"]').attributes('src')).toContain('/gorsel')
    expect(w.find('[data-test="qr-durum"]').text()).toContain(i18n.global.t('qrOnay.bekliyor'))
    w.unmount()
  })

  it('onaylandığında girişi tamamlar ve olay yayar', async () => {
    qrMock.durum.mockResolvedValue({ data: { durum: 'ONAYLANDI' } })
    const w = mountEt()
    await flushPromises()

    await vi.advanceTimersByTimeAsync(2100)
    await flushPromises()

    expect(qrMock.tamamla).toHaveBeenCalledWith('giris-1', false)
    expect(w.emitted('basarili')[0][0]).toMatchObject({ girisToken: 'giris-2' })
    w.unmount()
  })

  it('reddedildiğinde hata gösterir', async () => {
    qrMock.durum.mockResolvedValue({ data: { durum: 'REDDEDILDI' } })
    const w = mountEt()
    await flushPromises()

    await vi.advanceTimersByTimeAsync(2100)
    await flushPromises()

    expect(w.find('[data-test="qr-hata"]').text()).toContain(i18n.global.t('qrOnay.reddedildi'))
    w.unmount()
  })

  it('iptal edince kapatma olayı yayar', async () => {
    const w = mountEt()
    await flushPromises()

    await w.find('[data-test="qr-iptal"]').trigger('click')
    expect(w.emitted('kapat')).toHaveLength(1)
    w.unmount()
  })
})
