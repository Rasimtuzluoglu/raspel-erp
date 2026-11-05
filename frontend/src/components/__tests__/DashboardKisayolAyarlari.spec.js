import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import DashboardKisayolAyarlari from '../dashboard/DashboardKisayolAyarlari.vue'
import {
  KISAYOL_DEPOLAMA_ANAHTARI,
  MAX_KISAYOL,
  VARSAYILAN_KISAYOLLAR,
  kisayolKatalogu
} from '../../utils/dashboardKisayollar.js'

const mountEt = (props = {}, options = {}) =>
  mount(DashboardKisayolAyarlari, {
    props: { visible: true, isAdmin: true, secim: [...VARSAYILAN_KISAYOLLAR], ...props },
    global: { plugins: [i18n] },
    ...options
  })

describe('DashboardKisayolAyarlari — seçim ve rol kısıtı', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('mevcut seçimi işaretli gösterir ve katalog sırasını korur', () => {
    const wrapper = mountEt({ secim: ['raporlar', 'kasa'] })
    expect(wrapper.find('[data-test="kisayol-secim-kasa"]').element.checked).toBe(true)
    expect(wrapper.find('[data-test="kisayol-secim-raporlar"]').element.checked).toBe(true)
    expect(wrapper.find('[data-test="kisayol-secim-stoklar"]').element.checked).toBe(false)
    expect(wrapper.find('[data-test="kisayol-ayar-sayac"]').text()).toContain('2')
    wrapper.unmount()
  })

  it('ADMIN olmayan kullanıcı kokpit ve yedekleri görmez', () => {
    const wrapper = mountEt({ isAdmin: false })
    expect(wrapper.find('[data-test="kisayol-secim-kokpit"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="kisayol-secim-yedekler"]').exists()).toBe(false)
    // Yetkisiz anahtarlar secimde olsa bile listeye girmez.
    expect(wrapper.find('[data-test="kisayol-secim-raporlar"]').exists()).toBe(true)
    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(
      kisayolKatalogu(false).length
    )
    wrapper.unmount()
  })

  it('ADMIN tüm kataloğu görür ve yönetici rozetini gösterir', () => {
    const wrapper = mountEt({ isAdmin: true })
    expect(wrapper.find('[data-test="kisayol-secim-kokpit"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kisayol-secim-yedekler"]').exists()).toBe(true)
    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(
      kisayolKatalogu(true).length
    )
    expect(wrapper.text()).toContain(i18n.global.t('dashboard.kisayolAdmin'))
    wrapper.unmount()
  })

  it('en fazla 8 seçim yapılmasına izin verir', async () => {
    const wrapper = mountEt()
    const secili = wrapper.findAll('input[type="checkbox"]').filter((i) => i.element.checked)
    expect(secili).toHaveLength(MAX_KISAYOL)

    const satinalma = wrapper.find('[data-test="kisayol-secim-satinalma"]')
    expect(satinalma.element.checked).toBe(false)
    expect(satinalma.attributes('disabled')).toBeDefined()

    // 8 seciliyken yeni secim eklenmez. Disabled alani testte gecici olarak acilir.
    satinalma.element.disabled = false
    await satinalma.setValue(true)
    await nextTick()
    expect(wrapper.find('[data-test="kisayol-ayar-hata"]').exists()).toBe(true)

    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')
    const gonderilen = wrapper.emitted('kaydet')[0][0]
    expect(gonderilen).toHaveLength(MAX_KISAYOL)
    expect(gonderilen).not.toContain('satinalma')
    wrapper.unmount()
  })
})

describe('DashboardKisayolAyarlari — kaydet ve sıfırla', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('seçimi katalog sırasıyla yayar ve cihaza yazar', async () => {
    const wrapper = mountEt({ secim: ['kasa', 'raporlar'] })
    // Sıra katalog sırasına çevrilir: kasa (5) raporlar (10) -> kasa, raporlar.
    expect(wrapper.find('[data-test="kisayol-ayar-sayac"]').text()).toContain('2')

    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')

    expect(wrapper.emitted('kaydet')).toEqual([[['kasa', 'raporlar']]])
    expect(JSON.parse(localStorage.getItem(KISAYOL_DEPOLAMA_ANAHTARI))).toEqual([
      'kasa',
      'raporlar'
    ])
    wrapper.unmount()
  })

  it('seçim değişikliğini kaydeder', async () => {
    const wrapper = mountEt({ secim: ['kasa'] })
    await wrapper.find('[data-test="kisayol-secim-raporlar"]').setValue(true)
    await wrapper.find('[data-test="kisayol-secim-kasa"]').setValue(false)
    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')

    expect(wrapper.emitted('kaydet')).toEqual([[['raporlar']]])
    expect(JSON.parse(localStorage.getItem(KISAYOL_DEPOLAMA_ANAHTARI))).toEqual(['raporlar'])
    wrapper.unmount()
  })

  it('sıfırla varsayılan 8 seçime döner', async () => {
    const wrapper = mountEt({ secim: ['kasa'] })
    expect(wrapper.find('[data-test="kisayol-secim-yeniFatura"]').element.checked).toBe(false)

    await wrapper.find('[data-test="kisayol-sifirla"]').trigger('click')
    await nextTick()

    expect(wrapper.emitted('sifirla')).toEqual([[VARSAYILAN_KISAYOLLAR]])
    const secili = wrapper.findAll('input[type="checkbox"]').filter((i) => i.element.checked)
    expect(secili).toHaveLength(VARSAYILAN_KISAYOLLAR.length)
    expect(wrapper.findAll('input[type="checkbox"]').map((i) => i.element.value)).toEqual(
      expect.arrayContaining(VARSAYILAN_KISAYOLLAR)
    )

    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')
    expect(wrapper.emitted('kaydet').at(-1)).toEqual([VARSAYILAN_KISAYOLLAR])
    expect(JSON.parse(localStorage.getItem(KISAYOL_DEPOLAMA_ANAHTARI))).toEqual(
      VARSAYILAN_KISAYOLLAR
    )
    wrapper.unmount()
  })

  it('boş seçimle kaydetmez', async () => {
    const wrapper = mountEt({ secim: ['kasa'] })
    // Tek secimi kaldir.
    await wrapper.find('[data-test="kisayol-secim-kasa"]').setValue(false)
    await wrapper.find('[data-test="kisayol-kaydet"]').trigger('click')

    expect(wrapper.emitted('kaydet')).toBeUndefined()
    expect(wrapper.find('[data-test="kisayol-ayar-hata"]').text()).toBe(
      i18n.global.t('dashboard.kisayolEnAzBir')
    )
    expect(localStorage.getItem(KISAYOL_DEPOLAMA_ANAHTARI)).toBeNull()
    wrapper.unmount()
  })

  it('Escape kapatma olayı yayar', async () => {
    const wrapper = mountEt()
    await wrapper.find('[data-test="kisayol-ayarlari"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    wrapper.unmount()
  })
})
