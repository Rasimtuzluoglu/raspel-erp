/* eslint-disable vue/one-component-per-file */
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { defineComponent, h, nextTick } from 'vue'
import ErrorBoundary from '../ErrorBoundary.vue'
import i18n from '../../i18n.js'

const push = vi.fn()
vi.mock('vue-router', () => ({
  useRouter: () => ({ push })
}))

const Kontrol = defineComponent({
  name: 'Kontrol',
  props: { patlat: { type: Boolean, default: false } },
  setup(props) {
    return () => {
      if (props.patlat) throw new Error('Boom')
      return h('div', { class: 'saglikli' }, 'OK')
    }
  }
})

const Host = defineComponent({
  name: 'Host',
  components: { ErrorBoundary, Kontrol },
  data() {
    return { patlat: false }
  },
  template: `<ErrorBoundary><Kontrol :patlat="patlat" /></ErrorBoundary>`
})

describe('ErrorBoundary', () => {
  let consoleError
  beforeEach(() => {
    push.mockClear()
    consoleError = vi.spyOn(console, 'error').mockImplementation(() => {})
  })
  afterEach(() => {
    consoleError.mockRestore()
  })

  it('hata yoksa slot icerigini render eder', () => {
    const wrapper = mount(Host, { global: { plugins: [i18n] } })
    expect(wrapper.find('.saglikli').exists()).toBe(true)
    expect(wrapper.find('.error-boundary').exists()).toBe(false)
  })

  it('alt bilesendeki hatayi yakalar ve hata kartini gosterir', async () => {
    const wrapper = mount(Host, { global: { plugins: [i18n] } })

    wrapper.vm.patlat = true
    await nextTick()
    await nextTick()

    expect(wrapper.find('.error-boundary').exists()).toBe(true)
    expect(wrapper.find('.error-message').exists()).toBe(true)
    expect(consoleError).toHaveBeenCalled()
  })

  it('tekrar dene hata durumunu sifirlar', async () => {
    const wrapper = mount(Host, { global: { plugins: [i18n] } })

    wrapper.vm.patlat = true
    await nextTick()
    await nextTick()
    expect(wrapper.find('.error-boundary').exists()).toBe(true)

    wrapper.vm.patlat = false
    await nextTick()
    await wrapper.find('.retry-btn').trigger('click')
    await nextTick()

    expect(wrapper.find('.error-boundary').exists()).toBe(false)
    expect(wrapper.find('.saglikli').exists()).toBe(true)
  })

  it('ana sayfa butonu / rotasina yonlendirir', async () => {
    const wrapper = mount(Host, { global: { plugins: [i18n] } })

    wrapper.vm.patlat = true
    await nextTick()
    await nextTick()
    expect(wrapper.find('.error-boundary').exists()).toBe(true)

    await wrapper.find('.home-btn').trigger('click')

    expect(push).toHaveBeenCalledWith('/')
  })
})
