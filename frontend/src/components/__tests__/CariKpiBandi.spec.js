import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CariKpiBandi from '../cari/CariKpiBandi.vue'

const stubs = {
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>'
  }
}

const mountBandi = (props = {}) => mount(CariKpiBandi, {
  global: { plugins: [i18n], stubs },
  props: {
    cariOzet: { toplamKayit: 2, alacakli: 1800, borclu: 939737.5 },
    toplamKayit: 99,
    filtreBakiye: null,
    yukleniyor: false,
    aktifFiltreler: [],
    ...props
  }
})

describe('CariKpiBandi', () => {
  it('uc KPI kartini ve ozet degerlerini gosterir', () => {
    const wrapper = mountBandi()

    expect(wrapper.findAll('.cari-kpi-tikla')).toHaveLength(3)
    expect(wrapper.get('[data-test="kpi-toplam"]').text()).toContain('2')
    expect(wrapper.get('[data-test="kpi-alacak"]').text()).toContain('1800')
    expect(wrapper.get('[data-test="kpi-borc"]').text()).toContain('939737.5')
  })

  it('toplam kayit yoksa store toplamini kullanir', () => {
    const wrapper = mountBandi({ cariOzet: {}, toplamKayit: 99 })

    expect(wrapper.get('[data-test="kpi-toplam"]').text()).toContain('99')
  })

  it('bakiye kartina tiklayinca filtre olayini yayar ve aktif sinifi alir', async () => {
    const wrapper = mountBandi({ filtreBakiye: 'alacak' })

    const alacak = wrapper.get('[data-test="kpi-alacak"]')
    expect(alacak.classes()).toContain('cari-kpi-tikla--aktif')
    expect(alacak.attributes('aria-pressed')).toBe('true')

    await alacak.trigger('click')
    expect(wrapper.emitted('bakiye-filtre')[0]).toEqual(['alacak'])
  })

  it('toplam kartina tiklayinca temizle olayini yayar', async () => {
    const wrapper = mountBandi()

    await wrapper.get('[data-test="kpi-toplam"]').trigger('click')

    expect(wrapper.emitted('temizle')).toHaveLength(1)
  })

  it('aktif filtre varken bilgi notunu gosterir', () => {
    const wrapper = mountBandi({ aktifFiltreler: [{ tip: 'tur', etiket: 'Müşteri' }] })

    expect(wrapper.find('.cari-kpi-not').exists()).toBe(true)
  })

  it('yuklenirken KPI kartlarini gostermez', () => {
    const wrapper = mountBandi({ yukleniyor: true })

    expect(wrapper.find('.cari-kpi').exists()).toBe(false)
    expect(wrapper.find('.cari-kpi-not').exists()).toBe(false)
  })
})
