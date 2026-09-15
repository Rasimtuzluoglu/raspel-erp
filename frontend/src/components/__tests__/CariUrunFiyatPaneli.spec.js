import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import CariUrunFiyatPaneli from '../CariUrunFiyatPaneli.vue'
import i18n from '../../i18n.js'

const mountPanel = (props) =>
  mount(CariUrunFiyatPaneli, {
    props,
    global: { plugins: [i18n], stubs: { Button: { template: "<button class='p-button' @click=\"$emit('click')\"><slot /></button>" } } }
  })

describe('CariUrunFiyatPaneli', () => {
  it('son fiyat ve fatura bilgisini gosterir', () => {
    const wrapper = mountPanel({
      fiyatGecmisi: {
        sonFiyat: 120,
        gecmis: [{ birimFiyat: 120, tarih: '2026-01-01', faturaNumarasi: 'F-1' }]
      }
    })
    expect(wrapper.text()).toContain('#F-1')
    expect(wrapper.find('button').exists()).toBe(true)
  })

  it('Uygula tiklandiginda sonFiyat ile uygula eventi yayar', async () => {
    const wrapper = mountPanel({
      fiyatGecmisi: { sonFiyat: 99.5, gecmis: [{ birimFiyat: 99.5, tarih: '2026-02-02', faturaNumarasi: 'F-2' }] }
    })
    await wrapper.find('button').trigger('click')
    expect(wrapper.emitted('uygula')).toBeTruthy()
    expect(wrapper.emitted('uygula')[0][0]).toBe(99.5)
  })

  it('gecmis yoksa bos mesaj gosterir', () => {
    const wrapper = mountPanel({ fiyatGecmisi: { sonFiyat: null, gecmis: [] } })
    expect(wrapper.find('button').exists()).toBe(false)
    expect(wrapper.text().length).toBeGreaterThan(0)
  })
})
