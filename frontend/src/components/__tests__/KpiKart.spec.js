import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import KpiKart from '../KpiKart.vue'
import i18n from '../../i18n.js'

vi.mock('vue-chartjs', () => ({
  Line: { props: ['data', 'options'], template: '<div class="chart-line" />' }
}))
vi.mock('chart.js', () => ({
  Chart: { register: vi.fn() },
  CategoryScale: {},
  LinearScale: {},
  PointElement: {},
  LineElement: {},
  Filler: {}
}))

describe('KpiKart', () => {
  beforeEach(() => {
    window.matchMedia = vi.fn().mockReturnValue({ matches: true, media: '' })
  })

  it('başlık ve biçimlenmiş değeri gösterir', async () => {
    const wrapper = mount(KpiKart, {
      props: { baslik: 'Toplam', deger: 1250, paraBirimi: true },
      global: { plugins: [i18n] }
    })
    expect(wrapper.text()).toContain('Toplam')
    expect(wrapper.text()).toContain('1.250,00')
  })

  it('trend pozitifse yükseliş sınıfı uygular', () => {
    const wrapper = mount(KpiKart, {
      props: { baslik: 'X', deger: 10, paraBirimi: false, trend: 12.5 },
      global: { plugins: [i18n] }
    })
    expect(wrapper.find('.kpi-trend').classes()).toContain('yukselis')
    expect(wrapper.text()).toContain('12.5%')
  })

  it('trend negatifse düşüş sınıfı uygular', () => {
    const wrapper = mount(KpiKart, {
      props: { baslik: 'X', deger: 10, paraBirimi: false, trend: -5 },
      global: { plugins: [i18n] }
    })
    expect(wrapper.find('.kpi-trend').classes()).toContain('dusus')
  })

  it('sparkline verisi varsa mini grafik render eder', () => {
    const wrapper = mount(KpiKart, {
      props: { baslik: 'X', deger: 10, paraBirimi: false, sparkline: [1, 2, 3] },
      global: { plugins: [i18n] }
    })
    expect(wrapper.find('.chart-line').exists()).toBe(true)
  })
})
