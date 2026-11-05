import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import DashboardUstBar from '../dashboard/DashboardUstBar.vue'
import { NOTLAR_OLAYI, notlarAnahtari } from '../../utils/dashboardNotlar.js'

const { excelMock } = vi.hoisted(() => ({ excelMock: { dashboard: vi.fn() } }))

vi.mock('../../api/index.js', () => ({ excelAPI: excelMock }))

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

  it('kâr analizi ikonu kar-analizi olayını yayar', async () => {
    const wrapper = mountEt()
    const buton = wrapper.find('[data-test="kar-analizi-buton"]')
    expect(buton.exists()).toBe(true)
    expect(buton.attributes('aria-label')).toBe(i18n.global.t('dashboard.karAnaliziAc'))
    expect(buton.attributes('title')).toBe(i18n.global.t('dashboard.karAnaliziAc'))
    await buton.trigger('click')
    expect(wrapper.emitted('kar-analizi')).toHaveLength(1)
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

describe('DashboardUstBar — kişisel not rozeti', () => {
  beforeEach(() => {
    localStorage.clear()
  })
  afterEach(() => {
    localStorage.clear()
  })

  it('açık not sayısını rozet olarak gösterir ve notları açar', async () => {
    localStorage.setItem(
      notlarAnahtari(5),
      JSON.stringify([
        { id: '1', metin: 'Açık not', tamam: false, tarih: '2026-11-05T08:00:00.000Z' },
        { id: '2', metin: 'İkinci açık', tamam: false, tarih: '2026-11-05T09:00:00.000Z' },
        { id: '3', metin: 'Bitti', tamam: true, tarih: '2026-11-05T10:00:00.000Z' }
      ])
    )

    const wrapper = mountEt({ kullaniciId: 5 })
    const rozet = wrapper.find('[data-test="not-rozet"]')
    expect(rozet.exists()).toBe(true)
    expect(rozet.text()).toBe('2')
    expect(wrapper.find('[data-test="not-buton"]').attributes('aria-label')).toContain('2')

    await wrapper.find('[data-test="not-buton"]').trigger('click')
    expect(wrapper.emitted('notlar')).toHaveLength(1)
    wrapper.unmount()
  })

  it('not yoksa rozet göstermez', () => {
    const wrapper = mountEt({ kullaniciId: 5 })
    expect(wrapper.find('[data-test="not-rozet"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('not güncelleme olayında rozeti tazeler, başka kullanıcıyı yok sayar', async () => {
    const wrapper = mountEt({ kullaniciId: 5 })

    localStorage.setItem(
      notlarAnahtari(5),
      JSON.stringify([{ id: '1', metin: 'Yeni', tamam: false, tarih: '' }])
    )
    window.dispatchEvent(new CustomEvent(NOTLAR_OLAYI, { detail: { kullaniciId: 5 } }))
    await nextTick()
    expect(wrapper.find('[data-test="not-rozet"]').text()).toBe('1')

    localStorage.setItem(
      notlarAnahtari(5),
      JSON.stringify([
        { id: '1', metin: 'Yeni', tamam: false, tarih: '' },
        { id: '2', metin: 'Bir tane daha', tamam: false, tarih: '' }
      ])
    )
    window.dispatchEvent(new CustomEvent(NOTLAR_OLAYI, { detail: { kullaniciId: 99 } }))
    await nextTick()
    expect(wrapper.find('[data-test="not-rozet"]').text()).toBe('1')
    wrapper.unmount()
  })
})

describe('DashboardUstBar — Excel ve yazdırma', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('seçili dönemle Excel dışa aktarma çağrısı yapar', async () => {
    excelMock.dashboard.mockResolvedValue({ data: new Blob(['xlsx']) })
    const tikla = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = mountEt({ donem: 'GECEN_AY' })

    await wrapper.find('[data-test="excel-indir"]').trigger('click')
    await flushPromises()

    expect(excelMock.dashboard).toHaveBeenCalledTimes(1)
    expect(excelMock.dashboard).toHaveBeenCalledWith({ donem: 'GECEN_AY' })
    expect(wrapper.emitted('excel-indirildi')).toHaveLength(1)
    expect(wrapper.find('[data-test="excel-indir"]').attributes('disabled')).toBeUndefined()
    expect(tikla).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('Excel hatasında çağrıyı tamamlar ve butonu yeniden etkinleştirir', async () => {
    excelMock.dashboard.mockRejectedValue(new Error('offline'))
    const wrapper = mountEt()

    await wrapper.find('[data-test="excel-indir"]').trigger('click')
    await flushPromises()

    expect(excelMock.dashboard).toHaveBeenCalledTimes(1)
    expect(wrapper.emitted('excel-indirildi')).toBeUndefined()
    expect(wrapper.find('[data-test="excel-indir"]').attributes('disabled')).toBeUndefined()
    wrapper.unmount()
  })

  it('yazdır ikonu window.print çağırır', async () => {
    const yazdir = vi.spyOn(window, 'print').mockImplementation(() => {})
    const wrapper = mountEt()

    await wrapper.find('[data-test="yazdir"]').trigger('click')

    expect(yazdir).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })
})
