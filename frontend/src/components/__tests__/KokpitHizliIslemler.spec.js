import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitHizliIslemler from '../dashboard/kokpit/KokpitHizliIslemler.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :data-to="JSON.stringify(to)"><slot /></a>'
}

const mountBar = (props = {}) => mount(KokpitHizliIslemler, {
  global: {
    plugins: [i18n],
    stubs: { 'router-link': RouterLinkStub }
  },
  props: {
    bekleyenOnay: 0,
    onayGorebilir: true,
    ...props
  }
})

describe('KokpitHizliIslemler', () => {
  it('besAksiyonVeOnayRozetiniGosterir', () => {
    const wrapper = mountBar({ bekleyenOnay: 3 })

    expect(wrapper.find('[data-test="hizli-yeni-fatura"]').attributes('data-to')).toContain('/faturalar')
    expect(wrapper.find('[data-test="hizli-masraf"]').attributes('data-to')).toContain('/masraflar')
    expect(wrapper.find('[data-test="hizli-tahsilat"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="hizli-odeme"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="hizli-onaylar"]').text()).toContain('3')
  })

  it('tahsilatOdemeVeOnayOlaylariniYayar', async () => {
    const wrapper = mountBar()

    await wrapper.find('[data-test="hizli-tahsilat"]').trigger('click')
    await wrapper.find('[data-test="hizli-odeme"]').trigger('click')
    await wrapper.find('[data-test="hizli-onaylar"]').trigger('click')

    expect(wrapper.emitted('tahsilat')).toHaveLength(1)
    expect(wrapper.emitted('odeme')).toHaveLength(1)
    expect(wrapper.emitted('onaylar')).toHaveLength(1)
  })

  it('yetkisizRoldeOnayAksiyonunuGizler', () => {
    const wrapper = mountBar({ onayGorebilir: false })

    expect(wrapper.find('[data-test="hizli-onaylar"]').exists()).toBe(false)
  })
})
