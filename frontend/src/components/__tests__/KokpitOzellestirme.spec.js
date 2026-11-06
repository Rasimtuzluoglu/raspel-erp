import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitOzellestirme from '../dashboard/kokpit/KokpitOzellestirme.vue'

const BOLUMLER = ['nakit', 'alacakVerecek', 'karMarj', 'faturaSiparis']

const mountPanel = (props = {}) => mount(KokpitOzellestirme, {
  global: { plugins: [i18n] },
  props: {
    bolumler: BOLUMLER,
    gorunurBolumler: [...BOLUMLER],
    kaydediliyor: false,
    ...props
  }
})

describe('KokpitOzellestirme', () => {
  it('bolumGorunurluguDegistirir', async () => {
    const wrapper = mountPanel()

    await wrapper.find('[data-test="ozellestirme-gorunurluk-karMarj"]').setValue(false)

    expect(wrapper.emitted('gorunurluk-degistir')[0]).toEqual(['karMarj', false])
  })

  it('bolumSirasiniGunceller', async () => {
    const wrapper = mountPanel()

    await wrapper.find('[data-test="ozellestirme-asagi-nakit"]').trigger('click')

    expect(wrapper.emitted('sira-degistir')[0][0]).toEqual([
      'alacakVerecek',
      'nakit',
      'karMarj',
      'faturaSiparis'
    ])
  })

  it('sifirlaVarsayilanDuzeneDoner', async () => {
    const wrapper = mountPanel({ gorunurBolumler: ['nakit', 'faturaSiparis'] })

    await wrapper.find('[data-test="ozellestirme-sifirla"]').trigger('click')

    expect(wrapper.emitted('sifirla')).toHaveLength(1)
  })

  it('enAzBirBolumKalmali', () => {
    const wrapper = mountPanel({ gorunurBolumler: ['nakit'] })

    const onay = wrapper.find('[data-test="ozellestirme-gorunurluk-nakit"]')
    expect(onay.attributes('disabled')).toBeDefined()
    expect(wrapper.find('[data-test="ozellestirme-en-az-bir"]').exists()).toBe(true)
  })

  it('kaydetButonuOlayYayar', async () => {
    const wrapper = mountPanel()

    await wrapper.find('[data-test="ozellestirme-kaydet"]').trigger('click')

    expect(wrapper.emitted('kaydet')).toHaveLength(1)
  })

  it('klavyeIlePanelKapanir', () => {
    const wrapper = mountPanel()

    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))

    expect(wrapper.emitted('kapat')).toHaveLength(1)
  })
})
