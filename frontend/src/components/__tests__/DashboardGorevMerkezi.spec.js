import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import DashboardGorevMerkezi from '../dashboard/DashboardGorevMerkezi.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const mountEt = (props = {}) =>
  mount(DashboardGorevMerkezi, {
    props,
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

const gorevler = [
  {
    tur: 'ONAY_IZIN',
    baslik: 'İzin talebi',
    aciklama: '2 personel izin bekliyor',
    adet: 2,
    tutar: 0,
    oncelik: 30,
    route: '/izinler',
    ikon: 'pi pi-calendar',
    renk: '#3b82f6'
  },
  {
    tur: 'VADESI_GECEN',
    baslik: 'Vadesi geçen',
    aciklama: 'Gecikmiş alacaklar',
    adet: 4,
    tutar: 12500,
    oncelik: 90,
    route: '/tahsilat',
    ikon: 'pi pi-exclamation-triangle',
    renk: '#ef4444'
  }
]

describe('DashboardGorevMerkezi', () => {
  it('görevleri öncelik sırasına göre listeler', () => {
    const wrapper = mountEt({ gorevler })
    const basliklar = wrapper.findAll('.gorev-baslik').map((b) => b.text())
    expect(basliklar).toEqual(['Vadesi geçen', 'İzin talebi'])
  })

  it('adet, tutar ve öncelik rengini gösterir', () => {
    const wrapper = mountEt({ gorevler })
    const ilk = wrapper.findAll('.gorev-kart')[0]
    expect(ilk.classes()).toContain('gorev-kart--kritik')
    expect(ilk.text()).toContain('4')
    expect(ilk.text()).toContain('12.500,00')
    expect(ilk.attributes('href')).toBe('/tahsilat')
  })

  it('adet 0 olan görevleri gizler', () => {
    const wrapper = mountEt({ gorevler: [{ ...gorevler[0], adet: 0 }] })
    expect(wrapper.findAll('.gorev-kart')).toHaveLength(0)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevBos'))
  })

  it('boş durumda bilgilendirme gösterir', () => {
    const wrapper = mountEt({ gorevler: [] })
    expect(wrapper.find('.gorev-durum--bos').exists()).toBe(true)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevBos'))
  })

  it('hata durumunda hata mesajı gösterir', () => {
    const wrapper = mountEt({ gorevler: [], hata: 'boom' })
    expect(wrapper.find('.gorev-durum--hata').exists()).toBe(true)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevHata'))
  })

  it('yüklenirken iskelet gösterir', () => {
    const wrapper = mountEt({ gorevler: [], yukleniyor: true })
    expect(wrapper.find('.gorev-iskelet').exists()).toBe(true)
    expect(wrapper.findAll('.gorev-iskelet-satir')).toHaveLength(3)
  })

  it('baslik yoksa tür için çeviri anahtarını kullanır', () => {
    const wrapper = mountEt({ gorevler: [{ ...gorevler[1], baslik: '' }] })
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevTuruVadesiGecen'))
  })

  it('vade kayıtlarını her zaman /tahsilat satırı olarak bağlar', () => {
    const wrapper = mountEt({
      gorevler: [{ ...gorevler[1], tur: 'VADESI_YAKLASAN', route: '/baska-yer' }]
    })
    const satir = wrapper.find('[data-test="gorev-VADESI_YAKLASAN"]')
    expect(satir.attributes('href')).toBe('/tahsilat')
    // Tahsilat linki satırın kendisidir; ayrı liste/kart yok.
    expect(wrapper.findAll('.gorev-kart')).toHaveLength(1)
    expect(satir.find('.gorev-link').text()).toContain(i18n.global.t('dashboard.gorevTahsilatLink'))
  })
})
