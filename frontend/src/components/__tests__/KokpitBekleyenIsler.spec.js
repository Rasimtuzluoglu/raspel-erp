import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitBekleyenIsler from '../dashboard/kokpit/KokpitBekleyenIsler.vue'

const ogeler = [
  { anahtar: 'bekleyenOnay', etiket: 'Bekleyen Onay', seviye: 'WARN', adet: 3, tutar: null },
  { anahtar: 'vadesiGecen', etiket: 'Vadesi Geçen', seviye: 'KRITIK', adet: 2, tutar: 15000 },
  { anahtar: 'likidite', etiket: 'Likidite', seviye: 'BILGI', adet: null, tutar: 4.2 },
  { anahtar: 'kritikStok', etiket: 'Kritik Stok', seviye: 'WARN', adet: null, tutar: null }
]

const mountKart = (props = {}) => mount(KokpitBekleyenIsler, {
  global: { plugins: [i18n] },
  props: {
    ogeler: [],
    yukleniyor: false,
    paraBirimi: 'TRY',
    onayGorebilir: false,
    ...props
  }
})

describe('KokpitBekleyenIsler', () => {
  it('kalemleri adet/tutar ve seviye ile listeler', () => {
    const wrapper = mountKart({ ogeler })

    expect(wrapper.find('[data-test="bekleyen-oge-bekleyenOnay"]').text()).toContain('3')
    const vadesi = wrapper.find('[data-test="bekleyen-oge-vadesiGecen"]')
    // Adet varsa adet gösterilir (tutar değil); seviye rengi kritik olur.
    expect(vadesi.text()).toContain('2')
    expect(wrapper.find('[data-test="bekleyen-ikon-vadesiGecen"]').exists()).toBe(true)
    expect(wrapper.find('.bekleyen-ikon--kritik').exists()).toBe(true)
  })

  it('kaleme tiklaninca oge-sec yayar', async () => {
    const wrapper = mountKart({ ogeler })

    await wrapper.find('[data-test="bekleyen-oge-bekleyenOnay"]').trigger('click')

    expect(wrapper.emitted('oge-sec')).toHaveLength(1)
    expect(wrapper.emitted('oge-sec')[0][0].anahtar).toBe('bekleyenOnay')
  })

  it('bos durumda bilgi verir', () => {
    const wrapper = mountKart()

    expect(wrapper.find('[data-test="kokpit-bekleyen-bos"]').exists()).toBe(true)
  })

  it('uyari ve bilgi seviyelerini ve ekran okuyucu etiketini isaretler', () => {
    const wrapper = mountKart({ ogeler })

    expect(wrapper.find('[data-test="bekleyen-ikon-bekleyenOnay"]').classes())
      .toContain('bekleyen-ikon--uyari')
    expect(wrapper.find('[data-test="bekleyen-ikon-likidite"]').classes())
      .toContain('bekleyen-ikon--bilgi')
    expect(wrapper.find('[data-test="bekleyen-oge-bekleyenOnay"] .sr-only').text()).toBe('Uyarı')
    expect(wrapper.find('[data-test="bekleyen-oge-likidite"] .sr-only').text()).toBe('Bilgi')
    expect(wrapper.find('[data-test="bekleyen-oge-vadesiGecen"] .sr-only').text()).toBe('Kritik')
  })

  it('onay yetkisi varsa onay merkezi baglantisi cikar', async () => {
    const wrapper = mountKart({ ogeler, onayGorebilir: true })

    await wrapper.find('[data-test="bekleyen-onay-merkezi"]').trigger('click')
    expect(wrapper.emitted('onaylar')).toHaveLength(1)
  })

  it('onay yetkisi yoksa baglanti gizlenir', () => {
    const wrapper = mountKart({ ogeler, onayGorebilir: false })

    expect(wrapper.find('[data-test="bekleyen-onay-merkezi"]').exists()).toBe(false)
  })
})
