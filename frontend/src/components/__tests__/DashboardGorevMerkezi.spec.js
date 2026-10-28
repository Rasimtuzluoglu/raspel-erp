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

  it('her satır dört parçadan oluşur; boş sol/orta kolon yoktur', () => {
    const wrapper = mountEt({ gorevler })
    const satirlar = wrapper.findAll('.gorev-satir')
    expect(satirlar).toHaveLength(2)
    for (const satir of satirlar) {
      expect(satir.element.children).toHaveLength(4)
    }
    // Eski kart sarmalayıcısı ve ekstra grid kolonu kaldırıldı.
    expect(wrapper.find('.gorev-kart').exists()).toBe(false)
    expect(wrapper.find('.gorev-bos-kolon').exists()).toBe(false)
    expect(wrapper.find('.gorev-satir').element.children[0].className).toContain('gorev-ikon')
    expect(wrapper.find('.gorev-satir').element.children[1].className).toContain('gorev-icerik')
  })

  it('adet, tutar ve öncelik rengini gösterir', () => {
    const wrapper = mountEt({ gorevler })
    const ilk = wrapper.findAll('.gorev-satir')[0]
    expect(ilk.classes()).toContain('gorev-satir--kritik')
    expect(ilk.text()).toContain('4')
    expect(ilk.text()).toContain('12.500,00')
    expect(ilk.find('.gorev-adet').text()).toBe('4')
  })

  it('adet 0 olan görevleri gizler', () => {
    const wrapper = mountEt({ gorevler: [{ ...gorevler[0], adet: 0 }] })
    expect(wrapper.findAll('.gorev-satir')).toHaveLength(0)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevBos'))
  })

  it('boş durumda yeşil tik ile "Bekleyen iş yok" gösterir', () => {
    const wrapper = mountEt({ gorevler: [] })
    const bos = wrapper.find('.gorev-durum--bos')
    expect(bos.exists()).toBe(true)
    expect(bos.find('.pi-check-circle').exists()).toBe(true)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevBos'))
  })

  it('hata durumunda "Yeniden dene" butonu yayar', async () => {
    const wrapper = mountEt({ gorevler: [], hata: 'boom' })
    expect(wrapper.find('.gorev-durum--hata').exists()).toBe(true)
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevHata'))
    await wrapper.find('.gorev-dene').trigger('click')
    expect(wrapper.emitted('yenile')).toHaveLength(1)
  })

  it('yüklenirken iskelet gösterir', () => {
    const wrapper = mountEt({ gorevler: [], yukleniyor: true })
    expect(wrapper.find('.gorev-iskelet').exists()).toBe(true)
    expect(wrapper.findAll('.gorev-iskelet-satir')).toHaveLength(4)
  })

  it('en fazla 6 satır gösterir', () => {
    const cok = Array.from({ length: 10 }, (_, i) => ({
      ...gorevler[0],
      tur: `ONAY_${i}`,
      baslik: `Görev ${i}`,
      oncelik: 100 - i
    }))
    const wrapper = mountEt({ gorevler: cok })
    expect(wrapper.findAll('.gorev-satir')).toHaveLength(6)
  })

  it('baslik yoksa tür için çeviri anahtarını kullanır', () => {
    const wrapper = mountEt({ gorevler: [{ ...gorevler[1], baslik: '' }] })
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.gorevTuruVadesiGecen'))
  })

  it('vade kayıtlarını her zaman /tahsilat aksiyonuyla bağlar', () => {
    const wrapper = mountEt({
      gorevler: [{ ...gorevler[1], tur: 'VADESI_YAKLASAN', route: '/baska-yer' }]
    })
    const satir = wrapper.find('[data-test="gorev-VADESI_YAKLASAN"]')
    const eylem = satir.find('.gorev-eylem')
    expect(eylem.attributes('href')).toBe('/tahsilat')
    expect(eylem.text()).toContain(i18n.global.t('dashboard.gorevTahsilatLink'))
  })

  it('onay, kritik stok ve teslimat aksiyonlarını doğru rotalara bağlar', () => {
    const wrapper = mountEt({
      gorevler: [
        { ...gorevler[0], tur: 'ONAY_MASRAF', baslik: 'Masraf' },
        { ...gorevler[0], tur: 'KRITIK_STOK', baslik: 'Kritik' },
        { ...gorevler[0], tur: 'BEKLEYEN_TESLIMAT', baslik: 'Teslimat' }
      ]
    })
    expect(wrapper.find('[data-test="gorev-ONAY_MASRAF"] .gorev-eylem').attributes('href')).toBe(
      '/onaylar?tur=MASRAF'
    )
    expect(wrapper.find('[data-test="gorev-KRITIK_STOK"] .gorev-eylem').attributes('href')).toBe(
      '/stoklar?kritik=1'
    )
    expect(wrapper.find('[data-test="gorev-BEKLEYEN_TESLIMAT"] .gorev-eylem').attributes('href')).toBe(
      '/teslimatlar'
    )
  })
})
