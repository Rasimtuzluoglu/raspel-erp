import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import NakitBolumu from '../dashboard/kokpit/NakitBolumu.vue'
import AlacakVerecekBolumu from '../dashboard/kokpit/AlacakVerecekBolumu.vue'

const mountOptions = {
  global: { plugins: [i18n] }
}

const nakitVeri = {
  kasa: 20000,
  banka: 100000,
  toplam: 120000,
  trendYuzde: 4.2,
  sparkline: [90000, 100000, 120000],
  gunlukAkis: [{ net: 100 }, { net: -50 }, { net: 25 }],
  projeksiyon: {
    tahminiBitisBakiyesi: 140000,
    toplamBeklenenGiris: 5000,
    toplamBeklenenCikis: 3000,
    gunluk: [{ kumulatif: 120000 }, { kumulatif: 140000 }]
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
    expect(alacakWrapper.emitted('detay')[1][0].query.cariId).toBe('1')
  })
})
