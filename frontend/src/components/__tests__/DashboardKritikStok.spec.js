import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardKritikStok from '../dashboard/DashboardKritikStok.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const mountEt = (props = {}) =>
  mount(DashboardKritikStok, {
    props,
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

const stoklar = [
  { id: 1, stokKodu: 'KA-1', ad: 'Kalem A', miktar: 1, minMiktar: 10 },
  { id: 2, stokKodu: 'KA-2', ad: 'Kalem B', miktar: 2, minMiktar: 20 },
  { id: 3, stokKodu: 'KA-3', ad: 'Kalem C', miktar: 3, minMiktar: 30 },
  { id: 4, stokKodu: 'KA-4', ad: 'Kalem D', miktar: 4, minMiktar: 40 },
  { id: 5, stokKodu: 'KA-5', ad: 'Kalem E', miktar: 5, minMiktar: 50 }
]

describe('DashboardKritikStok', () => {
  it('en fazla 4 satırı ad ve miktar/min ile listeler', () => {
    const wrapper = mountEt({ stoklar })
    const satirlar = wrapper.findAll('.kritik-satir')
    expect(satirlar).toHaveLength(4)
    expect(satirlar[0].text()).toContain('Kalem A')
    expect(satirlar[0].text()).toContain('1 / min 10')
    // 5. ürün listelenmez.
    expect(wrapper.text()).not.toContain('Kalem E')
  })

  it('satırlar ve "tümünü gör" bağlantısı kritik stok listesine gider', () => {
    const wrapper = mountEt({ stoklar })
    expect(wrapper.find('.kritik-satir').attributes('href')).toBe('/stoklar?kritik=1')
    const tumu = wrapper.find('.kritik-tumu')
    expect(tumu.text()).toContain(i18n.global.t('dashboard.tumunuGor'))
    expect(tumu.attributes('href')).toBe('/stoklar?kritik=1')
  })

  it('liste yoksa özet sayacı gösterir', () => {
    const wrapper = mountEt({ stoklar: [], stok: { kritikAdet: 3 } })
    expect(wrapper.findAll('.kritik-satir')).toHaveLength(0)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.kritikStokSayisi', { n: 3 }))
  })

  it('kritik ürün yoksa bilgilendirir', () => {
    const wrapper = mountEt({ stoklar: [], stok: { kritikAdet: 0 } })
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.kritikStokYok'))
  })

  it('yüklenirken iskelet gösterir', () => {
    const wrapper = mountEt({ stoklar: [], yukleniyor: true })
    expect(wrapper.find('.kritik-iskelet').exists()).toBe(true)
  })
})
