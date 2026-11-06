import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KarMarjBolumu from '../dashboard/kokpit/KarMarjBolumu.vue'
import FaturaSiparisBolumu from '../dashboard/kokpit/FaturaSiparisBolumu.vue'

const mountOptions = {
  global: { plugins: [i18n] }
}

const kalemler = (onEk) => Array.from({ length: 6 }, (_, index) => ({
  id: index + 1,
  ad: `${onEk} ${index + 1}`,
  ciro: 1000 - index * 50,
  maliyet: 600,
  kar: 400 - index * 20,
  marj: 40 - index
}))

const karVeri = {
  ciro: 100000,
  maliyet: 70000,
  kar: 30000,
  marj: 30,
  maliyetsizKalem: 3,
  trend: [
    { ay: '2026-10', ciro: 90000, maliyet: 65000, kar: 25000, marj: 27.8 },
    { ay: '2026-11', ciro: 100000, maliyet: 70000, kar: 30000, marj: 30 }
  ],
  urunler: kalemler('Ürün'),
  cariler: kalemler('Cari'),
  hedef: { route: '/raporlar', query: {} }
}

const faturaVeri = {
  durumlar: [
    { anahtar: 'KESILDI', etiket: 'Kesildi', adet: 10, tutar: 10000 },
    { anahtar: 'VADESI_GECEN', etiket: 'Vadesi Geçen', adet: 3, tutar: 1500 }
  ],
  vadesiGecenTutar: 1500,
  vadesiGecenAdet: 3,
  vadesiGecenFaturalar: Array.from({ length: 10 }, (_, index) => ({
    faturaId: index + 1,
    faturaNumarasi: `FTR-${index + 1}`,
    cariAd: `Cari ${index + 1}`,
    vadeTarihi: '2026-10-01',
    kalanTutar: 1000,
    gecikmeGun: 10
  })),
  tahsilatOrani: 70,
  siparisDurumlari: [
    { anahtar: 'TEKLIF', etiket: 'Teklif', adet: 4, tutar: null },
    { anahtar: 'TESLIM_EDILDI', etiket: 'Teslim edildi', adet: 2, tutar: null }
  ],
  hedef: { route: '/tahsilat', query: {} }
}

describe('kâr/marj ve fatura/sipariş bölümleri', () => {
  it('karBolumuTrendVeIlkBesGosterir', () => {
    const wrapper = mount(KarMarjBolumu, {
      ...mountOptions,
      props: { veri: karVeri, paraBirimi: 'TRY' }
    })

    expect(wrapper.find('[data-test="kar-trend-grafik"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kar-marj-grafik"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-test="kar-urunler-tablo"] tbody tr')).toHaveLength(5)
    expect(wrapper.findAll('[data-test="kar-cariler-tablo"] tbody tr')).toHaveLength(5)
    expect(wrapper.text()).toContain('100.000,00')
    expect(wrapper.text()).toContain('30%')
  })

  it('karBolumuYetkisizRoldeBosDurumGosterir', () => {
    const wrapper = mount(KarMarjBolumu, {
      ...mountOptions,
      props: { veri: null, paraBirimi: 'TRY' }
    })

    expect(wrapper.find('[data-test="kar-yetkisiz"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('gizli')
  })

  it('maliyetsizKalemUyarisiGosterilir', () => {
    const wrapper = mount(KarMarjBolumu, {
      ...mountOptions,
      props: { veri: karVeri, paraBirimi: 'TRY' }
    })

    const uyari = wrapper.find('[data-test="kar-maliyetsiz-uyari"]')
    expect(uyari.exists()).toBe(true)
    expect(uyari.text()).toContain('3')
  })

  it('faturaBolumuPipelineKovalariniGosterir', () => {
    const wrapper = mount(FaturaSiparisBolumu, {
      ...mountOptions,
      props: { veri: faturaVeri, paraBirimi: 'TRY' }
    })

    const pipeline = wrapper.find('[data-test="fatura-pipeline"]')
    expect(pipeline.exists()).toBe(true)
    expect(pipeline.text()).toContain('Kesildi')
    expect(pipeline.text()).toContain('Vadesi Geçen')
    expect(wrapper.find('[data-test="siparis-pipeline"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('70%')
    expect(wrapper.text()).toContain('3 fatura')
  })

  it('faturaBolumuVadesiGecenIlkSekizGosterir', () => {
    const wrapper = mount(FaturaSiparisBolumu, {
      ...mountOptions,
      props: { veri: faturaVeri, paraBirimi: 'TRY' }
    })

    const satirlar = wrapper.findAll('[data-test="vadesi-gecen-tablo"] tbody tr')
    expect(satirlar).toHaveLength(8)
    expect(wrapper.text()).toContain('FTR-1')
    expect(wrapper.text()).not.toContain('FTR-10')
  })

  it('bolumDetayOlayiHedefTasiyor', async () => {
    const wrapper = mount(FaturaSiparisBolumu, {
      ...mountOptions,
      props: { veri: faturaVeri, paraBirimi: 'TRY' }
    })

    await wrapper.find('[data-test="fatura-detay"]').trigger('click')
    expect(wrapper.emitted('detay')[0][0].route).toBe('/tahsilat')

    await wrapper.find('[data-test="pipeline-kova-KESILDI"]').trigger('click')
    expect(wrapper.emitted('detay')[1][0]).toEqual({ route: '/faturalar', query: { durum: 'KESILDI' } })

    await wrapper.find('[data-test="vadesi-gecen-tablo"] tbody tr').trigger('click')
    expect(wrapper.emitted('detay')[2][0].query.no).toBe('FTR-1')
  })
})
