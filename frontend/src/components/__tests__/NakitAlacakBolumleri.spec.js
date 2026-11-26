import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import NakitBolumu from '../dashboard/kokpit/NakitBolumu.vue'
import AlacakVerecekBolumu from '../dashboard/kokpit/AlacakVerecekBolumu.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :data-to="JSON.stringify(to)"><slot /></a>'
}

const mountOptions = {
  global: {
    plugins: [i18n],
    stubs: { 'router-link': RouterLinkStub }
  }
}

const nakitVeri = {
  kasa: 20000,
  banka: 100000,
  toplam: 120000,
  trendYuzde: 4.2,
  sparkline: [90000, 100000, 120000],
  gunlukAkis: [
    { tarih: '2026-11-24', giris: 200, cikis: 100, net: 100 },
    { tarih: '2026-11-25', giris: 50, cikis: 100, net: -50 },
    { tarih: '2026-11-26', giris: 100, cikis: 75, net: 25 }
  ],
  projeksiyon: {
    tahminiBitisBakiyesi: 140000,
    toplamBeklenenGiris: 5000,
    toplamBeklenenCikis: 3000,
    gunluk: [
      { tarih: '2026-11-27', giris: 2000, cikis: 1000, kumulatif: 121000 },
      { tarih: '2026-11-28', giris: 3000, cikis: 2000, kumulatif: 122000 }
    ]
  },
  hedef: { route: '/kasa', query: {} }
}

const alacakVeri = {
  alacakToplam: 60000,
  verecekToplam: 20000,
  net: 40000,
  tahsilatOrani: 70,
  yaslanma: [
    { anahtar: 'VADEDI_GELMEMIS', etiket: 'Vadesi Gelmemiş', tutar: 1000, adet: null },
    { anahtar: 'GUN_0_30', etiket: '0-30 Gün', tutar: 500, adet: null }
  ],
  borclular: [{ cariId: 1, ad: 'Müşteri A', tutar: 5000 }],
  tedarikciler: [{ cariId: 2, ad: 'Tedarikçi A', tutar: 3000 }],
  hedef: { route: '/cari-hesaplar', query: {} }
}

