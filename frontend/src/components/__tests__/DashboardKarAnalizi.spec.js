import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import { dashboardAPI } from '../../api/index.js'
import DashboardKarAnalizi from '../dashboard/DashboardKarAnalizi.vue'

vi.mock('../../api/index.js', () => ({
  dashboardAPI: { karAnalizi: vi.fn() }
}))

vi.mock('vue-chartjs', () => ({
  Bar: { name: 'Bar', props: ['data', 'options'], template: '<div class="stub-bar" />' }
}))

const urunler = (adet = 12) =>
  Array.from({ length: adet }, (_, i) => ({
    stokId: i + 1,
    ad: `Ürün ${i + 1}`,
    ciro: 100000 - i * 1000,
    maliyet: 70000 - i * 500,
    kar: 30000 - i * 500,
    marj: 30 - i
  }))

const cariler = [
  { cariId: 1, ad: 'ABC Ticaret Ltd.', ciro: 250000, maliyet: 170000, kar: 80000, marj: 32 },
  { cariId: 2, ad: 'XYZ Sanayi A.Ş.', ciro: 120000, maliyet: 100000, kar: 20000, marj: null }
]

const ornekVeri = (degisiklikler = {}) => ({
  donem: { kod: 'BU_AY', baslangic: '2026-11-01', bitis: '2026-11-30', etiket: 'Bu Ay' },
  ozet: { ciro: 300000, maliyet: 210000, kar: 90000, marj: 30, maliyetsizKalem: 0 },
  urunler: urunler(),
  cariler,
  ...degisiklikler
})

const mountEt = (props = {}) =>
  mount(DashboardKarAnalizi, {
    props: { visible: true, donem: 'BU_AY', ...props },
    attachTo: document.body,
    global: { plugins: [i18n] }
  })

const para = (deger) =>
  new Intl.NumberFormat('tr-TR', { style: 'currency', currency: 'TRY' }).format(deger)

describe('DashboardKarAnalizi', () => {
  beforeEach(() => {
    dashboardAPI.karAnalizi.mockResolvedValue({ data: ornekVeri() })
  })

  afterEach(() => {
    vi.clearAllMocks()
    document.body.innerHTML = ''
  })

  it('açılınca seçili dönemle yükler ve özet hücrelerini biçimlendirir', async () => {
    const wrapper = mountEt({ donem: 'GECEN_AY' })
    await flushPromises()

    expect(dashboardAPI.karAnalizi).toHaveBeenCalledWith({ donem: 'GECEN_AY' })
    expect(wrapper.find('[data-test="kar-analizi-donem"]').text()).toBe('Bu Ay')
    expect(wrapper.find('[data-test="kar-analizi-ciro"]').text()).toBe(para(300000))
    expect(wrapper.find('[data-test="kar-analizi-maliyet"]').text()).toBe(para(210000))
    expect(wrapper.find('[data-test="kar-analizi-kar"]').text()).toBe(para(90000))
    expect(wrapper.find('[data-test="kar-analizi-marj"]').text()).toBe('%30.0')
    wrapper.unmount()
  })

  it('en kârlı ürünleri ilk 10 satırla tabloya ve grafiğe aktarır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const tablo = wrapper.find('[data-test="kar-analizi-urun-tablo"]')
    expect(tablo.exists()).toBe(true)
    expect(tablo.findAll('thead th').map((th) => th.text())).toEqual([
      i18n.global.t('dashboard.karAnaliziUrun'),
      i18n.global.t('dashboard.karAnaliziCiro'),
      i18n.global.t('dashboard.karAnaliziMaliyet'),
      i18n.global.t('dashboard.karAnaliziKar'),
      i18n.global.t('dashboard.karAnaliziMarj')
    ])
    expect(tablo.findAll('tbody tr')).toHaveLength(10)
    expect(tablo.find('[data-test="kar-urun-satir-0"]').text()).toContain('Ürün 1')
    expect(wrapper.find('[data-test="kar-analizi-urunler"] .stub-bar').exists()).toBe(true)
    wrapper.unmount()
  })

  it('en kârlı carileri tabloya aktarır ve null marjı "—" gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const tablo = wrapper.find('[data-test="kar-analizi-cari-tablo"]')
    expect(tablo.exists()).toBe(true)
    expect(tablo.findAll('tbody tr')).toHaveLength(2)

    const ikinci = tablo.find('[data-test="kar-cari-satir-1"]')
    expect(ikinci.text()).toContain('XYZ Sanayi A.Ş.')
    expect(ikinci.findAll('td').at(-1).text()).toBe('—')
    wrapper.unmount()
  })

  it('maliyet kaydı olmayan kalemler için amber uyarı gösterir', async () => {
    dashboardAPI.karAnalizi.mockResolvedValue({
      data: ornekVeri({ ozet: { ...ornekVeri().ozet, maliyetsizKalem: 3 } })
    })
    const wrapper = mountEt()
    await flushPromises()

    const uyari = wrapper.find('[data-test="kar-analizi-maliyetsiz-uyari"]')
    expect(uyari.exists()).toBe(true)
    expect(uyari.text()).toContain('3')
    expect(uyari.text()).toContain(i18n.global.t('dashboard.karAnaliziMaliyetsizUyari', { n: 3 }))
    wrapper.unmount()
  })

  it('ürün ve cari listesi boşsa "Veri yok" durumunu gösterir', async () => {
    dashboardAPI.karAnalizi.mockResolvedValue({
      data: ornekVeri({ urunler: [], cariler: [] })
    })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="kar-analizi-bos"]').text()).toContain(
      i18n.global.t('dashboard.karAnaliziVeriYok')
    )
    expect(wrapper.find('[data-test="kar-analizi-urun-tablo"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="kar-analizi-cari-tablo"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('hata durumunda "Yeniden dene" ile tekrar yükler', async () => {
    dashboardAPI.karAnalizi.mockRejectedValueOnce(new Error('offline'))
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="kar-analizi-hata"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="kar-analizi-hata"]').text()).toContain(
      i18n.global.t('dashboard.karAnaliziHata')
    )

    await wrapper.find('[data-test="kar-analizi-yeniden-dene"]').trigger('click')
    await flushPromises()

    expect(dashboardAPI.karAnalizi).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-test="kar-analizi-ciro"]').exists()).toBe(true)
    wrapper.unmount()
  })

  it('drawer açıkken dönem değişince yeni dönemle yeniden yükler', async () => {
    const wrapper = mountEt({ donem: 'BU_AY' })
    await flushPromises()
    dashboardAPI.karAnalizi.mockClear()

    await wrapper.setProps({ donem: 'BU_YIL' })
    await flushPromises()

    expect(dashboardAPI.karAnalizi).toHaveBeenCalledTimes(1)
    expect(dashboardAPI.karAnalizi).toHaveBeenCalledWith({ donem: 'BU_YIL' })
    wrapper.unmount()
  })

  it('kapalıyken veri yüklemez; Escape kapatma olayı yayar', async () => {
    const kapali = mountEt({ visible: false })
    await flushPromises()
    expect(dashboardAPI.karAnalizi).not.toHaveBeenCalled()
    kapali.unmount()

    const wrapper = mountEt()
    await flushPromises()
    await wrapper.find('[data-test="kar-analizi-drawer"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    wrapper.unmount()
  })

  it('açılınca odağı panele taşır ve aria-modal bildirir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const panel = wrapper.find('[data-test="kar-analizi-drawer"]')
    expect(panel.attributes('aria-modal')).toBe('true')
    expect(panel.attributes('role')).toBe('dialog')
    expect(document.activeElement).toBe(panel.element)
    wrapper.unmount()
  })
})
