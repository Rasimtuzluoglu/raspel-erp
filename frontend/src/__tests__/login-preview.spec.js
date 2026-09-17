import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../i18n.js'
import LoginPreview from '../components/LoginPreview.vue'

describe('LoginPreview', () => {
  it('alti sekme ile render edilir ve sekme degistirilebilir', async () => {
    const wrapper = mount(LoginPreview, { global: { plugins: [i18n] } })

    const sekmeler = wrapper.findAll('.preview-tab')
    expect(sekmeler.length).toBe(6)
    expect(wrapper.find('.preview-tab.aktif').text()).toContain('Panel')

    await sekmeler[1].trigger('click')
    expect(wrapper.find('.preview-tab.aktif').text()).toContain('e-Fatura')
    expect(wrapper.find('.preview-caption').text().length).toBeGreaterThan(0)

    await sekmeler[3].trigger('click')
    expect(wrapper.find('.preview-tab.aktif').text()).toContain('Üretim')

    wrapper.unmount()
  })
})
