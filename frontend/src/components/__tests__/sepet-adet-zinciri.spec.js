import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import PosSepetPaneli from '../PosSepetPaneli.vue'
import PosAdetGirisi from '../PosAdetGirisi.vue'
import i18n from '../../i18n.js'

// DEBUG: gercek PosAdetGirisi (stub YOK) ile +/− zincirini dogrular.

const stubs = {
  Button: { name: 'Button', props: ['label', 'icon'], template: '<button><slot />{{ label }}</button>' },
  InputNumber: { template: '<input class="inputnumber" />' },
  SelectButton: { template: '<div class="selectbutton"><slot /></div>' },
  AutoComplete: { template: '<div class="autocomplete"><slot /></div>' },
  SatirEylemleri: { template: '<div />' }
}

const urun = (ek = {}) => ({
  id: 1,
  ad: 'Vida M8',
  stokKodu: 'V-001',
  barkod: '8690000000001',
  miktar: 2,
  fiyat: 10,
  fiyatlar: [{ ad: 'Perakende', fiyat: 10 }],
  fiyatTipi: 'Perakende',
  birim: 'adet',
  kdvOrani: 20,
  birimHacim: 0.01,
  agirlik: 0.05,
  stokMiktari: 50,
  ...ek
})

describe('DEBUG zincir: gercek PosAdetGirisi', () => {
  it('+ tiklamasi miktar-artir [0] yayar', async () => {
    const w = mount(PosSepetPaneli, {
      props: {
        sepet: [urun()],
        acik: true,
        indirimTipleri: [
          { label: 'Tutar', value: 'tutar' },
          { label: 'Yuzde', value: 'yuzde' }
        ]
      },
      global: { plugins: [i18n], stubs: { ...stubs, teleport: true } }
    })
    const girisi = w.findComponent(PosAdetGirisi)
    expect(girisi.exists()).toBe(true)
    await girisi.find('[aria-label="Miktarı artır"]').trigger('click')
    expect(w.emitted('miktar-artir')).toEqual([[0]])
  })

  it('− tiklamasi miktar-azalt [0] yayar', async () => {
    const w = mount(PosSepetPaneli, {
      props: {
        sepet: [urun({ miktar: 2 })],
        acik: true,
        indirimTipleri: [{ label: 'Tutar', value: 'tutar' }]
      },
      global: { plugins: [i18n], stubs: { ...stubs, teleport: true } }
    })
    const girisi = w.findComponent(PosAdetGirisi)
    await girisi.find('[aria-label="Miktarı azalt"]').trigger('click')
    expect(w.emitted('miktar-azalt')).toEqual([[0]])
  })
})
