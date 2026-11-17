import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import SahaOzetKartlari from '../saha/SahaOzetKartlari.vue'

const ozet = {
  teslimat: { bekleyen: 3, yolda: 1, bugunTeslimEdilen: 2, geciken: 1 },
  ziyaret: { bugun: 2, sonZiyaretTarihi: '2026-11-17T14:00:00' },
  talepler: { bekleyenIzin: 1, bekleyenMasraf: 2 }
}

const mountKartlar = (props = {}) => mount(SahaOzetKartlari, {
  global: { plugins: [i18n] },
  props: { ozet: null, yukleniyor: false, ...props }
})

describe('SahaOzetKartlari', () => {
  it('uc karti degerleriyle gosterir', () => {
    const wrapper = mountKartlar({ ozet })

    expect(wrapper.find('[data-test="saha-ozet-teslimat"]').text()).toContain('4')
    expect(wrapper.find('[data-test="saha-ozet-teslimat"]').text()).toContain('2')
    expect(wrapper.find('[data-test="saha-ozet-ziyaret"]').text()).toContain('2')
    expect(wrapper.find('[data-test="saha-ozet-talepler"]').text()).toContain('3')
  })

  it('karta tiklaninca sec olayini yayar', async () => {
    const wrapper = mountKartlar({ ozet })

    await wrapper.find('[data-test="saha-ozet-teslimat"]').trigger('click')
    await wrapper.find('[data-test="saha-ozet-ziyaret"]').trigger('click')
    await wrapper.find('[data-test="saha-ozet-talepler"]').trigger('click')

    expect(wrapper.emitted('sec')[0]).toEqual(['teslimat'])
    expect(wrapper.emitted('sec')[1]).toEqual(['ziyaret'])
    expect(wrapper.emitted('sec')[2]).toEqual(['talepler'])
  })

  it('yuklenirken iskelet gosterir', () => {
    const wrapper = mountKartlar({ yukleniyor: true })

    expect(wrapper.findAll('.saha-ozet-iskelet')).toHaveLength(3)
  })

  it('veri yoksa sifir gosterir', () => {
    const wrapper = mountKartlar()

    expect(wrapper.find('[data-test="saha-ozet-teslimat"]').text()).toContain('0')
    expect(wrapper.find('[data-test="saha-ozet-ziyaret"]').text()).toContain('0')
  })
})
