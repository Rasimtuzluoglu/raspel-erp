import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardDuzenPaneli from '../dashboard/DashboardDuzenPaneli.vue'

const widgetlar = [
  { key: 'kpi', etiket: 'Finansal Göstergeler', gorunur: true, bolge: 'ana', sabit: true },
  { key: 'gorevler', etiket: 'Görev Merkezi', gorunur: true, bolge: 'ana', sabit: false },
  { key: 'akis', etiket: 'Canlı Akış', gorunur: false, bolge: 'ana', sabit: false },
  { key: 'icgoruler', etiket: 'İçgörüler', gorunur: true, bolge: 'ray', sabit: false },
  { key: 'kritikStok', etiket: 'Kritik Stok', gorunur: true, bolge: 'ray', sabit: false }
]

const mountEt = (props = {}) =>
  mount(DashboardDuzenPaneli, {
    props: { widgetlar, ...props },
    global: { plugins: [i18n] }
  })

describe('DashboardDuzenPaneli', () => {
  it('widget\'ları iki grupta listeler (ana içerik / sağ ray)', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('[data-widget]')).toHaveLength(5)

    const ana = wrapper.find('[data-bolge="ana"]')
    const ray = wrapper.find('[data-bolge="ray"]')
    expect(ana.text()).toContain(i18n.global.t('dashboard.anaIcerik'))
    expect(ray.text()).toContain(i18n.global.t('dashboard.sagRay'))
    expect(ana.findAll('[data-widget]').map((w) => w.attributes('data-widget'))).toEqual([
      'kpi',
      'gorevler',
      'akis'
    ])
    expect(ray.findAll('[data-widget]').map((w) => w.attributes('data-widget'))).toEqual([
      'icgoruler',
      'kritikStok'
    ])
    expect(wrapper.find('[data-widget="akis"]').classes()).toContain('duzen-satir--gizli')
  })

  it('boyut kontrolü içermez', () => {
    const wrapper = mountEt()
    expect(wrapper.find('.duzen-buton--boyut').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('1x')
    expect(wrapper.text()).not.toContain('2x')
  })

  it('sabit bölümde sıra oku yerine "sabit" rozeti gösterir', () => {
    const wrapper = mountEt()
    const kpi = wrapper.find('[data-widget="kpi"]')
    expect(kpi.classes()).toContain('duzen-satir--sabit')
    expect(kpi.find('.duzen-sabit').text()).toBe(i18n.global.t('dashboard.sabit'))
    expect(kpi.find('[aria-label*="Yukarı"]').exists()).toBe(false)
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

  it('yukarı/aşağı taşıma bildirir ve grup uçlarında devre dışı kalır', async () => {
    const wrapper = mountEt()
    const anaIlk = wrapper.find('[data-widget="gorevler"]')
    expect(anaIlk.find('[aria-label*="Yukarı"]').attributes('disabled')).toBeDefined()

    await wrapper.find('[data-widget="gorevler"] [aria-label*="Aşağı"]').trigger('click')
    expect(wrapper.emitted('tasi')?.[0]?.[0]).toEqual({ key: 'gorevler', yon: 1 })

    const anaSon = wrapper.find('[data-widget="akis"]')
    expect(anaSon.find('[aria-label*="Aşağı"]').attributes('disabled')).toBeDefined()

    // Ray grubunda ilk satır yukarı taşınamaz, son satır aşağı taşınamaz.
    const rayIlk = wrapper.find('[data-widget="icgoruler"]')
    expect(rayIlk.find('[aria-label*="Yukarı"]').attributes('disabled')).toBeDefined()
    const raySon = wrapper.find('[data-widget="kritikStok"]')
    expect(raySon.find('[aria-label*="Aşağı"]').attributes('disabled')).toBeDefined()
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
