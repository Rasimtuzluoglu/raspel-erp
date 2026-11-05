import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import DashboardKisayolSeridi from '../dashboard/DashboardKisayolSeridi.vue'
import {
  KISAYOL_DEPOLAMA_ANAHTARI,
  VARSAYILAN_KISAYOLLAR
} from '../../utils/dashboardKisayollar.js'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const mountEt = (props = {}) =>
  mount(DashboardKisayolSeridi, {
    props,
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('DashboardKisayolSeridi — 8 işlem', () => {
  it('tek satırlık nav olarak 8 kısayol render eder', () => {
    const wrapper = mountEt()
    expect(wrapper.element.tagName).toBe('NAV')
    expect(wrapper.classes()).toContain('kisayol-serit')
    expect(wrapper.attributes('aria-label')).toBe(i18n.global.t('dashboard.kisayolSeridi'))
    expect(wrapper.findAll('.kisayol')).toHaveLength(8)
  })

  it('yalnız mevcut rotalara bağlanır', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('a').map((a) => a.attributes('href'))).toEqual([
      '/faturalar',
      '/hareketler',
      '/hareketler',
      '/hizli-satis',
      '/cari-hesaplar',
      '/kasa',
      '/stoklar',
      '/teklifler'
    ])
  })

  it('her kısayolda ikon ve çeviri etiketi vardır', () => {
    const wrapper = mountEt()
    const kisayollar = wrapper.findAll('.kisayol')
    const beklenen = [
      ['yeniFatura', 'dashboard.kisayolYeniFatura'],
      ['tahsilat', 'dashboard.kisayolTahsilat'],
      ['odeme', 'dashboard.kisayolOdeme'],
      ['hizliSatis', 'dashboard.kisayolHizliSatis'],
      ['yeniCari', 'dashboard.kisayolYeniCari'],
      ['kasa', 'dashboard.kisayolKasa'],
      ['stoklar', 'dashboard.kisayolStoklar'],
      ['teklifler', 'dashboard.kisayolTeklifler']
    ]
    beklenen.forEach(([key, anahtar], i) => {
      expect(kisayollar[i].attributes('data-kisayol')).toBe(key)
      expect(kisayollar[i].find('.kisayol-etiket').text()).toBe(i18n.global.t(anahtar))
      expect(kisayollar[i].find('.kisayol-ikon i').exists()).toBe(true)
    })
  })
})

describe('DashboardKisayolSeridi — özel seçim ve varsayılan', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('cihazdaki seçimi katalog sırasıyla render eder', () => {
    localStorage.setItem(KISAYOL_DEPOLAMA_ANAHTARI, JSON.stringify(['raporlar', 'kasa']))
    const wrapper = mountEt()

    const kisayollar = wrapper.findAll('.kisayol')
    expect(kisayollar).toHaveLength(2)
    expect(kisayollar.map((k) => k.attributes('data-kisayol'))).toEqual(['kasa', 'raporlar'])
    expect(kisayollar.map((k) => k.attributes('href'))).toEqual(['/kasa', '/raporlar'])
    wrapper.unmount()
  })

  it('seçim yoksa varsayılan 8 kısayola düşer', () => {
    const wrapper = mountEt()
    expect(wrapper.findAll('.kisayol')).toHaveLength(8)
    wrapper.unmount()
  })

  it('bozuk kayıtta varsayılan 8 kısayola düşer', () => {
    localStorage.setItem(KISAYOL_DEPOLAMA_ANAHTARI, '{bozuk json')
    const wrapper = mountEt()
    expect(wrapper.findAll('.kisayol')).toHaveLength(8)
    wrapper.unmount()

    localStorage.setItem(KISAYOL_DEPOLAMA_ANAHTARI, JSON.stringify(['bilinmeyen']))
    const ikinci = mountEt()
    expect(ikinci.findAll('.kisayol')).toHaveLength(8)
    ikinci.unmount()
  })

  it('ADMIN olmayan kullanıcıda yetkisiz kayıtlar süzülür', () => {
    localStorage.setItem(
      KISAYOL_DEPOLAMA_ANAHTARI,
      JSON.stringify(['kasa', 'kokpit', 'yedekler'])
    )
    const wrapper = mountEt({ isAdmin: false })
    expect(wrapper.findAll('.kisayol').map((k) => k.attributes('data-kisayol'))).toEqual(['kasa'])
    wrapper.unmount()

    const admin = mountEt({ isAdmin: true })
    expect(admin.findAll('.kisayol').map((k) => k.attributes('data-kisayol'))).toEqual([
      'kasa',
      'kokpit',
      'yedekler'
    ])
    admin.unmount()
  })

  it('Düzenle butonu modalı açar ve kaydet şeridi günceller', async () => {
    const wrapper = mountEt()
    expect(wrapper.find('[data-test="kisayol-duzenle"]').exists()).toBe(true)

    await wrapper.find('[data-test="kisayol-duzenle"]').trigger('click')
    await nextTick()
    expect(wrapper.find('[data-test="kisayol-ayarlari"]').exists()).toBe(true)

    await wrapper.find('[data-test="kisayol-secim-teklifler"]').setValue(false)
    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')
    await nextTick()

    expect(wrapper.find('[data-test="kisayol-ayarlari"]').exists()).toBe(false)
    expect(wrapper.findAll('.kisayol')).toHaveLength(7)
    expect(wrapper.findAll('.kisayol').map((k) => k.attributes('data-kisayol'))).toEqual(
      VARSAYILAN_KISAYOLLAR.filter((k) => k !== 'teklifler')
    )
    expect(JSON.parse(localStorage.getItem(KISAYOL_DEPOLAMA_ANAHTARI))).not.toContain('teklifler')
    expect(wrapper.find('[role="status"]').text()).toBe(
      i18n.global.t('dashboard.kisayolKaydedildi')
    )
    wrapper.unmount()
  })
})
