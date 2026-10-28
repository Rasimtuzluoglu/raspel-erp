import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardDuzenPaneli from '../dashboard/DashboardDuzenPaneli.vue'

const widgetlar = [
  { key: 'kpi', etiket: 'Finansal Göstergeler', gorunur: true, sabit: true },
  { key: 'bugunSerit', etiket: 'Bugün', gorunur: true, sabit: true },
  { key: 'gorevler', etiket: 'Görev Merkezi', gorunur: true, sabit: false },
  { key: 'akis', etiket: 'Canlı Akış', gorunur: false, sabit: false },
  { key: 'icgoruler', etiket: 'İçgörüler', gorunur: true, sabit: false },
  { key: 'kritikStok', etiket: 'Kritik Stok', gorunur: true, sabit: false }
]

const mountEt = (props = {}) =>
  mount(DashboardDuzenPaneli, {
    props: { widgetlar, ...props },
    global: { plugins: [i18n] }
  })

describe('DashboardDuzenPaneli', () => {
  it('widget\'ları tek düz listede gösterir', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('[data-widget]')).toHaveLength(6)
    expect(wrapper.find('[data-bolge]').exists()).toBe(false)
    expect(wrapper.findAll('[data-widget]').map((w) => w.attributes('data-widget'))).toEqual([
      'kpi',
      'bugunSerit',
      'gorevler',
      'akis',
      'icgoruler',
      'kritikStok'
    ])
    expect(wrapper.find('[data-widget="akis"]').classes()).toContain('duzen-satir--gizli')
  })

  it('boyut kontrolü içermez', () => {
    const wrapper = mountEt()
    expect(wrapper.find('.duzen-buton--boyut').exists()).toBe(false)
    expect(wrapper.find('[data-test="boyut-kontrol"]').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('1x')
    expect(wrapper.text()).not.toContain('2x')
  })

  it('sabit bölümlerde sıra oku yerine "sabit" rozeti gösterir', () => {
    const wrapper = mountEt()
    for (const key of ['kpi', 'bugunSerit']) {
      const satir = wrapper.find(`[data-widget="${key}"]`)
      expect(satir.classes()).toContain('duzen-satir--sabit')
      expect(satir.find('.duzen-sabit').text()).toBe(i18n.global.t('dashboard.sabit'))
      expect(satir.find('[aria-label*="Yukarı"]').exists()).toBe(false)
      expect(satir.find('[aria-label*="Aşağı"]').exists()).toBe(false)
    }
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

  it('yukarı/aşağı taşıma bildirir ve liste uçlarında devre dışı kalır', async () => {
    const wrapper = mountEt()
    const ilk = wrapper.find('[data-widget="gorevler"]')
    expect(ilk.find('[aria-label*="Yukarı"]').attributes('disabled')).toBeDefined()

    await wrapper.find('[data-widget="gorevler"] [aria-label*="Aşağı"]').trigger('click')
    expect(wrapper.emitted('tasi')?.[0]?.[0]).toEqual({ key: 'gorevler', yon: 1 })

    const son = wrapper.find('[data-widget="kritikStok"]')
    expect(son.find('[aria-label*="Aşağı"]').attributes('disabled')).toBeDefined()

    // Sabitlerin hemen altındaki ilk taşınabilir satır yukarı çıkabilir.
    const icgoruler = wrapper.find('[data-widget="icgoruler"]')
    expect(icgoruler.find('[aria-label*="Yukarı"]').attributes('disabled')).toBeUndefined()
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
