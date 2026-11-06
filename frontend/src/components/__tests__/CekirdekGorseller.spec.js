import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import FinansKarti from '../dashboard/kokpit/FinansKarti.vue'
import MetrikKutu from '../dashboard/kokpit/MetrikKutu.vue'
import MiniTablo from '../dashboard/kokpit/MiniTablo.vue'
import MikroGrafik from '../dashboard/kokpit/MikroGrafik.vue'

describe('kokpit çekirdek görselleri', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('finansKartiBaslikVeHataGosterir', () => {
    const wrapper = mount(FinansKarti, {
      props: { baslik: 'Nakit', hata: 'Veri alınamadı' },
      slots: { default: '<p>içerik</p>' }
    })

    expect(wrapper.text()).toContain('Nakit')
    expect(wrapper.find('.fin-kart-hata').attributes('role')).toBe('alert')
    expect(wrapper.text()).toContain('Veri alınamadı')
    expect(wrapper.find('.fin-kart-govde').exists()).toBe(false)
  })

  it('finansKartiYukleniyorDurumundaIskeletGosterir', () => {
    const wrapper = mount(FinansKarti, {
      props: { baslik: 'Nakit', yukleniyor: true },
      slots: { default: '<p>içerik</p>' }
    })

    expect(wrapper.find('.fin-kart-iskelet').exists()).toBe(true)
    expect(wrapper.find('.fin-kart-govde').exists()).toBe(false)
  })

  it('metrikKutuDegisimVeKritikTasiyor', () => {
    const wrapper = mount(MetrikKutu, {
      props: {
        etiket: 'Net Nakit',
        deger: 1234.5,
        degisimYuzde: 12.3,
        kritik: true,
        sparkline: [1, 2, 3]
      }
    })

    expect(wrapper.text()).toContain('1.234,50')
    expect(wrapper.text()).toContain('12,3%')
    expect(wrapper.classes()).toContain('metrik-kutu--kritik')
    expect(wrapper.find('.metrik-kutu-kritik').exists()).toBe(true)
    expect(wrapper.findComponent(MikroGrafik).exists()).toBe(true)
  })

  it('metrikKutuTersYondeRenkDegistirir', () => {
    const wrapper = mount(MetrikKutu, {
      props: {
        etiket: 'Vadesi Geçen',
        deger: 500,
        degisimYuzde: 10,
        ters: true
      }
    })

    expect(wrapper.find('.metrik-kutu-degisim').classes())
      .toContain('metrik-kutu-degisim--kotu')
  })

  it('miniTabloSatirSecmeOlayiYayar', async () => {
    const satirlar = [
      { id: 1, ad: 'Müşteri A', tutar: 100 },
      { id: 2, ad: 'Müşteri B', tutar: 200 }
    ]
    const wrapper = mount(MiniTablo, {
      props: {
        kolonlar: [
          { anahtar: 'ad', etiket: 'Ad' },
          { anahtar: 'tutar', etiket: 'Tutar', hiza: 'sag', bicim: 'para' }
        ],
        satirlar
      }
    })

    expect(wrapper.findAll('tbody tr')).toHaveLength(2)
    expect(wrapper.text()).toContain('100,00')
    await wrapper.find('tbody tr').trigger('click')
    expect(wrapper.emitted('satir-sec')[0][0]).toEqual(satirlar[0])
    expect(wrapper.find('tbody tr').attributes('tabindex')).toBe('0')
  })

  it('miniTabloBosMetinGosterir', () => {
    const wrapper = mount(MiniTablo, {
      props: { kolonlar: [], satirlar: [], bosMetin: 'Kayıt yok' }
    })

    expect(wrapper.find('[data-test="mini-tablo-bos"]').text()).toBe('Kayıt yok')
  })

  it('mikroGrafikBosVerideCizmez', () => {
    const wrapper = mount(MikroGrafik, { props: { seriler: [] } })

    expect(wrapper.find('svg').exists()).toBe(false)
    expect(wrapper.find('.mikro-grafik-bos').exists()).toBe(true)
  })

  it('mikroGrafikSeriCizer', () => {
    const wrapper = mount(MikroGrafik, { props: { seriler: [1, 2, 3, 4] } })

    expect(wrapper.find('svg').exists()).toBe(true)
    expect(wrapper.find('polyline').attributes('points')).toBeTruthy()
    expect(wrapper.find('polygon').exists()).toBe(true)
  })

  it('mikroGrafikBarTipiCizer', () => {
    const wrapper = mount(MikroGrafik, {
      props: { tip: 'bar', seriler: [1, 2, 3] }
    })

    expect(wrapper.findAll('rect')).toHaveLength(3)
  })

  it('reducedMotionIleAnimasyonKapali', () => {
    vi.spyOn(window, 'matchMedia').mockImplementation((query) => ({
      matches: query.includes('prefers-reduced-motion'),
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn()
    }))

    const wrapper = mount(FinansKarti, {
      props: { baslik: 'Nakit', yukleniyor: true }
    })

    expect(wrapper.classes()).toContain('fin-kart--sakin')
  })
})
