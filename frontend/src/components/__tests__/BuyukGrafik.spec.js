import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import BuyukGrafik from '../dashboard/kokpit/BuyukGrafik.vue'

const mountGrafik = (props = {}) => mount(BuyukGrafik, {
  global: { plugins: [i18n] },
  props: {
    etiketler: ['Oca', 'Şub', 'Mar'],
    seriler: [
      { ad: 'Ciro', degerler: [1000, 2000, 1500], renk: 'var(--accent)' },
      { ad: 'Kâr', degerler: [200, 500, 300], renk: 'var(--success)' }
    ],
    yukseklik: 190,
    paraBirimi: 'TRY',
    ariaLabel: 'Test grafik',
    dataTest: 'test-grafik',
    ...props
  }
})

describe('BuyukGrafik', () => {
  it('cizgi serilerini, eksen etiketlerini ve legendi gosterir', () => {
    const wrapper = mountGrafik()

    expect(wrapper.attributes('data-test')).toBe('test-grafik')
    expect(wrapper.attributes('aria-label')).toBe('Test grafik')
    expect(wrapper.find('svg').exists()).toBe(true)
    expect(wrapper.findAll('polyline')).toHaveLength(2)
    expect(wrapper.findAll('.buyuk-grafik-y-etiket')).toHaveLength(5)
    expect(wrapper.findAll('.buyuk-grafik-x span')).toHaveLength(3)
    expect(wrapper.text()).toContain('Ciro')
    expect(wrapper.text()).toContain('Kâr')
  })

  it('bar tipinde her seri icin cubuk cizer ve ekseni sifirdan baslatir', () => {
    const wrapper = mountGrafik({ tip: 'bar' })

    expect(wrapper.findAll('rect[fill="currentColor"]')).toHaveLength(6)
    expect(wrapper.findAll('polyline')).toHaveLength(0)
    const yEtiketleri = wrapper.findAll('.buyuk-grafik-y-etiket').map((el) => el.text())
    expect(yEtiketleri.at(-1)).toBe('0')
  })

  it('x ekseninde ilk ve son etiketi kenarlara hizalar', () => {
    const wrapper = mountGrafik()

    const etiketler = wrapper.findAll('.buyuk-grafik-x span')
    expect(etiketler[0].classes()).toContain('buyuk-grafik-x--ilk')
    expect(etiketler.at(-1).classes()).toContain('buyuk-grafik-x--son')
  })

  it('sifir cizgisi ve hedef bandi cizer', () => {
    const wrapper = mountGrafik({
      tip: 'line',
      seriler: [{ ad: 'Projeksiyon', degerler: [-1000, 0, 2000], renk: 'var(--info)', dolgu: true }],
      hedefBand: { alt: 0, ust: 500 },
      sifirCizgisi: true
    })

    expect(wrapper.find('.buyuk-grafik-bant').exists()).toBe(true)
    expect(wrapper.find('.buyuk-grafik-sifir').exists()).toBe(true)
    expect(wrapper.find('polygon').exists()).toBe(true)
  })

  it('uzerine gelinen noktada tooltip gosterir', async () => {
    const wrapper = mountGrafik()

    const vuruslar = wrapper.findAll('rect[fill="transparent"]')
    await vuruslar[1].trigger('mouseenter')

    const ipucu = wrapper.find('.buyuk-grafik-ipucu')
    expect(ipucu.exists()).toBe(true)
    expect(ipucu.text()).toContain('Şub')
    expect(ipucu.text()).toContain('Ciro')
  })
})
