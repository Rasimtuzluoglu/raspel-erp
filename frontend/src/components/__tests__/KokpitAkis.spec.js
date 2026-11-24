import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitAkis from '../dashboard/kokpit/KokpitAkis.vue'

const olay = (overrides = {}) => ({
  tur: 'TAHSILAT',
  baslik: 'Tahsilat: Müşteri A',
  aciklama: 'FTR-1',
  tutar: 1500,
  tarih: new Date().toISOString(),
  route: '/hareketler',
  ikon: 'pi pi-wallet',
  renk: '#10b981',
  ...overrides
})

const mountAkis = (props = {}) => mount(KokpitAkis, {
  global: { plugins: [i18n] },
  props: {
    olaylar: [],
    yukleniyor: false,
    hata: null,
    paraBirimi: 'TRY',
    ...props
  }
})

describe('KokpitAkis', () => {
  it('olaylari tutar ve goreli zamanla listeler', () => {
    const wrapper = mountAkis({ olaylar: [olay(), olay({ tur: 'FATURA', baslik: 'Fatura kesildi', tutar: 200 })] })

    const satirlar = wrapper.findAll('[data-test^="akis-oge-"]')
    expect(satirlar).toHaveLength(2)
    expect(satirlar[0].text()).toContain('Tahsilat: Müşteri A')
    expect(satirlar[0].text()).toContain('1.500')
    expect(satirlar[0].text()).toContain('az önce')
  })

  it('satira tiklaninca sec olayini yayar', async () => {
    const wrapper = mountAkis({ olaylar: [olay()] })

    await wrapper.find('[data-test="akis-oge-0"]').trigger('click')

    expect(wrapper.emitted('sec')).toHaveLength(1)
    expect(wrapper.emitted('sec')[0][0].tur).toBe('TAHSILAT')
  })

  it('bos durumda mesaj gosterir', () => {
    const wrapper = mountAkis()

    expect(wrapper.find('[data-test="kokpit-akis-bos"]').exists()).toBe(true)
  })

  it('hata durumunda yeniden dene olayi yayar', async () => {
    const wrapper = mountAkis({ hata: 'akis kapali' })

    const hata = wrapper.find('[data-test="kokpit-akis-hata"]')
    expect(hata.exists()).toBe(true)
    await hata.find('button').trigger('click')

    expect(wrapper.emitted('yenile')).toHaveLength(1)
  })

  it('yuklenirken iskelet gosterir', () => {
    const wrapper = mountAkis({ yukleniyor: true })

    expect(wrapper.find('[data-test="kokpit-akis-bos"]').exists()).toBe(false)
    expect(wrapper.findAll('.akis-iskelet').length).toBeGreaterThan(0)
  })

  it('para birimi degisince tutari o birimde bicimler', () => {
    const wrapper = mountAkis({ olaylar: [olay({ tutar: 100 })], paraBirimi: 'USD' })

    expect(wrapper.find('[data-test="akis-oge-0"]').text()).toContain('$')
  })

  it('olaylari gune gore gruplar ve global indeksi korur', () => {
    const bugun = new Date()
    const dun = new Date(Date.now() - 86400000)
    const eski = new Date(Date.now() - 10 * 86400000)
    const wrapper = mountAkis({
      olaylar: [
        olay({ tarih: bugun.toISOString() }),
        olay({ tarih: dun.toISOString(), tur: 'FATURA' }),
        olay({ tarih: eski.toISOString(), tur: 'ODEME' })
      ]
    })
    expect(wrapper.find('[data-test="akis-grup-BUGUN"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="akis-grup-DUN"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="akis-grup-DAHA_ESKI"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="akis-oge-2"]').exists()).toBe(true)
  })

  it('grup sirasini sabit tutar ve gecersiz tarihi daha eskiye atar', () => {
    const bugun = new Date().toISOString()
    const dun = new Date(Date.now() - 86400000).toISOString()
    const wrapper = mountAkis({
      olaylar: [
        olay({ tarih: null, tur: 'ODEME' }),
        olay({ tarih: dun, tur: 'FATURA' }),
        olay({ tarih: bugun })
      ]
    })

    const gruplar = wrapper.findAll('[data-test^="akis-grup-"]').map((grup) => grup.attributes('data-test'))
    expect(gruplar).toEqual(['akis-grup-BUGUN', 'akis-grup-DUN', 'akis-grup-DAHA_ESKI'])
    // Global indeks giriş sırasını korur: geçersiz tarih 0, bugünkü olay 2.
    expect(wrapper.find('[data-test="akis-grup-DAHA_ESKI"] [data-test="akis-oge-0"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="akis-grup-BUGUN"] [data-test="akis-oge-2"]').exists()).toBe(true)
  })

  it('tur filtresi gruplari yalnizca filtrelenmis olaylardan kurar', async () => {
    const bugun = new Date().toISOString()
    const dun = new Date(Date.now() - 86400000).toISOString()
    const wrapper = mountAkis({
      olaylar: [
        olay({ tarih: bugun }),
        olay({ tarih: dun, tur: 'FATURA' })
      ]
    })

    await wrapper.find('[data-test="akis-filtre-FATURA"]').trigger('click')

    expect(wrapper.find('[data-test="akis-grup-BUGUN"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="akis-grup-DUN"]').exists()).toBe(true)
  })
})
