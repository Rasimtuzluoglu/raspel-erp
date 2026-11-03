import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardDuzenPaneli from '../dashboard/DashboardDuzenPaneli.vue'

const widgetlar = [
  { key: 'kpi', etiket: 'Finansal Göstergeler', gorunur: true, sabit: true },
  { key: 'bugunSerit', etiket: 'Bugün', gorunur: false, sabit: true },
  { key: 'nakitAkisi', etiket: 'Nakit Akışı', gorunur: true, sabit: false },
  { key: 'siparisDurumlari', etiket: 'Sipariş Durumları', gorunur: false, sabit: false },
  { key: 'gorevler', etiket: 'Görev Merkezi', gorunur: true, sabit: false },
  { key: 'akis', etiket: 'Canlı Akış', gorunur: false, sabit: false },
  { key: 'icgoruler', etiket: 'İçgörüler', gorunur: true, sabit: false }
]

const mountEt = (props = {}) =>
  mount(DashboardDuzenPaneli, {
    props: { widgetlar, ...props },
    global: { plugins: [i18n] }
  })

describe('DashboardDuzenPaneli', () => {
  it('sabit bantları ve iki bölge grubunu gösterir', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('[data-widget]')).toHaveLength(7)

    const sabit = wrapper.find('[data-bolge="SABIT"]')
    expect(sabit.findAll('[data-widget]').map((w) => w.attributes('data-widget'))).toEqual([
      'kpi',
      'bugunSerit'
    ])

    const sol = wrapper.find('[data-bolge="SOL"]')
    expect(sol.findAll('[data-widget]').map((w) => w.attributes('data-widget'))).toEqual([
      'nakitAkisi'
    ])
    expect(sol.find('.duzen-grup-baslik').text()).toBe(i18n.global.t('dashboard.bolgeGrafikler'))

    const sag = wrapper.find('[data-bolge="SAG"]')
    expect(sag.findAll('[data-widget]').map((w) => w.attributes('data-widget'))).toEqual([
      'siparisDurumlari',
      'gorevler',
      'akis',
      'icgoruler'
    ])
    expect(sag.find('.duzen-grup-baslik').text()).toBe(i18n.global.t('dashboard.bolgeAksiyon'))

    expect(wrapper.find('[data-widget="akis"]').classes()).toContain('duzen-satir--gizli')
    expect(wrapper.find('[data-widget="bugunSerit"]').classes()).toContain('duzen-satir--gizli')
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

  it('yukarı/aşağı taşıma bildirir ve her bölgenin uçlarında devre dışı kalır', async () => {
    const wrapper = mountEt()

    // SOL grubu: ilk satır yukarı, son satır aşağı gidemez.
    expect(
      wrapper.find('[data-widget="nakitAkisi"] [aria-label*="Yukarı"]').attributes('disabled')
    ).toBeDefined()
    expect(
      wrapper.find('[data-widget="nakitAkisi"] [aria-label*="Aşağı"]').attributes('disabled')
    ).toBeDefined()

    // SAG grubu: ilk satır yukarı, son satır aşağı gidemez.
    expect(
      wrapper.find('[data-widget="siparisDurumlari"] [aria-label*="Yukarı"]').attributes('disabled')
    ).toBeDefined()
    expect(
      wrapper.find('[data-widget="icgoruler"] [aria-label*="Aşağı"]').attributes('disabled')
    ).toBeDefined()

    // Orta satırlar her iki yöne gidebilir.
    expect(
      wrapper.find('[data-widget="akis"] [aria-label*="Yukarı"]').attributes('disabled')
    ).toBeUndefined()

    await wrapper.find('[data-widget="gorevler"] [aria-label*="Aşağı"]').trigger('click')
    expect(wrapper.emitted('tasi')?.[0]?.[0]).toEqual({ key: 'gorevler', yon: 1 })
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