describe('nakit ve alacak/verecek bölümleri', () => {
  it('nakitBolumuDagilimVeProjeksiyonGosterir', () => {
    const wrapper = mount(NakitBolumu, {
      ...mountOptions,
      props: { veri: nakitVeri, paraBirimi: 'TRY' }
    })

    expect(wrapper.text()).toContain('120.000,00')
    expect(wrapper.text()).toContain('20.000,00')
    expect(wrapper.text()).toContain('100.000,00')
    expect(wrapper.text()).toContain('140.000,00')
    expect(wrapper.text()).toContain('Kasa')
    expect(wrapper.text()).toContain('Banka')
    expect(wrapper.find('[data-test="nakit-gunluk-grafik"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-projeksiyon"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-sparkline"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-saglik"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="nakit-runway"]').exists()).toBe(true)
  })

  it('nakitBolumu30GunSecimindeProjeksiyonSerisiniVeNetiniKullanir', async () => {
    const wrapper = mount(NakitBolumu, {
      ...mountOptions,
      props: { veri: nakitVeri, paraBirimi: 'TRY' }
    })

    expect(wrapper.find('[data-test="nakit-gunluk-grafik"]').findAll('rect[fill="currentColor"]')).toHaveLength(6)
    expect(wrapper.find('.nakit-net').text()).toContain('75,00')

    await wrapper.find('[data-test="nakit-grafik-30"]').trigger('click')

    expect(wrapper.find('[data-test="nakit-grafik-30"]').attributes('aria-pressed')).toBe('true')
    expect(wrapper.find('[data-test="nakit-gunluk-grafik"]').findAll('rect[fill="currentColor"]')).toHaveLength(4)
    expect(wrapper.find('.nakit-net').text()).toContain('2.000,00')
  })

  it('nakitBolumuBosVerideSifirGosterir', () => {
    const wrapper = mount(NakitBolumu, {
      ...mountOptions,
      props: { veri: null, paraBirimi: 'TRY' }
    })

    expect(wrapper.text()).toContain('0,00')
    expect(wrapper.find('[data-test="nakit-gunluk-grafik"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="nakit-projeksiyon"]').exists()).toBe(true)
  })

  it('alacakBolumuYaslanmaKovalariniGosterir', () => {
    const wrapper = mount(AlacakVerecekBolumu, {
      ...mountOptions,
      props: { veri: alacakVeri, paraBirimi: 'TRY' }
    })

    const yaslanma = wrapper.find('[data-test="alacak-yaslanma"]')
    expect(yaslanma.text()).toContain('Vadesi Gelmemiş')
    expect(yaslanma.text()).toContain('0-30 Gün')
    expect(yaslanma.text()).toContain('1.000,00')
    expect(wrapper.text()).toContain('70%')
  })

  it('alacakBolumuBorcluTedarikciTablolariniGosterir', () => {
    const wrapper = mount(AlacakVerecekBolumu, {
      ...mountOptions,
      props: { veri: alacakVeri, paraBirimi: 'TRY' }
    })

    expect(wrapper.find('[data-test="borclular-tablo"]').text()).toContain('Müşteri A')
    expect(wrapper.find('[data-test="borclular-tablo"]').text()).toContain('5.000,00')
    expect(wrapper.find('[data-test="tedarikciler-tablo"]').text()).toContain('Tedarikçi A')
    expect(wrapper.find('[data-test="tedarikciler-tablo"]').text()).toContain('3.000,00')
    expect(wrapper.find('[data-test="alacak-tahsilat-performansi"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="alacak-oncelik"]').exists()).toBe(true)
  })

  it('alacakOncelikKartiHizliTahsilatAcar', async () => {
    const wrapper = mount(AlacakVerecekBolumu, {
      ...mountOptions,
      props: { veri: alacakVeri, paraBirimi: 'TRY' }
    })

    await wrapper.find('[data-test="alacak-oncelik-tahsilat"]').trigger('click')
    expect(wrapper.emitted('detay')[0][0].route).toBe('/tahsilat')
    expect(wrapper.emitted('detay')[0][0].query.cariId).toBe('1')
  })

  it('tahsilat oranini esige gore renklendirir', () => {
    const iyi = mount(AlacakVerecekBolumu, {
      ...mountOptions,
      props: { veri: { ...alacakVeri, tahsilatOrani: 90 }, paraBirimi: 'TRY' }
    })
    const kotu = mount(AlacakVerecekBolumu, {
      ...mountOptions,
      props: { veri: { ...alacakVeri, tahsilatOrani: 30 }, paraBirimi: 'TRY' }
    })
    expect(iyi.find('[data-test="alacak-tahsilat-performansi"]').classes()).toContain('alacak-performans--iyi')
    expect(kotu.find('[data-test="alacak-tahsilat-performansi"]').classes()).toContain('alacak-performans--kotu')
  })

  it('tahsilat esikleri 49/50/79/80 sinirlarinda dogru seviyeyi secer', () => {
    const seviyeler = [
      [80, 'iyi'],
      [79, 'uyari'],
      [50, 'uyari'],
      [49, 'kotu']
    ]

    for (const [oran, seviye] of seviyeler) {
      const wrapper = mount(AlacakVerecekBolumu, {
        ...mountOptions,
        props: { veri: { ...alacakVeri, tahsilatOrani: oran }, paraBirimi: 'TRY' }
      })
      const performans = wrapper.find('[data-test="alacak-tahsilat-performansi"]')
      expect(performans.classes()).toContain(`alacak-performans--${seviye}`)
      expect(wrapper.find('.alacak-stat-deger--' + seviye).exists()).toBe(true)
    }
  })

  it('tahsilat orani yoksa notr durum ve tire gosterir', () => {
    for (const ham of [null, undefined, '', 'gecersiz']) {
      const wrapper = mount(AlacakVerecekBolumu, {
        ...mountOptions,
        props: { veri: { ...alacakVeri, tahsilatOrani: ham }, paraBirimi: 'TRY' }
      })
      const performans = wrapper.find('[data-test="alacak-tahsilat-performansi"]')
      expect(performans.classes()).toContain('alacak-performans--notr')
      expect(performans.classes()).not.toContain('alacak-performans--kotu')
      const oranDegerleri = wrapper.findAll('.alacak-stat-deger')
      expect(oranDegerleri[2].text()).toBe('—')
      expect(oranDegerleri[2].classes()).toContain('alacak-stat-deger--notr')
      expect(oranDegerleri[2].classes()).not.toContain('alacak-stat-deger--kotu')
    }
  })

  it('bolumDetayOlayiHedefTasiyor', async () => {
    const nakitWrapper = mount(NakitBolumu, {
      ...mountOptions,
      props: { veri: nakitVeri, paraBirimi: 'TRY' }
    })
    await nakitWrapper.find('[data-test="nakit-detay"]').trigger('click')
    expect(nakitWrapper.emitted('detay')[0][0].route).toBe('/kasa')

    const alacakWrapper = mount(AlacakVerecekBolumu, {
      ...mountOptions,
      props: { veri: alacakVeri, paraBirimi: 'TRY' }
    })
    await alacakWrapper.find('[data-test="alacak-detay"]').trigger('click')
    expect(alacakWrapper.emitted('detay')[0][0].route).toBe('/cari-hesaplar')

    await alacakWrapper.find('[data-test="borclular-tablo"] tbody tr').trigger('click')
    expect(alacakWrapper.emitted('detay')[1][0].route).toBe('/tahsilat')
    expect(alacakWrapper.emitted('detay')[1][0].query.cariId).toBe('1')

    await alacakWrapper.find('[data-test="borclu-tahsilat"]').trigger('click')
    expect(alacakWrapper.emitted('detay')[2][0].route).toBe('/tahsilat')
    expect(alacakWrapper.emitted('detay')[2][0].query.cariId).toBe('1')
    expect(alacakWrapper.emitted('detay')).toHaveLength(3)
  })
})
