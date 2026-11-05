import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import i18n from '../../i18n.js'
import DashboardUstBar from '../dashboard/DashboardUstBar.vue'

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

describe('DashboardUstBar — kişisel notlar', () => {
  it('not butonu rozet göstermez ve notları açar', async () => {
    const wrapper = mountEt()
    const buton = wrapper.find('[data-test="not-buton"]')

    expect(buton.attributes('aria-label')).toBe(i18n.global.t('dashboard.notlarAc'))
    expect(wrapper.find('[data-test="not-rozet"]').exists()).toBe(false)

    await buton.trigger('click')
    expect(wrapper.emitted('notlar')).toHaveLength(1)
    wrapper.unmount()
  })

  it('mobil menüdeki not satırı da notları açar', async () => {
    const wrapper = mountEt()
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await nextTick()

    const satir = wrapper.find('[data-test="mobil-aksiyon-notlar"]')
    expect(satir.text()).toContain(i18n.global.t('dashboard.notlar'))
    expect(satir.find('.ust-mobil-rozet').exists()).toBe(false)

    await satir.trigger('click')
    expect(wrapper.emitted('notlar')).toHaveLength(1)
    wrapper.unmount()
  })
})

describe('DashboardUstBar — canli gostergesi', () => {
  it('bagliyken yesil "Canli" gostergesi sunar', () => {
    const wrapper = mountEt({ canli: true, bagli: true })
    const durum = wrapper.find('[data-test="canli-durum"]')
    expect(durum.exists()).toBe(true)
    expect(durum.text()).toBe(i18n.global.t('dashboard.canli'))
    expect(durum.classes()).not.toContain('ust-canli--bekliyor')
    wrapper.unmount()
  })

  it('kopukken amber "Yeniden baglaniyor" gostergesi sunar', () => {
    const wrapper = mountEt({ canli: true, bagli: false })
    const durum = wrapper.find('[data-test="canli-durum"]')
    expect(durum.text()).toBe(i18n.global.t('dashboard.yenidenBaglaniyor'))
    expect(durum.classes()).toContain('ust-canli--bekliyor')
    wrapper.unmount()
  })

  it('canli mod kapaliyken cevrimdisi gostergesi sunar', () => {
    const wrapper = mountEt({ canli: false, bagli: false })
    const durum = wrapper.find('[data-test="canli-durum"]')
    expect(durum.text()).toBe(i18n.global.t('dashboard.cevrimdisiVeri'))
    expect(durum.classes()).toContain('ust-canli--kapali')
    wrapper.unmount()
  })
})

