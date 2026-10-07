import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import YasalMetinler from '../YasalMetinler.vue'

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: { sekme: 'gizlilik' } })
}))

const stubs = {
  PageHeader: { template: '<div class="ph"><slot name="actions" /><slot /></div>' },
  TabView: { template: '<div class="tv"><slot /></div>' },
  TabPanel: { template: '<div class="tp"><slot /></div>' },
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  InputText: true,
  Button: true
}

describe('YasalMetinler.vue — birleşik yasal sayfa', () => {
  const kur = () => mount(YasalMetinler, { global: { stubs, plugins: [i18n] } })

  it('hem Kullanım Şartları hem Gizlilik Politikası içeriğini barındırır', () => {
    const wrapper = kur()
    expect(wrapper.find('.yasal-page').exists()).toBe(true)
    expect(wrapper.text()).toContain('Kullanım Şartları')
    expect(wrapper.text()).toContain('Gizlilik Politikası')
  })

  it('her bölümde 7 madde (toplam 14) render edilir', () => {
    const wrapper = kur()
    expect(wrapper.findAll('.madde-card').length).toBe(14)
  })

  it('?sekme=gizlilik iken aktif sekme gizlilik olur', () => {
    const wrapper = kur()
    expect(wrapper.vm.aktifSekme).toBe(1)
  })
})
