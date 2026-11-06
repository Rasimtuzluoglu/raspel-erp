import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import YaslanmaKovalari from '../dashboard/kokpit/YaslanmaKovalari.vue'
import PipelineKovalari from '../dashboard/kokpit/PipelineKovalari.vue'

const yaslanma = [
  { anahtar: 'VADEDI_GELMEMIS', etiket: 'Vadesi Gelmemiş', tutar: 1000, adet: null },
  { anahtar: 'GUN_0_30', etiket: '0-30 Gün', tutar: 500, adet: null },
  { anahtar: 'GUN_31_60', etiket: '31-60 Gün', tutar: 0, adet: null },
  { anahtar: 'GUN_61_90', etiket: '61-90 Gün', tutar: 250, adet: null },
  { anahtar: 'GUN_90_PLUS', etiket: '90+ Gün', tutar: 100, adet: null }
]

const pipeline = [
  { anahtar: 'KESILDI', etiket: 'Kesildi', adet: 10, tutar: 10000 },
  { anahtar: 'VADESI_GECEN', etiket: 'Vadesi Geçen', adet: 0, tutar: 0 },
  { anahtar: 'TAHSIL_EDILDI', etiket: 'Tahsil Edildi', adet: 4, tutar: 4000 }
]

describe('kova görselleri', () => {
  it('yaslanmaKovalariEtiketVeTutarGosterir', () => {
    const wrapper = mount(YaslanmaKovalari, { props: { kovalar: yaslanma } })

    expect(wrapper.text()).toContain('Vadesi Gelmemiş')
    expect(wrapper.text()).toContain('1.000,00')
    expect(wrapper.text()).toContain('0-30 Gün')
    expect(wrapper.findAll('.kovalar-bar-segment')).toHaveLength(5)
  })

  it('pipelineKovalariSifirAdetteSolukGosterir', () => {
    const wrapper = mount(PipelineKovalari, { props: { kovalar: pipeline } })

    const sifir = wrapper.find('[data-test="pipeline-kova-VADESI_GECEN"]')
    expect(sifir.classes()).toContain('pipeline-kova--sifir')
    expect(sifir.text()).toContain('0')
    expect(wrapper.text()).toContain('10')
    expect(wrapper.text()).toContain('4.000,00')
  })

  it('kovaSecimOlayiYayilir', async () => {
    const yaslanmaWrapper = mount(YaslanmaKovalari, { props: { kovalar: yaslanma } })
    await yaslanmaWrapper.find('[data-test="yaslanma-kova-GUN_0_30"]').trigger('click')
    expect(yaslanmaWrapper.emitted('kova-sec')[0][0].anahtar).toBe('GUN_0_30')

    const pipelineWrapper = mount(PipelineKovalari, { props: { kovalar: pipeline } })
    await pipelineWrapper.find('[data-test="pipeline-kova-KESILDI"]').trigger('click')
    expect(pipelineWrapper.emitted('kova-sec')[0][0].anahtar).toBe('KESILDI')
  })

  it('bosKovaListesindeBosDurumGosterilir', () => {
    const yaslanmaWrapper = mount(YaslanmaKovalari, { props: { kovalar: [], bosMetin: 'Yaşlandırma yok' } })
    expect(yaslanmaWrapper.find('[data-test="kova-bos"]').text()).toContain('Yaşlandırma yok')

    const pipelineWrapper = mount(PipelineKovalari, { props: { kovalar: [], bosMetin: 'Pipeline yok' } })
    expect(pipelineWrapper.find('[data-test="pipeline-bos"]').text()).toContain('Pipeline yok')
  })
})
