import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardNakitAkisi from '../dashboard/DashboardNakitAkisi.vue'

vi.mock('vue-chartjs', () => ({
  Line: { name: 'Line', props: ['data', 'options'], template: '<div class="stub-line" />' }
}))

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const bugun = new Date()
const gunAnahtari = (tarih) =>
  `${tarih.getFullYear()}-${String(tarih.getMonth() + 1).padStart(2, '0')}-${String(tarih.getDate()).padStart(2, '0')}`

const gunler = Array.from({ length: 30 }, (_, i) => {
  const tarih = new Date(bugun.getFullYear(), bugun.getMonth(), bugun.getDate() - (29 - i))
  return { gun: gunAnahtari(tarih), gelir: 1000 + i, gider: 500 }
})

const veri = {
  labels: gunler.map((g) => g.gun),
  datasets: [{ label: 'Gelir', data: gunler.map((g) => g.gelir) }]
}

const projeksiyonlar = [
  { etiket: '30 gün', deger: 125000 },
  { etiket: '60 gün', deger: 148000 },
  { etiket: '90 gün', deger: 162000 }
]

const mountEt = (props = {}) =>
  mount(DashboardNakitAkisi, {
    props: { veri, projeksiyonlar, gun: 30, ...props },
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('DashboardNakitAkisi — tahmini projeksiyon grafiği', () => {
  it('30/60/90 gün tahminini üç noktalı mini çizgi grafik olarak çizer', () => {
    const wrapper = mountEt()
    const grafik = wrapper.find('[data-test="nakit-projeksiyon-grafik"]')

    expect(grafik.exists()).toBe(true)
    expect(grafik.attributes('role')).toBe('img')
    expect(grafik.attributes('aria-label')).toContain(i18n.global.t('dashboard.tahmini'))

    const cizgi = wrapper.findAllComponents({ name: 'Line' })[1]
    expect(cizgi).toBeTruthy()
    const veriProp = cizgi.props('data')
    expect(veriProp.labels).toEqual(['30 gün', '60 gün', '90 gün'])
    expect(veriProp.datasets[0].data).toEqual([125000, 148000, 162000])
    expect(veriProp.datasets[0].label).toBe(i18n.global.t('dashboard.tahmini'))
    wrapper.unmount()
  })

  it('"Tahmini" etiketi ve mevcut çipler korunur', () => {
    const wrapper = mountEt()
    expect(wrapper.find('.nakit-projeksiyon-rozet').text()).toBe(i18n.global.t('dashboard.tahmini'))
    expect(wrapper.findAll('[data-test="nakit-projeksiyon-cip"]')).toHaveLength(3)
    wrapper.unmount()
  })

  it('gerçekleşen nakit verisi yoksa tahmin grafiği çizilmez, çipler kalır', () => {
    const wrapper = mountEt({
      veri: { labels: [], datasets: [] }
    })
    expect(wrapper.find('[data-test="nakit-projeksiyon-grafik"]').exists()).toBe(false)
    expect(wrapper.findAll('[data-test="nakit-projeksiyon-cip"]')).toHaveLength(3)
    wrapper.unmount()
  })

  it('projeksiyon verisi eksikse grafik yerine yalnız çipler gösterilir', () => {
    const wrapper = mountEt({ projeksiyonlar: [] })
    expect(wrapper.find('[data-test="nakit-projeksiyon-grafik"]').exists()).toBe(false)
    wrapper.unmount()
  })
})
