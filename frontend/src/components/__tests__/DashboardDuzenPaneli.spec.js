import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardDuzenPaneli from '../dashboard/DashboardDuzenPaneli.vue'

const widgetlar = [
  { key: 'kpi', etiket: 'Finansal Göstergeler', gorunur: true, boyut: 2 },
  { key: 'gorevler', etiket: 'Görev Merkezi', gorunur: true, boyut: 1 },
  { key: 'akis', etiket: 'Canlı Akış', gorunur: false, boyut: 1 }
]

const mountEt = (props = {}) =>
  mount(DashboardDuzenPaneli, {
    props: { widgetlar, ...props },
    global: { plugins: [i18n] }
  })

describe('DashboardDuzenPaneli', () => {
  it('tüm widget\'ları görünürlük ve boyutla listeler', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('[data-widget]')).toHaveLength(3)
    expect(wrapper.find('[data-widget="akis"]').classes()).toContain('duzen-satir--gizli')
    expect(wrapper.find('[data-widget="kpi"] .duzen-buton--boyut').text()).toBe('2x')
  })

  it('görünürlük değişimini bildirir', async () => {
    const wrapper = mountEt()
    await wrapper.find('[data-widget="gorevler"] input[type="checkbox"]').setValue(false)
    expect(wrapper.emitted('degistir')?.[0]?.[0]).toEqual({
      key: 'gorevler',
      alan: 'gorunur',
      deger: false
    })
  })

  it('boyut değişimini bildirir', async () => {
    const wrapper = mountEt()
    await wrapper.find('[data-widget="kpi"] .duzen-buton--boyut').trigger('click')
    expect(wrapper.emitted('degistir')?.[0]?.[0]).toEqual({ key: 'kpi', alan: 'boyut', deger: 1 })
  })

  it('yukarı/aşağı taşıma bildirir ve uçlarda devre dışı kalır', async () => {
    const wrapper = mountEt()
    const ilkSatir = wrapper.find('[data-widget="kpi"]')
    expect(ilkSatir.find('[aria-label*="Yukarı"]').attributes('disabled')).toBeDefined()

    await wrapper.find('[data-widget="gorevler"] [aria-label*="Yukarı"]').trigger('click')
    expect(wrapper.emitted('tasi')?.[0]?.[0]).toEqual({ key: 'gorevler', yon: -1 })

    await wrapper.find('[data-widget="gorevler"] [aria-label*="Aşağı"]').trigger('click')
    expect(wrapper.emitted('tasi')?.[1]?.[0]).toEqual({ key: 'gorevler', yon: 1 })

    const sonSatir = wrapper.find('[data-widget="akis"]')
    expect(sonSatir.find('[aria-label*="Aşağı"]').attributes('disabled')).toBeDefined()
  })

  it('kaydet, iptal ve sıfırla olaylarını yayar', async () => {
    const wrapper = mountEt()
    await wrapper.find('.duzen-eylem--kaydet').trigger('click')
    await wrapper.find('.duzen-eylem--iptal').trigger('click')
    await wrapper.find('.duzen-eylem--sifirla').trigger('click')
    expect(wrapper.emitted('kaydet')).toHaveLength(1)
    expect(wrapper.emitted('iptal')).toHaveLength(1)
    expect(wrapper.emitted('sifirla')).toHaveLength(1)
  })

  it('kaydedilirken kaydet butonunu kilitler', () => {
    const wrapper = mountEt({ kaydediliyor: true })
    expect(wrapper.find('.duzen-eylem--kaydet').attributes('disabled')).toBeDefined()
  })

  it('rol varsayılanına sıfırla etiketi çeviriden gelir', () => {
    const wrapper = mountEt()
    expect(wrapper.find('.duzen-eylem--sifirla').text()).toBe(i18n.global.t('dashboard.duzenSifirla'))
  })
})
