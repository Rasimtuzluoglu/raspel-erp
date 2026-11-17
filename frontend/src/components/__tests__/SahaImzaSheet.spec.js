import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import SahaImzaSheet from '../saha/SahaImzaSheet.vue'

const AppDialogStub = {
  name: 'AppDialog',
  props: ['visible', 'header'],
  emits: ['update:visible'],
  template: '<div v-if="visible" data-test="dialog"><slot /><slot name="footer" /></div>'
}

const ImzaPadStub = {
  name: 'ImzaPad',
  emits: ['imza-degisti'],
  template: '<button type="button" data-test="imza-ciz" @click="$emit(\'imza-degisti\', \'data:image/png;base64,IMZA\')">ciz</button>'
}

const teslimat = {
  id: 5,
  faturaNumarasi: 'FTR-2026-0005',
  musteriAdi: 'Yılmaz Ticaret',
  teslimatAdresi: 'Kadıköy / İstanbul',
  durum: 'YOLDA'
}

const kur = (props = {}) => mount(SahaImzaSheet, {
  global: {
    plugins: [i18n],
    stubs: { AppDialog: AppDialogStub, ImzaPad: ImzaPadStub }
  },
  props: {
    visible: true,
    teslimat,
    cevrimici: true,
    kaydediliyor: false,
    ...props
  }
})

const adGir = async (wrapper, deger = 'Ali Veli') => {
  await wrapper.get('[data-test="saha-imza-ad"]').setValue(deger)
}

const imzala = async (wrapper) => {
  await wrapper.get('[data-test="imza-ciz"]').trigger('click')
}

describe('SahaImzaSheet', () => {
  it('teslim alan adi ve imza olmadan gonderilemez', async () => {
    const wrapper = kur()
    const dugme = wrapper.get('[data-test="saha-imza-gonder"]')

    expect(dugme.attributes('disabled')).toBeDefined()

    await adGir(wrapper)
    expect(dugme.attributes('disabled')).toBeDefined()

    await wrapper.get('[data-test="saha-imza-ad"]').setValue('')
    await imzala(wrapper)
    expect(dugme.attributes('disabled')).toBeDefined()

    await adGir(wrapper)
    expect(dugme.attributes('disabled')).toBeUndefined()
  })

  it('cevrimici gonderimde govdeyi yayar', async () => {
    const wrapper = kur()

    await adGir(wrapper, 'Ali Veli')
    await imzala(wrapper)

    await wrapper.get('[data-test="saha-imza-gonder"]').trigger('click')

    const govde = wrapper.emitted('gonder')[0][0]
    expect(govde).toEqual({
      teslimAlanAd: 'Ali Veli',
      teslimNotu: '',
      imzaDataUrl: 'data:image/png;base64,IMZA',
      fotoDataUrl: null
    })
    expect(wrapper.emitted('kuyrukla')).toBeUndefined()
  })

  it('cevrimdisi kuyruga al dugmesi kuyrukla olayini yayar', async () => {
    const wrapper = kur({ cevrimici: false })

    expect(wrapper.find('[data-test="saha-imza-gonder"]').exists()).toBe(false)

    await adGir(wrapper, 'Ayşe Yılmaz')
    await imzala(wrapper)
    await wrapper.get('[data-test="saha-imza-kuyruk"]').trigger('click')

    const govde = wrapper.emitted('kuyrukla')[0][0]
    expect(govde.teslimAlanAd).toBe('Ayşe Yılmaz')
    expect(govde.imzaDataUrl).toBe('data:image/png;base64,IMZA')
    expect(wrapper.emitted('gonder')).toBeUndefined()
  })

  it('kapat dugmesi update:visible false yayar', async () => {
    const wrapper = kur()

    await wrapper.get('[data-test="saha-imza-kapat"]').trigger('click')

    expect(wrapper.emitted('update:visible')[0]).toEqual([false])
  })

  it('kaydediliyor iken gonder dugmesi kilitlenir ve aria-busy olur', () => {
    const wrapper = kur({ kaydediliyor: true })

    const dugme = wrapper.get('[data-test="saha-imza-gonder"]')
    expect(dugme.attributes('disabled')).toBeDefined()
    expect(dugme.attributes('aria-busy')).toBe('true')
  })

  it('gizli iken icerik render edilmez', () => {
    const wrapper = kur({ visible: false })

    expect(wrapper.find('[data-test="saha-imza-sheet"]').exists()).toBe(false)
  })
})
