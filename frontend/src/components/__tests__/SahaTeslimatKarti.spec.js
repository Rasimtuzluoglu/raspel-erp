import { describe, it, expect, vi, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import SahaTeslimatKarti from '../saha/SahaTeslimatKarti.vue'

const teslimat = (ek = {}) => ({
  id: 5,
  faturaNumarasi: 'FTR-2026-0005',
  teslimatAdresi: 'Atatürk Cad. No:5 Kadıköy / İstanbul',
  musteriAdi: 'Yılmaz Ticaret',
  musteriTelefon: '05001112233',
  durum: 'YOLDA',
  gecikti: false,
  ...ek
})

const kur = (props = {}) => mount(SahaTeslimatKarti, {
  global: { plugins: [i18n] },
  props: { teslimat: teslimat(), sira: 1, ilkMi: false, ...props }
})

afterEach(() => {
  vi.restoreAllMocks()
})

describe('SahaTeslimatKarti', () => {
  it('durum, adres ve musteri bilgisini gosterir', () => {
    const wrapper = kur()

    const metin = wrapper.get('[data-test="saha-teslimat-karti"]').text()
    expect(metin).toContain('Yolda')
    expect(metin).toContain('Atatürk Cad. No:5')
    expect(metin).toContain('Yılmaz Ticaret')
    expect(wrapper.find('[data-test="saha-teslimat-imza"]').exists()).toBe(true)
  })

  it('adres varsa navigasyon dogru url ile acilir', async () => {
    const ac = vi.spyOn(window, 'open').mockImplementation(() => null)
    const wrapper = kur()

    await wrapper.get('[data-test="saha-navigasyon"]').trigger('click')

    expect(ac).toHaveBeenCalledWith(
      `https://www.google.com/maps/dir/?api=1&destination=${encodeURIComponent('Atatürk Cad. No:5 Kadıköy / İstanbul')}`,
      '_blank'
    )
  })

  it('adres yoksa navigasyon gizlenir', () => {
    const wrapper = kur({ teslimat: teslimat({ teslimatAdresi: '' }) })

    expect(wrapper.find('[data-test="saha-navigasyon"]').exists()).toBe(false)
  })

  it('imza istegini teslimat ile yayar', async () => {
    const wrapper = kur()
    const t = wrapper.props('teslimat')

    await wrapper.get('[data-test="saha-teslimat-imza"]').trigger('click')

    expect(wrapper.emitted('imza')[0]).toEqual([t])
  })

  it('telefon varsa tel: baglantisi ve whatsapp sunar', () => {
    const wrapper = kur()

    const tel = wrapper.get('[data-test="saha-teslimat-telefon"]')
    expect(tel.attributes('href')).toBe('tel:05001112233')
    expect(wrapper.get('[data-test="saha-teslimat-whatsapp"]').attributes('href'))
      .toContain('https://wa.me/905001112233')
  })

  it('telefon yoksa arama aksiyonlari gizlenir', () => {
    const wrapper = kur({ teslimat: teslimat({ musteriTelefon: null }) })

    expect(wrapper.find('[data-test="saha-teslimat-telefon"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="saha-teslimat-whatsapp"]').exists()).toBe(false)
  })

  it('ilk kartta siradaki rozetini gosterir', () => {
    const wrapper = kur({ ilkMi: true })

    expect(wrapper.get('[data-test="saha-teslimat-siradaki"]').text()).toContain('Sıradaki')
  })

  it('teslim edilmis teslimatta imza dugmesini kapatir', () => {
    const wrapper = kur({ teslimat: teslimat({ durum: 'TESLIM_EDILDI' }) })

    expect(wrapper.get('[data-test="saha-teslimat-imza"]').attributes('disabled')).toBeDefined()
  })
})
