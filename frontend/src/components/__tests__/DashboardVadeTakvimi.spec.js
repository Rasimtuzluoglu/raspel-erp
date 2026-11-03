import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import { dashboardAPI } from '../../api/index.js'
import DashboardVadeTakvimi from '../dashboard/DashboardVadeTakvimi.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

vi.mock('../../api/index.js', () => ({
  dashboardAPI: { vadeler: vi.fn() }
}))

const iki = (n) => String(n).padStart(2, '0')
const kod = (yil, ay, gun) => `${yil}-${iki(ay + 1)}-${iki(gun)}`

const bugun = new Date()
const YIL = bugun.getFullYear()
const AY = bugun.getMonth()
const ayEtiketi = (yil, ay) =>
  new Intl.DateTimeFormat('tr-TR', { month: 'long', year: 'numeric' }).format(new Date(yil, ay, 1))

const ayinGunu = (gun, ay = AY, yil = YIL) => kod(yil, ay, gun)
const aySonu = (yil, ay) => {
  const son = new Date(yil, ay + 1, 0)
  return kod(son.getFullYear(), son.getMonth(), son.getDate())
}

const kayitlar = [
  {
    faturaId: 101,
    faturaNumarasi: 'FTR-2026-0101',
    cariAd: 'ABC Ticaret Ltd.',
    vadeTarihi: ayinGunu(5),
    kalanTutar: 12500,
    geciktiMi: true
  },
  {
    faturaId: 102,
    faturaNumarasi: 'FTR-2026-0102',
    cariAd: 'XYZ Sanayi A.Ş.',
    vadeTarihi: ayinGunu(5),
    kalanTutar: 7500,
    geciktiMi: true
  }
]

const mountEt = () =>
  mount(DashboardVadeTakvimi, {
    props: { visible: true },
    global: {
      plugins: [i18n],
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('DashboardVadeTakvimi', () => {
  beforeEach(() => {
    dashboardAPI.vadeler.mockResolvedValue({ data: kayitlar })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('açılınca içinde bulunulan ayın aralığını yükler ve Pzt–Paz ızgarası çizer', async () => {
    const wrapper = mountEt()
    await flushPromises()

    const ilk = ayinGunu(1)
    const son = aySonu(YIL, AY)
    expect(dashboardAPI.vadeler).toHaveBeenCalledWith({ baslangic: ilk, bitis: son })
    expect(wrapper.find('[data-test="takvim-ay"]').text()).toBe(ayEtiketi(YIL, AY))
    expect(wrapper.findAll('.takvim-gun-adi')).toHaveLength(7)
    expect(wrapper.findAll('.takvim-gun-adi')[0].text()).toBe(i18n.global.t('dashboard.gunKisaPzt'))
    expect(wrapper.findAll('.takvim-gun-adi')[6].text()).toBe(i18n.global.t('dashboard.gunKisaPaz'))

    const gun5 = wrapper.find(`[data-test="takvim-gun-${ayinGunu(5)}"]`)
    expect(gun5.exists()).toBe(true)
    expect(gun5.classes()).toContain('takvim-hucre--gecikmis')
    expect(gun5.find('.takvim-gun-adet').text()).toBe('2')
    expect(gun5.find('.takvim-gun-tutar').text()).toContain('₺')
    wrapper.unmount()
  })

  it('önceki/sonraki ay geçişi aralığı yeniden yükler', async () => {
    const wrapper = mountEt()
    await flushPromises()
    dashboardAPI.vadeler.mockClear()

    await wrapper.find('[data-test="takvim-sonraki-ay"]').trigger('click')
    await flushPromises()

    const sonrakiAy = AY === 11 ? 0 : AY + 1
    const sonrakiYil = AY === 11 ? YIL + 1 : YIL
    expect(dashboardAPI.vadeler).toHaveBeenCalledWith({
      baslangic: kod(sonrakiYil, sonrakiAy, 1),
      bitis: aySonu(sonrakiYil, sonrakiAy)
    })
    expect(wrapper.find('[data-test="takvim-ay"]').text()).toBe(ayEtiketi(sonrakiYil, sonrakiAy))

    dashboardAPI.vadeler.mockClear()
    await wrapper.find('[data-test="takvim-onceki-ay"]').trigger('click')
    await flushPromises()
    await wrapper.find('[data-test="takvim-onceki-ay"]').trigger('click')
    await flushPromises()

    const oncekiAy = AY === 0 ? 11 : AY - 1
    const oncekiYil = AY === 0 ? YIL - 1 : YIL
    expect(wrapper.find('[data-test="takvim-ay"]').text()).toBe(ayEtiketi(oncekiYil, oncekiAy))
    wrapper.unmount()
  })

  it('güne tıklanınca fatura listesini ve tahsilat bağlantısını gösterir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find(`[data-test="takvim-gun-${ayinGunu(5)}"]`).trigger('click')

    const liste = wrapper.find('[data-test="takvim-liste"]')
    expect(liste.exists()).toBe(true)
    expect(liste.text()).toContain('FTR-2026-0101')
    expect(liste.text()).toContain('ABC Ticaret Ltd.')
    expect(liste.findAll('.takvim-kayit')).toHaveLength(2)
    expect(liste.find('.takvim-liste-link').attributes('href')).toBe('/tahsilat')
    expect(liste.find('.takvim-kayit--gecikmis').exists()).toBe(true)

    // Ayni gune tekrar tiklamak secimi kaldirir.
    await wrapper.find(`[data-test="takvim-gun-${ayinGunu(5)}"]`).trigger('click')
    expect(wrapper.find('[data-test="takvim-liste"]').exists()).toBe(false)
    wrapper.unmount()
  })

  it('boş ayda bilgi mesajı gösterir', async () => {
    dashboardAPI.vadeler.mockResolvedValueOnce({ data: [] })
    const wrapper = mountEt()
    await flushPromises()

    expect(wrapper.find('[data-test="takvim-bos"]').exists()).toBe(true)
    expect(wrapper.find('[data-test="takvim-bos"]').text()).toContain(
      i18n.global.t('dashboard.takvimBos')
    )
    wrapper.unmount()
  })

  it('hata durumunda mesaj ve yeniden dene sunar', async () => {
    dashboardAPI.vadeler.mockRejectedValueOnce(new Error('ağ hatası'))
    const wrapper = mountEt()
    await flushPromises()

    const hata = wrapper.find('[data-test="takvim-hata"]')
    expect(hata.exists()).toBe(true)
    expect(hata.text()).toContain(i18n.global.t('dashboard.takvimHata'))

    dashboardAPI.vadeler.mockResolvedValueOnce({ data: kayitlar })
    await hata.find('.takvim-dene').trigger('click')
    await flushPromises()
    expect(wrapper.find('[data-test="takvim-hata"]').exists()).toBe(false)
    expect(wrapper.findAll('.takvim-hucre--dolu').length).toBeGreaterThan(0)
    wrapper.unmount()
  })

  it('Escape paneli kapatır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="vade-takvimi"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    wrapper.unmount()
  })
})
