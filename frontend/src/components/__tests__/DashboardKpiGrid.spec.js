import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardKpiGrid from '../dashboard/DashboardKpiGrid.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const kpi = {
  likidite: { deger: 120000, kasa: 20000, banka: 100000, sparkline: [1, 2, 3] },
  netCari: { alacak: 90000, borc: 40000 },
  ciro: { deger: 300000, hedef: 500000, yuzde: 60 },
  kar: { deger: 80000, hedef: 100000, marj: 26.6 }
}
const bugun = { tahsilat: 15000, odeme: 4000, siparis: 3, teslimat: 2 }
const stok = { deger: 250000, kritikAdet: 5 }

const mountEt = (props = {}) =>
  mount(DashboardKpiGrid, {
    props: { kpi, bugun, stok, bekleyenSiparis: 4, ...props },
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub, SparkLine: true }
    }
  })

describe('DashboardKpiGrid — 9 kart', () => {
  it('dokuz KPI kartını 5 + 4 düzeninde render eder', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('.kpi-kart')).toHaveLength(9)
    expect(wrapper.attributes('data-test')).toBe('kpi-grid')
    expect(wrapper.attributes('role')).toBe('group')

    for (const test of [
      'kpi-likidite',
      'kpi-alacak',
      'kpi-borc',
      'kpi-ciro',
      'kpi-kar',
      'kpi-tahsilat',
      'kpi-odeme',
      'kpi-bekleyen-siparis',
      'kpi-stok-degeri'
    ]) {
      expect(wrapper.find(`[data-test="${test}"]`).exists(), test).toBe(true)
    }
  })

  it('değerleri ve alt etiketleri doğru biçimler', () => {
    const wrapper = mountEt()
    expect(wrapper.find('[data-test="kpi-deger-likidite"]').text()).toContain('120.000,00')
    expect(wrapper.find('[data-test="kpi-deger-alacak"]').text()).toContain('90.000,00')
    expect(wrapper.find('[data-test="kpi-deger-borc"]').text()).toContain('40.000,00')
    expect(wrapper.find('[data-test="kpi-deger-ciro"]').text()).toContain('300.000,00')
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('%60')
    expect(wrapper.find('[data-test="kpi-deger-kar"]').text()).toContain('+')
    expect(wrapper.find('[data-test="kpi-kar"]').text()).toContain('26.6')
    expect(wrapper.find('[data-test="kpi-deger-tahsilat"]').text()).toContain('15.000,00')
    expect(wrapper.find('[data-test="kpi-deger-odeme"]').text()).toContain('4.000,00')
    expect(wrapper.find('[data-test="kpi-deger-bekleyen-siparis"]').text()).toBe('4')
    expect(wrapper.find('[data-test="kpi-deger-stok-degeri"]').text()).toContain('250.000,00')
    expect(wrapper.find('[data-test="kpi-stok-degeri"]').text()).toContain('5')
  })

  it('likidite sparkline\'ı gösterir, yapay delta/trend göstermez', () => {
    const wrapper = mountEt()
    expect(wrapper.find('[data-test="kpi-likidite"] .kpi-spark').exists()).toBe(true)
    expect(wrapper.find('.kpi-delta').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('%4.2')
  })

  it('eksik veride "—" gösterir, sıfır uydurmaz; bekleyen sipariş 0 kalır', () => {
    const wrapper = mountEt({
      kpi: {},
      bugun: {},
      stok: {},
      bekleyenSiparis: 0
    })
    for (const test of ['likidite', 'alacak', 'borc', 'ciro', 'kar', 'tahsilat', 'odeme', 'stok-degeri']) {
      expect(wrapper.find(`[data-test="kpi-deger-${test}"]`).text()).toBe('—')
    }
    expect(wrapper.find('[data-test="kpi-deger-bekleyen-siparis"]').text()).toBe('0')
    expect(wrapper.findAll('.kpi-hedef-yok')).toHaveLength(2)
    expect(wrapper.find('[data-test="kpi-likidite"] .kpi-spark').exists()).toBe(false)
  })

  it('yüklenirken 9 iskelet kart çizer', () => {
    const wrapper = mountEt({ kpi: null, yukleniyor: true })
    expect(wrapper.findAll('.kpi-kart--iskelet')).toHaveLength(9)
    expect(wrapper.find('[data-test="kpi-deger-likidite"]').exists()).toBe(false)
  })

  it('kritik stok adedini stok kartı altında gösterir', () => {
    const wrapper = mountEt({ stok: { deger: 1000, kritikAdet: 12 } })
    expect(wrapper.find('[data-test="kpi-stok-degeri"]').text()).toContain('12')
  })
})
