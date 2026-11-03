import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import {
  OTO_YENILEME_ANAHTAR,
  OTO_YENILEME_ARALIK_MS,
  useDashboardOtoYenileme
} from '../useDashboardOtoYenileme.js'

const mountEt = (yenile) =>
  mount({
    setup() {
      const { aktif, ayarla } = useDashboardOtoYenileme(yenile)
      return { aktif, ayarla }
    },
    template: '<div />'
  })

const gorunurluk = (deger) =>
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => deger
  })

describe('useDashboardOtoYenileme', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    delete document.visibilityState
  })

  it('varsayılan olarak kapalıdır ve zamanlayıcı kurmaz', () => {
    const yenile = vi.fn()
    const wrapper = mountEt(yenile)

    expect(wrapper.vm.aktif).toBe(false)
    vi.advanceTimersByTime(OTO_YENILEME_ARALIK_MS * 2)
    expect(yenile).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it('cihazda açık tercih varsa 60 saniyede bir yeniler', () => {
    localStorage.setItem(OTO_YENILEME_ANAHTAR, '1')
    const yenile = vi.fn()
    const wrapper = mountEt(yenile)

    expect(wrapper.vm.aktif).toBe(true)
    expect(OTO_YENILEME_ARALIK_MS).toBe(60000)

    vi.advanceTimersByTime(60000)
    expect(yenile).toHaveBeenCalledTimes(1)

    vi.advanceTimersByTime(60000)
    expect(yenile).toHaveBeenCalledTimes(2)
    wrapper.unmount()
  })

  it('sekme görünür değilken istek üretmez', () => {
    localStorage.setItem(OTO_YENILEME_ANAHTAR, '1')
    gorunurluk('hidden')
    const yenile = vi.fn()
    const wrapper = mountEt(yenile)

    vi.advanceTimersByTime(60000 * 2)
    expect(yenile).not.toHaveBeenCalled()

    gorunurluk('visible')
    vi.advanceTimersByTime(60000)
    expect(yenile).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('anahtar kapatılınca zamanlayıcı durur, açılınca tercih kaydedilir', async () => {
    const yenile = vi.fn()
    const wrapper = mountEt(yenile)

    wrapper.vm.ayarla(true)
    await nextTick()
    expect(wrapper.vm.aktif).toBe(true)
    expect(localStorage.getItem(OTO_YENILEME_ANAHTAR)).toBe('1')

    vi.advanceTimersByTime(60000)
    expect(yenile).toHaveBeenCalledTimes(1)

    wrapper.vm.ayarla(false)
    await nextTick()
    expect(wrapper.vm.aktif).toBe(false)
    expect(localStorage.getItem(OTO_YENILEME_ANAHTAR)).toBe('0')

    vi.advanceTimersByTime(60000 * 3)
    expect(yenile).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('bileşen kaldırılınca zamanlayıcı temizlenir', () => {
    localStorage.setItem(OTO_YENILEME_ANAHTAR, '1')
    const yenile = vi.fn()
    const wrapper = mountEt(yenile)

    vi.advanceTimersByTime(60000)
    expect(yenile).toHaveBeenCalledTimes(1)

    wrapper.unmount()
    vi.advanceTimersByTime(60000 * 3)
    expect(yenile).toHaveBeenCalledTimes(1)
  })
})