describe('DashboardUstBar — mobil "Daha fazla" menüsü', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
  })
  afterEach(() => {
    vi.restoreAllMocks()
    localStorage.clear()
    document.body.style.overflow = ''
  })

  it('tetikleyici aria durumunu yansıtır ve menüyü açıp kapatır', async () => {
    const wrapper = mountEt()
    const daha = wrapper.find('[data-test="mobil-daha"]')

    expect(daha.exists()).toBe(true)
    expect(daha.attributes('aria-haspopup')).toBe('dialog')
    expect(daha.attributes('aria-expanded')).toBe('false')
    expect(daha.attributes('aria-controls')).toBe('db-mobil-menu')
    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(false)

    await daha.trigger('click')
    expect(daha.attributes('aria-expanded')).toBe('true')
    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(true)

    await daha.trigger('click')
    expect(daha.attributes('aria-expanded')).toBe('false')
    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('mobil satırları 44px hedefli eylemleri listeler', async () => {
    const wrapper = mountEt()
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')

    const satirlar = [
      'mobil-aksiyon-notlar',
      'mobil-aksiyon-takvim',
      'mobil-aksiyon-kar',
      'mobil-aksiyon-excel',
      'mobil-aksiyon-yazdir',
      'mobil-aksiyon-canli',
      'mobil-aksiyon-yenile',
      'mobil-aksiyon-duzenle'
    ]
    for (const anahtar of satirlar) {
      expect(wrapper.find(`[data-test="${anahtar}"]`).exists(), anahtar).toBe(true)
    }
    expect(wrapper.find('[data-test="mobil-aksiyon-canli"]').attributes('role')).toBe('switch')
    wrapper.unmount()
  })

  it('Notlar ve Vade Takvimi satırları aynı emit\'leri tetikler ve menüyü kapatır', async () => {
    const wrapper = mountEt()
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await wrapper.find('[data-test="mobil-aksiyon-notlar"]').trigger('click')
    expect(wrapper.emitted('notlar')).toHaveLength(1)
    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(false)

    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await wrapper.find('[data-test="mobil-aksiyon-takvim"]').trigger('click')
    expect(wrapper.emitted('takvim')).toHaveLength(1)

    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await wrapper.find('[data-test="mobil-aksiyon-kar"]').trigger('click')
    expect(wrapper.emitted('kar-analizi')).toHaveLength(1)
    wrapper.unmount()
  })

  it('Excel satırı seçili dönemle dışa aktarır', async () => {
    excelMock.dashboard.mockResolvedValue({ data: new Blob(['xlsx']) })
    const tikla = vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
    const wrapper = mountEt({ donem: 'BU_YIL' })

    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await wrapper.find('[data-test="mobil-aksiyon-excel"]').trigger('click')
    await flushPromises()

    expect(excelMock.dashboard).toHaveBeenCalledWith({ donem: 'BU_YIL' })
    expect(wrapper.emitted('excel-indirildi')).toHaveLength(1)
    expect(tikla).toHaveBeenCalled()
    wrapper.unmount()
  })

  it('Yazdır satırı window.print çağırır', async () => {
    const yazdir = vi.spyOn(window, 'print').mockImplementation(() => {})
    const wrapper = mountEt()

    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await wrapper.find('[data-test="mobil-aksiyon-yazdir"]').trigger('click')

    expect(yazdir).toHaveBeenCalledTimes(1)
    wrapper.unmount()
  })

  it('Canlı mod anahtarı durumu yansıtır ve değişimi iletir', async () => {
    const wrapper = mountEt({ otoYenile: true })
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')

    const anahtar = wrapper.find('[data-test="mobil-aksiyon-canli"]')
    expect(anahtar.attributes('aria-checked')).toBe('true')
    expect(anahtar.attributes('aria-label')).toBe(i18n.global.t('dashboard.otoYenilemeKapat'))

    await anahtar.trigger('click')
    expect(wrapper.emitted('oto-degistir')).toEqual([[false]])
    // Anahtar menüyü kapatmaz: kullanıcı durumu yerinde görür.
    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(true)
    wrapper.unmount()

    const kapali = mountEt({ otoYenile: false })
    await kapali.find('[data-test="mobil-daha"]').trigger('click')
    expect(kapali.find('[data-test="mobil-aksiyon-canli"]').attributes('aria-checked')).toBe('false')
    kapali.unmount()
  })

  it('Yenile ve Düzenle satırları emit eder, düzen durumu yansıtılır', async () => {
    const wrapper = mountEt({ duzenModu: true })
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')

    const duzenle = wrapper.find('[data-test="mobil-aksiyon-duzenle"]')
    expect(duzenle.attributes('aria-pressed')).toBe('true')
    expect(duzenle.text()).toContain(i18n.global.t('dashboard.duzenBitir'))

    await duzenle.trigger('click')
    expect(wrapper.emitted('duzenle')).toHaveLength(1)

    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    await wrapper.find('[data-test="mobil-aksiyon-yenile"]').trigger('click')
    expect(wrapper.emitted('yenile')).toHaveLength(1)
    wrapper.unmount()
  })

  it('açılınca odağı ilk satıra taşır, Escape kapatıp odağı tetikleyiciye döndürür', async () => {
    const wrapper = mountEt({}, { attachTo: document.body })
    const daha = wrapper.find('[data-test="mobil-daha"]')
    await daha.trigger('click')
    await nextTick()
    await nextTick()

    expect(document.activeElement?.dataset?.test).toBe('mobil-aksiyon-notlar')

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    await nextTick()

    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(false)
    expect(document.activeElement).toBe(daha.element)
    wrapper.unmount()
  })

  it('dışarı tıklama menüyü kapatır', async () => {
    const wrapper = mountEt({}, { attachTo: document.body })
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(true)

    document.body.dispatchEvent(new MouseEvent('click', { bubbles: true }))
    await nextTick()

    expect(wrapper.find('[data-test="mobil-menu"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('masaüstü butonları DOM\'da korunur (CSS ile gizlenir)', () => {
    const wrapper = mountEt()
    const masaustu = [
      'not-buton',
      'excel-indir',
      'yazdir',
      'kar-analizi-buton',
      'vade-takvimi-buton',
      'oto-yenileme',
      'bildirim-zili',
      'donem-secici'
    ]
    for (const anahtar of masaustu) {
      expect(wrapper.find(`[data-test="${anahtar}"]`).exists(), anahtar).toBe(true)
    }
    expect(wrapper.find('.ust-buton--ara').exists()).toBe(true)
    wrapper.unmount()
  })

  it('kaldırılınca mobil belge dinleyicileri temizlenir', async () => {
    const kaldir = vi.spyOn(document, 'removeEventListener')
    const wrapper = mountEt()
    await wrapper.find('[data-test="mobil-daha"]').trigger('click')
    wrapper.unmount()

    expect(kaldir).toHaveBeenCalledWith('click', expect.any(Function), true)
    expect(kaldir).toHaveBeenCalledWith('keydown', expect.any(Function))
    kaldir.mockRestore()
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
