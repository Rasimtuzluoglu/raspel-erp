import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitGlobalFiltre from '../dashboard/kokpit/KokpitGlobalFiltre.vue'
import FinansOzetBandi from '../dashboard/kokpit/FinansOzetBandi.vue'
import KritikDurumSeridi from '../dashboard/kokpit/KritikDurumSeridi.vue'
import MetrikKutu from '../dashboard/kokpit/MetrikKutu.vue'

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

const kpiListesi = [
  { anahtar: 'netNakit', etiket: 'Net Nakit', deger: 120000, degisimYuzde: 4.2, sparkline: [1, 2, 3], kritik: false, hedef: { route: '/kasa', query: {} } },
  { anahtar: 'alacak', etiket: 'Alacak', deger: 90000, kritik: false, hedef: { route: '/cari-hesaplar', query: { bakiyeYonu: 'alacak' } } },
  { anahtar: 'verecek', etiket: 'Verecek', deger: 40000, kritik: false, hedef: { route: '/cari-hesaplar', query: { bakiyeYonu: 'borc' } } },
  { anahtar: 'netCalismaSermayesi', etiket: 'Net Çalışma Sermayesi', deger: 170000, kritik: false, hedef: { route: '/cari-hesaplar', query: {} } },
  { anahtar: 'ciro', etiket: 'Dönem Cirosu', deger: 300000, degisimYuzde: 12, sparkline: [4, 5, 6], kritik: false, hedef: { route: '/faturalar', query: {} } },
  { anahtar: 'brutKar', etiket: 'Dönem Kârı', deger: 80000, degisimYuzde: 8, kritik: false, hedef: { route: '/raporlar', query: {} } },
  { anahtar: 'marj', etiket: 'Kâr Marjı', deger: 26.6, kritik: false, hedef: { route: '/raporlar', query: {} } },
  { anahtar: 'vadesiGecen', etiket: 'Vadesi Geçen', deger: 15000, kritik: true, hedef: { route: '/tahsilat', query: {} } }
]

const kritikOgeler = [
  { anahtar: 'vadesiGecen', etiket: 'Vadesi Geçen Fatura', seviye: 'KRITIK', tutar: 15000, adet: 3, hedef: { route: '/tahsilat', query: {} } },
  { anahtar: 'kritikStok', etiket: 'Kritik Stok', seviye: 'WARN', tutar: null, adet: 5, hedef: { route: '/stoklar', query: { kritik: '1' } } },
  { anahtar: 'likiditeTrendi', etiket: 'Likidite Trendi', seviye: 'BILGI', tutar: 4.2, adet: null, hedef: { route: '/kasa', query: {} } }
]

describe('kokpit üst bandı', () => {
  it('globalFiltreDonemOlayiYayar', async () => {
    const wrapper = mount(KokpitGlobalFiltre, {
      ...mountOptions,
      props: { donem: 'BU_AY', paraBirimi: 'TRY', canli: true }
    })

    expect(wrapper.text()).toContain('Finans Masası')
    await wrapper.find('[data-test="donem-GECEN_AY"]').trigger('click')
    expect(wrapper.emitted('update:donem')[0]).toEqual(['GECEN_AY'])

    await wrapper.find('[data-test="para-birimi"]').setValue('USD')
    expect(wrapper.emitted('update:paraBirimi')[0]).toEqual(['USD'])

    await wrapper.find('[data-test="canli-durum"]').trigger('click')
    expect(wrapper.emitted('canli-degistir')).toHaveLength(1)

    await wrapper.find('[data-test="kokpit-ozellestir"]').trigger('click')
    expect(wrapper.emitted('ozellestir')).toHaveLength(1)
  })

  it('kpiBandiSekizMetrikGosterir', () => {
    const wrapper = mount(FinansOzetBandi, {
      ...mountOptions,
      props: { kpi: kpiListesi, paraBirimi: 'TRY' }
    })

    expect(wrapper.findAllComponents(MetrikKutu)).toHaveLength(8)
    expect(wrapper.find('[data-test="kpi-netNakit"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kpi-marj"]').text()).toContain('26,6%')
  })

  it('kpiKartiHedefLinkiTasiyor', () => {
    const wrapper = mount(FinansOzetBandi, {
      ...mountOptions,
      props: { kpi: kpiListesi, paraBirimi: 'TRY' }
    })

    const alacak = wrapper.find('[data-test="kpi-alacak"]')
    expect(alacak.attributes('data-to')).toContain('/cari-hesaplar')
    expect(alacak.attributes('data-to')).toContain('alacak')
  })

  it('kritikSeridSeviyeyeGoreRenkVeIkonTasiyor', () => {
    const wrapper = mount(KritikDurumSeridi, {
      ...mountOptions,
      props: { ogeler: kritikOgeler, paraBirimi: 'TRY' }
    })

    const kritik = wrapper.find('[data-test="kritik-oge-vadesiGecen"]')
    expect(kritik.classes()).toContain('kritik-serit-oge--kritik')
    expect(kritik.find('.kritik-serit-ikon').classes()).toContain('pi-exclamation-circle')
    expect(kritik.text()).toContain('15.000,00')
    expect(kritik.text()).toContain('3 fatura')

    const uyari = wrapper.find('[data-test="kritik-oge-kritikStok"]')
    expect(uyari.classes()).toContain('kritik-serit-oge--uyari')
    expect(uyari.find('.kritik-serit-ikon').classes()).toContain('pi-exclamation-triangle')
  })

  it('kritikOgeSecimOlayiYayar', async () => {
    const wrapper = mount(KritikDurumSeridi, {
      ...mountOptions,
      props: { ogeler: kritikOgeler, paraBirimi: 'TRY' }
    })

    await wrapper.find('[data-test="kritik-oge-vadesiGecen"]').trigger('click')
    expect(wrapper.emitted('oge-sec')[0][0].anahtar).toBe('vadesiGecen')
    expect(wrapper.emitted('oge-sec')[0][0].hedef.route).toBe('/tahsilat')
  })

  it('kritikSeritBosDurumuGosterir', () => {
    const wrapper = mount(KritikDurumSeridi, {
      ...mountOptions,
      props: { ogeler: [], paraBirimi: 'TRY' }
    })

    expect(wrapper.find('[data-test="kritik-serit-bos"]').exists()).toBe(true)
  })
})
