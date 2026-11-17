import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import SahaKuyrukCubugu from '../saha/SahaKuyrukCubugu.vue'

const kayit = (tur, ek = {}) => ({
  id: `${tur}-${Math.random()}`,
  clientKey: `k-${tur}`,
  tur,
  payload: {},
  dosyalar: [],
  durum: 'BEKLIYOR',
  deneme: 0,
  sonHata: null,
  olusturma: '2026-11-17T10:00:00Z',
  ...ek
})

const kur = (kuyruk, props = {}) => mount(SahaKuyrukCubugu, {
  global: { plugins: [i18n] },
  props: { kuyruk, senkronizeEdiliyor: false, ...props }
})

describe('SahaKuyrukCubugu', () => {
  it('kuyruk varken sayaci ve tur kirilimini gosterir', () => {
    const wrapper = kur([kayit('TESLIMAT'), kayit('TESLIMAT'), kayit('TAHSILAT')])

    const metin = wrapper.get('[data-test="saha-kuyruk-cubugu"]').text()
    expect(metin).toMatch(/3/)
    expect(metin).toMatch(/2\s*teslimat/i)
    expect(metin).toMatch(/1\s*tahsilat/i)
  })

  it('gonder tiklaninca gonder olayini yayar', async () => {
    const wrapper = kur([kayit('ZIYARET')])

    await wrapper.get('[data-test="saha-kuyruk-gonder"]').trigger('click')

    expect(wrapper.emitted('gonder')).toHaveLength(1)
  })

  it('kuyruk bossa gorunmez', () => {
    const wrapper = kur([])

    expect(wrapper.find('[data-test="saha-kuyruk-cubugu"]').exists()).toBe(false)
  })

  it('hata varsa uyari ve son hata ipucunu gosterir', () => {
    const wrapper = kur([
      kayit('TESLIMAT', { durum: 'HATA', deneme: 1, sonHata: 'Gecersiz imza' }),
      kayit('ZIYARET')
    ])

    const metin = wrapper.get('[data-test="saha-kuyruk-cubugu"]').text()
    expect(metin).toMatch(/1/)
    expect(metin).toContain('Gecersiz imza')
  })

  it('gonderim sirasinda gonder dugmesini kilitler', () => {
    const wrapper = kur([kayit('TESLIMAT')], { senkronizeEdiliyor: true })

    const dugme = wrapper.get('[data-test="saha-kuyruk-gonder"]')
    expect(dugme.attributes('disabled')).toBeDefined()
    expect(dugme.attributes('aria-busy')).toBe('true')
  })

  it('kayit silme istegi icin sil olayini yayar', async () => {
    const k = kayit('MASRAF', { durum: 'HATA' })
    const wrapper = kur([k])

    const silDugmeleri = wrapper.findAll('[data-test="saha-kuyruk-sil"]')
    expect(silDugmeleri).toHaveLength(1)
    await silDugmeleri[0].trigger('click')

    expect(wrapper.emitted('sil')[0]).toEqual([k.id])
  })
})
