import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KarKoprusu from '../dashboard/kokpit/KarKoprusu.vue'

const mountKopru = (props = {}) => mount(KarKoprusu, {
  global: { plugins: [i18n] },
  props: {
    ciro: 100000,
    maliyet: 70000,
    brutKar: 30000,
    netKar: 22000,
    paraBirimi: 'TRY',
    ...props
  }
})

describe('KarKoprusu', () => {
  it('ciroden net kara bes adimi gosterir', () => {
    const wrapper = mountKopru()

    expect(wrapper.findAll('[data-test="kar-kopru-satir"]')).toHaveLength(5)
    expect(wrapper.text()).toContain('Ciro')
    expect(wrapper.text()).toContain('Maliyet')
    expect(wrapper.text()).toContain('Brüt kâr')
    expect(wrapper.text()).toContain('Diğer giderler')
    expect(wrapper.text()).toContain('Net kâr')
    expect(wrapper.text()).toContain('100.000,00')
    expect(wrapper.text()).toContain('22.000,00')
  })

  it('netKarYoksaUcAdimGosterir', () => {
    const wrapper = mountKopru({ netKar: null })

    expect(wrapper.findAll('[data-test="kar-kopru-satir"]')).toHaveLength(3)
  })

  it('negatifNetKariKirmiziTasir', () => {
    const wrapper = mountKopru({ ciro: 100000, maliyet: 90000, brutKar: 10000, netKar: -5000 })

    const netSatir = wrapper.findAll('[data-test="kar-kopru-satir"]').at(-1)
    expect(netSatir.find('.kopru-deger').classes()).toContain('negatif')
  })
})
