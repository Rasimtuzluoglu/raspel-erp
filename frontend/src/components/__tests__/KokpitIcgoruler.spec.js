import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitIcgoruler from '../dashboard/kokpit/KokpitIcgoruler.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :data-to="JSON.stringify(to)"><slot /></a>'
}

const mountSerit = (props = {}) => mount(KokpitIcgoruler, {
  global: {
    plugins: [i18n],
    stubs: { 'router-link': RouterLinkStub }
  },
  props: {
    icgoruler: [],
    yukleniyor: false,
    ...props
  }
})

const icgoruler = [
  {
    seviye: 'KRITIK',
    baslik: 'Vadesi geçen faturalar var',
    mesaj: '3 faturanın vadesi geçmiş; tahsilat önerilir.',
    route: '/tahsilat?durum=gecikmis',
    ikon: 'pi pi-exclamation-triangle'
  },
  {
    seviye: 'WARN',
    baslik: 'Ciro hedefin gerisinde',
    mesaj: 'Bu ay ciro hedefinin %40 kadarı gerçekleşti.',
    route: '/raporlar',
    ikon: 'pi pi-chart-line'
  }
]

describe('KokpitIcgoruler', () => {
  it('icgoruleriBaslikVeMesajlaGosterir', () => {
    const wrapper = mountSerit({ icgoruler })

    expect(wrapper.text()).toContain('Yönetici İçgörüleri')
    expect(wrapper.text()).toContain('Vadesi geçen faturalar var')
    expect(wrapper.text()).toContain('Ciro hedefin gerisinde')
    expect(wrapper.find('[data-test="kokpit-icgoru-0"]').attributes('data-to')).toContain('/tahsilat')
  })

  it('bosDurumGosterir', () => {
    const wrapper = mountSerit()

    expect(wrapper.find('[data-test="kokpit-icgoru-bos"]').exists()).toBe(true)
  })

  it('yuklenirkenIskeletGosterir', () => {
    const wrapper = mountSerit({ yukleniyor: true })

    expect(wrapper.find('.icgoru-iskelet').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-icgoru-bos"]').exists()).toBe(false)
  })
})
