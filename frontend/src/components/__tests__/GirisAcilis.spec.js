import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import GirisAcilis from '../GirisAcilis.vue'

const mountAcilis = (props = {}) =>
  mount(GirisAcilis, {
    props,
    global: { plugins: [i18n] }
  })

describe('GirisAcilis', () => {
  beforeEach(() => {
    i18n.global.locale.value = 'tr'
    vi.useFakeTimers()
  })

  afterEach(() => {
    vi.useRealTimers()
    document.body.innerHTML = ''
  })

  it('varsayılan olarak 1200 ms sonra biter', () => {
    const wrapper = mountAcilis()
    vi.advanceTimersByTime(1199)
    expect(wrapper.emitted('bitti')).toBeUndefined()

    vi.advanceTimersByTime(1)
    expect(wrapper.emitted('bitti')).toHaveLength(1)
    wrapper.unmount()
  })

  it('Atla düğmesi klavye erişimli bir butondur ve hemen bitirir', async () => {
    const wrapper = mountAcilis()
    const atla = wrapper.get('[data-test="giris-acilis-atla"]')

    expect(atla.element.tagName).toBe('BUTTON')
    expect(atla.attributes('type')).toBe('button')

    await atla.trigger('click')
    expect(wrapper.emitted('bitti')).toHaveLength(1)
    wrapper.unmount()
  })

  it('prefers-reduced-motion açıkken 250 ms kullanır', () => {
    const eski = window.matchMedia
    window.matchMedia = vi.fn((sorgu) => ({
      matches: sorgu.includes('prefers-reduced-motion'),
      media: sorgu,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn()
    }))

    const wrapper = mountAcilis()
    vi.advanceTimersByTime(249)
    expect(wrapper.emitted('bitti')).toBeUndefined()

    vi.advanceTimersByTime(1)
    expect(wrapper.emitted('bitti')).toHaveLength(1)

    wrapper.unmount()
    window.matchMedia = eski
  })

  it('bitince boş kullanıcı adı alanına odaklanır', async () => {
    const input = document.createElement('input')
    input.setAttribute('data-test', 'giris-username')
    document.body.appendChild(input)

    const wrapper = mountAcilis()
    vi.advanceTimersByTime(1200)
    await nextTick()

    expect(document.activeElement).toBe(input)
    wrapper.unmount()
  })

  it('kullanıcı adı doluysa odağı çalmaz', async () => {
    const input = document.createElement('input')
    input.setAttribute('data-test', 'giris-username')
    input.value = 'ali'
    document.body.appendChild(input)

    const wrapper = mountAcilis()
    vi.advanceTimersByTime(1200)
    await nextTick()

    expect(document.activeElement).not.toBe(input)
    wrapper.unmount()
  })

  it('role=status ve aria-live=polite ile duyurulur', () => {
    const wrapper = mountAcilis()
    const kok = wrapper.get('[data-test="giris-acilis"]')

    expect(kok.attributes('role')).toBe('status')
    expect(kok.attributes('aria-live')).toBe('polite')
    wrapper.unmount()
  })
})
