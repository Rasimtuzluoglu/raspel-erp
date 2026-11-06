import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'

const { durumMock } = vi.hoisted(() => ({ durumMock: vi.fn() }))

vi.mock('../../api/index.js', () => ({
  durumAPI: {
    durum: (...args) => durumMock(...args)
  }
}))

import Durum from '../Durum.vue'

let wrapper = null

const mountDurum = async () => {
  wrapper = mount(Durum, { global: { plugins: [i18n] } })
  await flushPromises()
  return wrapper
}

describe('Durum.vue', () => {
  beforeEach(() => {
    i18n.global.locale.value = 'tr'
    durumMock.mockReset()
  })

  afterEach(() => {
    wrapper?.unmount()
    wrapper = null
  })

  it('UP durumunda tüm sistemlerin çalıştığını, sürümü ve son kontrolü gösterir', async () => {
    durumMock.mockResolvedValue({
      data: { durum: 'UP', surum: '1.36.3', zaman: '2026-11-05T10:00:00Z' }
    })

    const w = await mountDurum()
    const genel = w.get('[data-test="durum-genel"]')

    expect(genel.classes()).toContain('durum-genel--up')
    expect(genel.text()).toContain('Tüm sistemler çalışıyor')
    expect(w.get('[data-test="durum-surum"]').text()).toBe('1.36.3')
    expect(w.get('[data-test="durum-zaman"]').text().length).toBeGreaterThan(0)
    expect(w.find('[data-test="durum-hata"]').exists()).toBe(false)
  })

  it('DOWN durumunda sistem kesintisi gösterir', async () => {
    durumMock.mockResolvedValue({
      data: { durum: 'DOWN', surum: '1.36.3', zaman: '2026-11-05T10:00:00Z' }
    })

    const w = await mountDurum()
    const genel = w.get('[data-test="durum-genel"]')

    expect(genel.classes()).toContain('durum-genel--down')
    expect(genel.text()).toContain('Sistem kesintisi')
  })

  it('servis ulaşılamazsa "ulaşılamıyor" ve açıklama gösterir', async () => {
    durumMock.mockRejectedValue(new Error('ağ hatası'))

    const w = await mountDurum()

    expect(w.get('[data-test="durum-genel"]').text()).toContain('Duruma ulaşılamıyor')
    expect(w.get('[data-test="durum-hata"]').text()).toContain('Şu anda durum bilgisi alınamıyor')
    expect(w.find('[data-test="durum-surum"]').exists()).toBe(false)
  })

  it('yenile düğmesi durumu tekrar sorgular', async () => {
    durumMock.mockResolvedValue({
      data: { durum: 'UP', surum: '1.36.3', zaman: '2026-11-05T10:00:00Z' }
    })

    const w = await mountDurum()
    expect(durumMock).toHaveBeenCalledTimes(1)

    await w.get('[data-test="durum-yenile"]').trigger('click')
    await flushPromises()

    expect(durumMock).toHaveBeenCalledTimes(2)
  })
})
