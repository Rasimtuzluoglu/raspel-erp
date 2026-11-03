import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import DashboardUstBar from '../dashboard/DashboardUstBar.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

const mountEt = (props = {}, options = {}) =>
  mount(DashboardUstBar, {
    props: {
      kullaniciAd: 'Ayşe Yılmaz',
      sirketAd: 'RasPel A.Ş.',
      gorevSayilari: { onayBekleyen: 2, vadesiGecen: 1, kritikStok: 3 },
      ...props
    },
    ...options,
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('DashboardUstBar — dönem seçici', () => {
  it('dört dönem seçeneğini sunar ve seçimi üst bileşene iletir', async () => {
    const wrapper = mountEt({ donem: 'BU_AY' })
    const secim = wrapper.find('[data-test="donem-secici"]')

    expect(secim.exists()).toBe(true)
    expect(secim.attributes('aria-label')).toBe(i18n.global.t('dashboard.donem'))
    const secenekler = secim.findAll('option')
    expect(secenekler).toHaveLength(4)
    expect(secenekler.map((o) => o.attributes('value'))).toEqual([
      'BU_AY',
      'GECEN_AY',
      'BU_CEYREK',
      'BU_YIL'
    ])
    expect(secenekler.map((o) => o.text())).toEqual([
      i18n.global.t('dashboard.donemBuAy'),
      i18n.global.t('dashboard.donemGecenAy'),
      i18n.global.t('dashboard.donemBuCeyrek'),
      i18n.global.t('dashboard.donemBuYil')
    ])
    expect(secim.element.value).toBe('BU_AY')

    await secim.setValue('BU_YIL')
    expect(wrapper.emitted('donem-degistir')).toEqual([['BU_YIL']])
    wrapper.unmount()
  })
})

describe('DashboardUstBar — bildirim zili', () => {
  it('toplam rozeti görev sayılarının toplamını gösterir', () => {
    const wrapper = mountEt()
    const rozet = wrapper.find('[data-test="bildirim-rozet"]')
    expect(rozet.exists()).toBe(true)
    expect(rozet.text()).toBe('6')
    expect(wrapper.find('[data-test="bildirim-zili"]').attributes('aria-label')).toContain('6')
    wrapper.unmount()
  })

  it('sayaçlar sıfırsa rozet gösterilmez', () => {
    const wrapper = mountEt({ gorevSayilari: { onayBekleyen: 0, vadesiGecen: 0, kritikStok: 0 } })
    expect(wrapper.find('[data-test="bildirim-rozet"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('zil tıklanınca ilgili ekranlara bağlantılı menüyü açar', async () => {
    const wrapper = mountEt()
    const zil = wrapper.find('[data-test="bildirim-zili"]')
    expect(zil.attributes('aria-expanded')).toBe('false')
    expect(zil.attributes('aria-haspopup')).toBe('true')
    expect(zil.attributes('aria-controls')).toBe('db-bildirim-menusu')

    await zil.trigger('click')
    const menu = wrapper.find('[data-test="bildirim-menusu"]')
    expect(menu.exists()).toBe(true)
    expect(zil.attributes('aria-expanded')).toBe('true')
    expect(menu.attributes('role')).toBe('menu')

    expect(wrapper.find('[data-test="bildirim-onayBekleyen"]').attributes('href')).toBe('/onaylar')
    expect(wrapper.find('[data-test="bildirim-vadesiGecen"]').attributes('href')).toBe('/tahsilat')
    expect(wrapper.find('[data-test="bildirim-kritikStok"]').attributes('href')).toBe('/stoklar?kritik=1')
    expect(wrapper.find('[data-test="bildirim-onayBekleyen"]').text()).toContain(
      i18n.global.t('dashboard.bildirimOnayBekleyen')
    )
    expect(wrapper.find('[data-test="bildirim-kritikStok"] .ust-zil-oge-adet').text()).toBe('3')
    wrapper.unmount()
  })

  it('Escape menüyü kapatır ve odağı zile döndürür', async () => {
    const wrapper = mountEt({}, { attachTo: document.body })
    const zil = wrapper.find('[data-test="bildirim-zili"]')
    await zil.trigger('click')
    expect(wrapper.find('[data-test="bildirim-menusu"]').exists()).toBe(true)

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await nextTick()

    expect(wrapper.find('[data-test="bildirim-menusu"]').exists()).toBe(false)
    expect(zil.attributes('aria-expanded')).toBe('false')
    wrapper.unmount()
  })

  it('dışarı tıklama menüyü kapatır', async () => {
    const wrapper = mountEt({}, { attachTo: document.body })
    await wrapper.find('[data-test="bildirim-zili"]').trigger('click')
    expect(wrapper.find('[data-test="bildirim-menusu"]').exists()).toBe(true)

    document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()

    expect(wrapper.find('[data-test="bildirim-menusu"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('kaldırılınca belge dinleyicileri temizlenir', async () => {
    const kaldir = vi.spyOn(document, 'removeEventListener')
    const wrapper = mountEt()
    await wrapper.find('[data-test="bildirim-zili"]').trigger('click')
    wrapper.unmount()
    expect(kaldir).toHaveBeenCalledWith('click', expect.any(Function), true)
    expect(kaldir).toHaveBeenCalledWith('keydown', expect.any(Function))
    kaldir.mockRestore()
  })
})

describe('DashboardUstBar — takvim ve otomatik yenileme', () => {
  it('takvim ikonu takvim olayını yayar', async () => {
    const wrapper = mountEt()
    const buton = wrapper.find('[data-test="vade-takvimi-buton"]')
    expect(buton.attributes('aria-label')).toBe(i18n.global.t('dashboard.vadeTakvimiAc'))
    await buton.trigger('click')
    expect(wrapper.emitted('takvim')).toHaveLength(1)
    wrapper.unmount()
  })

  it('otomatik yenileme anahtarı durumu yansıtır ve değişimi iletir', async () => {
    const wrapper = mountEt({ otoYenile: false })
    const anahtar = wrapper.find('[data-test="oto-yenileme"]')
    expect(anahtar.attributes('role')).toBe('switch')
    expect(anahtar.attributes('aria-checked')).toBe('false')
    expect(anahtar.attributes('aria-label')).toBe(i18n.global.t('dashboard.otoYenilemeAc'))

    await anahtar.trigger('click')
    expect(wrapper.emitted('oto-degistir')).toEqual([[true]])
    wrapper.unmount()

    const acik = mountEt({ otoYenile: true })
    const acikAnahtar = acik.find('[data-test="oto-yenileme"]')
    expect(acikAnahtar.attributes('aria-checked')).toBe('true')
    expect(acikAnahtar.attributes('aria-label')).toBe(i18n.global.t('dashboard.otoYenilemeKapat'))
    await acikAnahtar.trigger('click')
    expect(acik.emitted('oto-degistir')).toEqual([[false]])
    acik.unmount()
  })
})
