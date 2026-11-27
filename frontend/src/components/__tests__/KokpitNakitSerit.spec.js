import { describe, it, expect } from 'vitest'
import { mount, RouterLinkStub } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitNakitSerit from '../dashboard/kokpit/KokpitNakitSerit.vue'

const t = (anahtar) => i18n.global.t(anahtar)

const projeksiyon = (bitis = 140000) => ({
  baslangicBakiyesi: 120000,
  toplamBeklenenGiris: 5000,
  toplamBeklenenCikis: 3000,
  tahminiBitisBakiyesi: bitis,
  gunSayisi: 30,
  gunluk: [
    { tarih: '2026-11-27', kumulatif: 120000 },
    { tarih: '2026-11-28', kumulatif: 130000 },
    { tarih: '2026-11-29', kumulatif: bitis }
  ]
})

const mountSerit = (props = {}) => mount(KokpitNakitSerit, {
  global: {
    plugins: [i18n],
    stubs: { 'router-link': RouterLinkStub }
  },
  props: {
    projeksiyon: projeksiyon(),
    paraBirimi: 'TRY',
    ...props
  }
})

describe('KokpitNakitSerit', () => {
  it('baslik, deger, grafik ve detay linkini gosterir', () => {
    const wrapper = mountSerit()

    expect(wrapper.find('[data-test="kokpit-nakit-serit"]').exists()).toBe(true)
    expect(wrapper.find('.nakit-serit-baslik').text()).toBe(t('dashboard.kokpitNakitSeritBaslik'))
    expect(wrapper.find('.nakit-serit-deger').text()).toContain('140.000,00')
    expect(wrapper.find('[data-test="kokpit-nakit-serit-grafik"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-nakit-serit-detay"]').exists()).toBe(true)
    expect(wrapper.findComponent(RouterLinkStub).props('to')).toBe('/kasa')
  })

  it('guvenli bant icinde yesil tonu uygular', () => {
    const wrapper = mountSerit({ projeksiyon: projeksiyon(30000) })

    const durum = wrapper.find('.nakit-serit-durum')
    expect(durum.text()).toBe(t('dashboard.kokpitNakitSeritBantta'))
    expect(durum.classes()).toContain('nakit-serit-durum--iyi')
    expect(durum.find('.pi-check-circle').exists()).toBe(true)
  })

  it('guvenli bant ustunde yesil tonu uygular (risk degil)', () => {
    const wrapper = mountSerit({ projeksiyon: projeksiyon(80000) })

    const durum = wrapper.find('.nakit-serit-durum')
    expect(durum.text()).toBe(t('dashboard.kokpitNakitSeritUstunde'))
    expect(durum.classes()).toContain('nakit-serit-durum--iyi')
    expect(durum.find('.pi-check-circle').exists()).toBe(true)
  })

  it('sifirin altinda uyari tonunu uygular', () => {
    const wrapper = mountSerit({ projeksiyon: projeksiyon(-10000) })

    const durum = wrapper.find('.nakit-serit-durum')
    expect(durum.text()).toBe(t('dashboard.kokpitNakitSeritAltinda'))
    expect(durum.classes()).toContain('nakit-serit-durum--risk')
    expect(durum.find('.pi-exclamation-triangle').exists()).toBe(true)
  })

  it('para birimini degere yansitir', () => {
    const wrapper = mountSerit({ paraBirimi: 'USD' })

    expect(wrapper.find('.nakit-serit-deger').text()).toContain('$')
  })

  it('grafigi uc y ekseni tikiyle cizer', () => {
    const wrapper = mountSerit()
    const grafik = wrapper.find('[data-test="kokpit-nakit-serit-grafik"]')

    expect(grafik.attributes('aria-label')).toBe(t('dashboard.kokpitNakitSeritGrafikAria'))
    expect(grafik.findAll('.buyuk-grafik-y-etiket')).toHaveLength(3)
  })

  it('yetersiz projeksiyon verisinde render edilmez', () => {
    const wrapper = mountSerit({
      projeksiyon: { tahminiBitisBakiyesi: 1, gunluk: [{ kumulatif: 1 }] }
    })

    expect(wrapper.find('[data-test="kokpit-nakit-serit"]').exists()).toBe(false)
  })

  it('yuklenirken ayni yukseklikte iskelet gosterir', () => {
    const wrapper = mountSerit({ projeksiyon: null, yukleniyor: true })

    const serit = wrapper.find('[data-test="kokpit-nakit-serit"]')
    expect(serit.exists()).toBe(true)
    expect(serit.attributes('aria-busy')).toBe('true')
    expect(wrapper.find('.nakit-serit-iskelet').exists()).toBe(true)
    expect(wrapper.find('[data-test="kokpit-nakit-serit-grafik"]').exists()).toBe(false)
  })
})
