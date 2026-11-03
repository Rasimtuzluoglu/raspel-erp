import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardKisayolSeridi from '../dashboard/DashboardKisayolSeridi.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const mountEt = () =>
  mount(DashboardKisayolSeridi, {
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('DashboardKisayolSeridi — 8 işlem', () => {
  it('tek satırlık nav olarak 8 kısayol render eder', () => {
    const wrapper = mountEt()
    expect(wrapper.element.tagName).toBe('NAV')
    expect(wrapper.classes()).toContain('kisayol-serit')
    expect(wrapper.attributes('aria-label')).toBe(i18n.global.t('dashboard.kisayolSeridi'))
    expect(wrapper.findAll('.kisayol')).toHaveLength(8)
  })

  it('yalnız mevcut rotalara bağlanır', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('a').map((a) => a.attributes('href'))).toEqual([
      '/faturalar',
      '/hareketler',
      '/hareketler',
      '/hizli-satis',
      '/cari-hesaplar',
      '/kasa',
      '/stoklar',
      '/teklifler'
    ])
  })

  it('her kısayolda ikon ve çeviri etiketi vardır', () => {
    const wrapper = mountEt()
    const kisayollar = wrapper.findAll('.kisayol')
    const beklenen = [
      ['yeniFatura', 'dashboard.kisayolYeniFatura'],
      ['tahsilat', 'dashboard.kisayolTahsilat'],
      ['odeme', 'dashboard.kisayolOdeme'],
      ['hizliSatis', 'dashboard.kisayolHizliSatis'],
      ['yeniCari', 'dashboard.kisayolYeniCari'],
      ['kasa', 'dashboard.kisayolKasa'],
      ['stoklar', 'dashboard.kisayolStoklar'],
      ['teklifler', 'dashboard.kisayolTeklifler']
    ]
    beklenen.forEach(([key, anahtar], i) => {
      expect(kisayollar[i].attributes('data-kisayol')).toBe(key)
      expect(kisayollar[i].find('.kisayol-etiket').text()).toBe(i18n.global.t(anahtar))
      expect(kisayollar[i].find('.kisayol-ikon i').exists()).toBe(true)
    })
  })
})
