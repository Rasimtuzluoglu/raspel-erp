import { describe, it, expect, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { useCevrimici } from '../useCevrimici.js'

const mountEt = () =>
  mount({
    setup() {
      const { cevrimici } = useCevrimici()
      return { cevrimici }
    },
    template: '<div>{{ cevrimici }}</div>'
  })

const onLineAyarla = (deger) => {
  Object.defineProperty(navigator, 'onLine', {
    configurable: true,
    get: () => deger
  })
}

describe('useCevrimici', () => {
  afterEach(() => {
    onLineAyarla(true)
  })

  it('başlangıçta navigator.onLine değerini yansıtır', () => {
    onLineAyarla(true)
    const wrapper = mountEt()
    expect(wrapper.vm.cevrimici).toBe(true)
    wrapper.unmount()
  })

  it('offline olayında false, online olayında true olur', async () => {
    onLineAyarla(true)
    const wrapper = mountEt()
    expect(wrapper.vm.cevrimici).toBe(true)

    onLineAyarla(false)
    window.dispatchEvent(new Event('offline'))
    await nextTick()
    expect(wrapper.vm.cevrimici).toBe(false)

    onLineAyarla(true)
    window.dispatchEvent(new Event('online'))
    await nextTick()
    expect(wrapper.vm.cevrimici).toBe(true)
    wrapper.unmount()
  })

  it('bileşen kaldırılınca olay dinleyicileri temizlenir', async () => {
    const wrapper = mountEt()
    wrapper.unmount()

    onLineAyarla(false)
    window.dispatchEvent(new Event('offline'))
    await nextTick()
    // Kaldırılmış bileşen tepki vermez; hata da fırlatmaz.
    expect(wrapper.vm.cevrimici).toBe(true)
  })
})
