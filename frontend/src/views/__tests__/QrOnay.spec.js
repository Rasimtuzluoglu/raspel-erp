import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import QrOnay from '../QrOnay.vue'

const { qrMock, pushMock } = vi.hoisted(() => ({
  qrMock: {
    durum: vi.fn(),
    onayla: vi.fn(),
    reddet: vi.fn()
  },
  pushMock: vi.fn()
}))

vi.mock('../../api/index.js', () => ({ qrOturumAPI: qrMock }))
vi.mock('vue-router', () => ({
  useRoute: () => ({ params: { token: 'qr-1' } }),
  useRouter: () => ({ push: pushMock })
}))

const ButtonStub = {
  props: ['label', 'icon', 'loading', 'disabled', 'severity', 'outlined'],
  template: '<button v-bind="$attrs" @click="$emit(\'click\')"><slot />{{ label }}</button>'
}

const mountEt = () =>
  mount(QrOnay, {
    global: {
      plugins: [i18n],
      stubs: { Button: ButtonStub }
    }
  })

describe('QrOnay sayfası', () => {
  beforeEach(() => {
    qrMock.durum.mockResolvedValue({ data: { durum: 'BEKLIYOR' } })
    qrMock.onayla.mockResolvedValue({ data: {} })
    qrMock.reddet.mockResolvedValue({ data: {} })
  })

  it('bekleyen isteği onaylar ve başarı durumunu gösterir', async () => {
    const w = mountEt()
    await flushPromises()

    expect(qrMock.durum).toHaveBeenCalledWith('qr-1')
    expect(w.find('[data-test="qr-onayla"]').exists()).toBe(true)

    await w.find('[data-test="qr-onayla"]').trigger('click')
    await flushPromises()

    expect(qrMock.onayla).toHaveBeenCalledWith('qr-1')
    expect(w.find('[data-test="qr-onay-durum"]').text()).toContain(
      i18n.global.t('qrOnay.onayBasarili')
    )
    w.unmount()
  })

  it('reddeder ve durumu gösterir', async () => {
    const w = mountEt()
    await flushPromises()

    await w.find('[data-test="qr-reddet"]').trigger('click')
    await flushPromises()

    expect(qrMock.reddet).toHaveBeenCalledWith('qr-1')
    expect(w.find('[data-test="qr-onay-durum"]').text()).toContain(
      i18n.global.t('qrOnay.reddedildi')
    )
    w.unmount()
  })

  it('süresi dolmuş QR için hata gösterir', async () => {
    qrMock.durum.mockResolvedValue({ data: { durum: 'SURESI_DOLDU' } })
    const w = mountEt()
    await flushPromises()

    expect(w.find('[data-test="qr-onay-durum"]').text()).toContain(
      i18n.global.t('qrOnay.suresiDoldu')
    )
    w.unmount()
  })

  it('onay API hatasını gösterir', async () => {
    qrMock.onayla.mockRejectedValue({ response: { data: { message: 'Süre doldu' } } })
    const w = mountEt()
    await flushPromises()

    await w.find('[data-test="qr-onayla"]').trigger('click')
    await flushPromises()

    expect(w.text()).toContain('Süre doldu')
    w.unmount()
  })
})
