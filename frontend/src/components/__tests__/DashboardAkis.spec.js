import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardAkis from '../dashboard/DashboardAkis.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const mountEt = (props = {}) =>
  mount(DashboardAkis, {
    props,
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

const akis = [
  {
    tur: 'TAHSILAT',
    baslik: 'Tahsilat alındı',
    aciklama: 'ABC Ltd. — 5.000 TL',
    tutar: 5000,
    tarih: '2026-10-28T11:55:00',
    route: '/hareketler',
    ikon: null,
    renk: null
  },
  {
    tur: 'FATURA',
    baslik: 'Fatura kesildi',
    aciklama: 'FTR-2026-0001',
    tutar: 12000,
    tarih: '2026-10-28T09:00:00',
    route: null,
    ikon: null,
    renk: null
  }
]

describe('DashboardAkis', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date('2026-10-28T12:00:00'))
  })

  afterEach(() => {
    vi.useRealTimers()
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'visible' })
  })

  it('olayları tür ikonu, tutar ve göreli zamanla listeler', () => {
    const wrapper = mountEt({ akis })
    const satirlar = wrapper.findAll('.akis-satir')
    expect(satirlar).toHaveLength(2)
    expect(satirlar[0].find('.akis-nokta i').classes()).toContain('pi-arrow-down-left')
    expect(satirlar[1].find('.akis-nokta i').classes()).toContain('pi-file')
    expect(satirlar[0].text()).toContain('5 dk önce')
    expect(satirlar[0].text()).toContain('5.000,00')
    expect(satirlar[0].find('a').attributes('href')).toBe('/hareketler')
    // Rota yoksa bağlantı üretilmez.
    expect(satirlar[1].find('a').exists()).toBe(false)
  })

  it('60 saniyede bir sessiz yenileme olayı yayar (sayfa görünürken)', () => {
    const wrapper = mountEt({ akis })
    expect(wrapper.emitted('yenile')).toBeUndefined()
    vi.advanceTimersByTime(60000)
    expect(wrapper.emitted('yenile')).toHaveLength(1)
    wrapper.unmount()
  })

  it('sayfa görünmezken yenileme yapmaz', () => {
    Object.defineProperty(document, 'visibilityState', { configurable: true, value: 'hidden' })
    const wrapper = mountEt({ akis })
    vi.advanceTimersByTime(120000)
    expect(wrapper.emitted('yenile')).toBeUndefined()
    wrapper.unmount()
  })

  it('otomatikYenile kapalıysa zamanlayıcı kurmaz', () => {
    const wrapper = mountEt({ akis, otomatikYenile: false })
    vi.advanceTimersByTime(120000)
    expect(wrapper.emitted('yenile')).toBeUndefined()
    wrapper.unmount()
  })

  it('boş ve hata durumlarını gösterir', () => {
    const bos = mountEt({ akis: [] })
    expect(bos.text()).toContain(i18n.global.t('dashboard.akisBos'))
    const hata = mountEt({ akis: [], hata: 'x' })
    expect(hata.text()).toContain(i18n.global.t('dashboard.akisHata'))
  })

  it('yüklenirken iskelet gösterir', () => {
    const wrapper = mountEt({ akis: [], yukleniyor: true })
    expect(wrapper.find('.akis-iskelet').exists()).toBe(true)
  })
})
