import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import FinansOzetBandi from '../dashboard/kokpit/FinansOzetBandi.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

const kpi = [
  { anahtar: 'netNakit', deger: 1000, hedef: { route: '/kasa', query: {} } },
  { anahtar: 'alacak', deger: 2000, hedef: { route: '/cari-hesaplar', query: {} } },
  { anahtar: 'verecek', deger: 500, hedef: { route: '/cari-hesaplar', query: {} } },
  { anahtar: 'netCalismaSermayesi', deger: 1500 },
  { anahtar: 'ciro', deger: 3000, degisimYuzde: 10 },
  { anahtar: 'brutKar', deger: 900, degisimYuzde: 4 },
  { anahtar: 'marj', deger: 30, degisimYuzde: 2 },
  { anahtar: 'vadesiGecen', deger: 300, kritik: true }
]

const mountBant = (props = {}) => mount(FinansOzetBandi, {
  global: {
    plugins: [i18n],
    stubs: { 'router-link': RouterLinkStub }
  },
  props: { kpi: [], yukleniyor: false, paraBirimi: 'TRY', ...props }
})

describe('FinansOzetBandi', () => {
  it('oncelikli dort metrigi buyuk kartlarda gosterir', () => {
    const wrapper = mountBant({ kpi })

    for (const anahtar of ['ciro', 'marj', 'alacak', 'verecek']) {
      expect(wrapper.find(`[data-test="kpi-${anahtar}"]`).exists()).toBe(true)
    }
    // İkincil metrikler üst ızgarada büyük kart olarak yer almaz.
    expect(wrapper.find('[data-test="kpi-netNakit"]').exists()).toBe(false)
  })

  it('kalan metrikleri kompakt cip olarak korur', () => {
    const wrapper = mountBant({ kpi })

    const ikincil = wrapper.find('[data-test="finans-ikincil"]')
    expect(ikincil.exists()).toBe(true)
    for (const anahtar of ['netNakit', 'brutKar', 'netCalismaSermayesi', 'vadesiGecen']) {
      expect(ikincil.find(`[data-test="kpi-alt-${anahtar}"]`).exists()).toBe(true)
    }
  })

  it('marj yoksa dorduncu kart diger metrikle tamamlanir', () => {
    const eksik = kpi.filter((oge) => oge.anahtar !== 'marj')
    const wrapper = mountBant({ kpi: eksik })

    const buyukler = wrapper.findAll('.finans-ust > *')
    expect(buyukler).toHaveLength(4)
    expect(wrapper.find('[data-test="kpi-netNakit"]').exists()).toBe(true)
  })

  it('yuklenirken iskelet gosterir', () => {
    const wrapper = mountBant({ yukleniyor: true })

    expect(wrapper.findAll('.metrik-iskelet')).toHaveLength(4)
    expect(wrapper.findAll('.ikincil-iskelet')).toHaveLength(4)
  })

  it('marj degerini yuzde olarak bicimler', () => {
    const wrapper = mountBant({ kpi: [kpi[6]] })

    expect(wrapper.find('[data-test="kpi-marj"]').text()).toContain('30%')
  })

  it('ilk metrigi buyuk odak karti olarak isaretler', () => {
    const wrapper = mountBant({ kpi })
    const ciro = wrapper.find('[data-test="kpi-ciro"]')
    expect(ciro.classes()).toContain('metrik-kutu--buyuk')
    expect(wrapper.find('[data-test="finans-ust"]').classes()).toContain('finans-ust--odakli')
  })

  it('dort buyuk metrik ve kompakt serit korunur', () => {
    const wrapper = mountBant({ kpi })
    expect(wrapper.findAll('.finans-ust > *')).toHaveLength(4)
    expect(wrapper.find('[data-test="finans-ikincil"]').exists()).toBe(true)
  })

  it('degisim yuzdesi yoksa sahte rozet gostermez', () => {
    const wrapper = mountBant({ kpi: [{ anahtar: 'ciro', deger: 0 }] })
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).toContain('0,00')
    expect(wrapper.find('[data-test="kpi-ciro"] .metrik-kutu-degisim--notr').exists()).toBe(true)
    expect(wrapper.find('[data-test="kpi-ciro"]').text()).not.toContain('-100')
  })

  it('tam olarak tek buyuk karti ve 44/28 px sparkline hiyerarsisini korur', () => {
    const sparkline = [1, 2, 3, 4]
    const veri = kpi.map((oge) => (
      ['ciro', 'alacak'].includes(oge.anahtar) ? { ...oge, sparkline } : oge
    ))
    const wrapper = mountBant({ kpi: veri })

    expect(wrapper.findAll('.metrik-kutu--buyuk')).toHaveLength(1)

    const kartlar = wrapper.findAll('.finans-ust > *')
    expect(kartlar[0].find('.mikro-grafik').element.style.height).toBe('44px')
    const destek = kartlar.slice(1).find((kart) => kart.find('.mikro-grafik').exists())
    expect(destek.find('.mikro-grafik').element.style.height).toBe('28px')
  })

  it('gercek degisim yuzdesinde yonlu rozet gosterir', () => {
    const artan = mountBant({ kpi: [{ anahtar: 'ciro', deger: 100, degisimYuzde: 12 }] })
    expect(artan.find('[data-test="kpi-ciro"] .metrik-kutu-degisim--iyi').exists()).toBe(true)
    expect(artan.find('[data-test="kpi-ciro"]').text()).toContain('+12')

    const azalan = mountBant({ kpi: [{ anahtar: 'ciro', deger: 100, degisimYuzde: -7.5 }] })
    expect(azalan.find('[data-test="kpi-ciro"] .metrik-kutu-degisim--kotu').exists()).toBe(true)
    expect(azalan.find('[data-test="kpi-ciro"]').text()).toContain('-7,5')
  })
})
