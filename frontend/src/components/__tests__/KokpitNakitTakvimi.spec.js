import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitNakitTakvimi from '../dashboard/kokpit/KokpitNakitTakvimi.vue'

const gunler = Array.from({ length: 16 }, (_, index) => {
  const gun = String(index + 1).padStart(2, '0')
  return {
    tarih: `2026-11-${gun}`,
    giris: index === 0 ? 5000 : 0,
    cikis: index === 0 ? 2000 : 100,
    net: index === 0 ? 3000 : -100,
    kumulatif: 100000 - index * 100
  }
})

const mountTakvim = (props = {}) => mount(KokpitNakitTakvimi, {
  global: { plugins: [i18n] },
  props: {
    projeksiyon: { gunluk: gunler },
    yukleniyor: false,
    paraBirimi: 'TRY',
    ...props
  }
})

describe('KokpitNakitTakvimi', () => {
  it('ilk on dort gunu ve ozeti gosterir', () => {
    const wrapper = mountTakvim()

    expect(wrapper.findAll('[data-test="nakit-takvim-gun"]')).toHaveLength(14)
    expect(wrapper.find('[data-test="nakit-takvim-ozet"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('14 Günlük Nakit Takvimi')
    expect(wrapper.text()).toContain('Giriş')
    expect(wrapper.text()).toContain('Çıkış')
  })

  it('en yogun gunu ozetler', () => {
    const wrapper = mountTakvim()

    expect(wrapper.text()).toContain('En yoğun gün')
    expect(wrapper.text()).toContain('01/11')
  })

  it('bos projeksiyonda bos durum gosterir', () => {
    const wrapper = mountTakvim({ projeksiyon: { gunluk: [] } })

    expect(wrapper.find('[data-test="nakit-takvim-bos"]').exists()).toBe(true)
    expect(wrapper.findAll('[data-test="nakit-takvim-gun"]')).toHaveLength(0)
  })

  it('takvim verisinden tahsilat ve odeme adetlerini gosterir', () => {
    const wrapper = mountTakvim({
      projeksiyon: null,
      takvim: {
        toplamGiris: 1000,
        toplamCikis: 500,
        net: 500,
        gunler: [
          {
            tarih: '2026-11-01',
            giris: 1000,
            cikis: 500,
            net: 500,
            kumulatif: 500,
            girisAdet: 2,
            cikisAdet: 1
          }
        ]
      }
    })

    expect(wrapper.findAll('[data-test="nakit-takvim-gun"]')).toHaveLength(1)
    expect(wrapper.text()).toContain('2 tahsilat')
    expect(wrapper.text()).toContain('1 ödeme')
  })

  it('yuklenirken iskelet gosterir', () => {
    const wrapper = mountTakvim({ projeksiyon: null, yukleniyor: true })

    expect(wrapper.find('.nakit-takvim-iskelet').exists()).toBe(true)
  })
})
