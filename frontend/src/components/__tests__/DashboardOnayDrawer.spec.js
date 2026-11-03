import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { PrimeVueToastSymbol } from 'primevue/usetoast'
import i18n from '../../i18n.js'
import { onaylarAPI } from '../../api/index.js'
import DashboardOnayDrawer from '../dashboard/DashboardOnayDrawer.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

vi.mock('../../api/index.js', () => ({
  onaylarAPI: {
    bekleyen: vi.fn(),
    karar: vi.fn()
  }
}))

const bekleyenVeri = [
  {
    tur: 'IZIN',
    kaynakId: 11,
    baslik: 'Yıllık izin talebi',
    altBaslik: 'Ayşe Yılmaz · 3 gün',
    tutar: null,
    tarih: '2026-11-01'
  },
  {
    tur: 'IZIN',
    kaynakId: 12,
    baslik: 'Mazeret izni',
    altBaslik: 'Mehmet Demir · 1 gün',
    tutar: null,
    tarih: '2026-11-02'
  }
]

const toast = { add: vi.fn() }

const mountEt = (props = {}) =>
  mount(DashboardOnayDrawer, {
    props: { visible: true, tur: 'ONAY_IZIN', ...props },
    global: {
      plugins: [i18n],
      provide: { [PrimeVueToastSymbol]: toast },
      stubs: { 'router-link': RouterLinkStub }
    }
  })

describe('DashboardOnayDrawer', () => {
  beforeEach(() => {
    onaylarAPI.bekleyen.mockResolvedValue({ data: bekleyenVeri })
    onaylarAPI.karar.mockResolvedValue({ data: { basarili: 1 } })
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('açıldığında tür eşlemesiyle bekleyen kayıtları yükler', async () => {
    const wrapper = mountEt()
    await flushPromises()

    expect(onaylarAPI.bekleyen).toHaveBeenCalledWith({ tur: 'IZIN', page: 0, size: 50 })
    expect(wrapper.find('[data-test="onay-drawer"]').attributes('role')).toBe('dialog')
    expect(wrapper.find('[data-test="onay-drawer"]').attributes('aria-modal')).toBe('true')

    const satirlar = wrapper.findAll('.onay-satir')
    expect(satirlar).toHaveLength(2)
    expect(satirlar[0].text()).toContain('Yıllık izin talebi')
    expect(satirlar[0].text()).toContain('Ayşe Yılmaz')
    expect(satirlar[0].text()).toContain('2026')
    wrapper.unmount()
  })

  it('diğer türleri doğru API türüne eşler', async () => {
    const masraf = mountEt({ tur: 'ONAY_MASRAF' })
    await flushPromises()
    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith({ tur: 'MASRAF', page: 0, size: 50 })
    masraf.unmount()

    const siparis = mountEt({ tur: 'ONAY_SIPARIS' })
    await flushPromises()
    expect(onaylarAPI.bekleyen).toHaveBeenLastCalledWith({ tur: 'SIPARIS', page: 0, size: 50 })
    siparis.unmount()
  })

  it('onaylar: karar gönderir, satırı listeden çıkarır ve bildirir', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="onayla-IZIN:11"]').trigger('click')
    await flushPromises()

    expect(onaylarAPI.karar).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakId: 11,
      karar: 'ONAY',
      gerekce: ''
    })
    expect(wrapper.findAll('.onay-satir')).toHaveLength(1)
    expect(wrapper.find('[data-test="onay-satir-IZIN:11"]').exists()).toBe(false)
    expect(wrapper.emitted('guncellendi')).toHaveLength(1)
    expect(wrapper.emitted('guncellendi')[0][0].karar).toBe('ONAY')
    expect(toast.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', detail: i18n.global.t('dashboard.onayBasarili') })
    )
    wrapper.unmount()
  })

  it('reddederken gerekçe ister ve en az 3 karakter zorunludur', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="reddet-IZIN:12"]').trigger('click')
    await nextTick()
    const modal = wrapper.find('[data-test="onay-red-modal"]')
    expect(modal.exists()).toBe(true)

    const gonder = wrapper.find('[data-test="onay-red-gonder"]')
    expect(gonder.attributes('disabled')).toBeDefined()

    await wrapper.find('[data-test="onay-red-gerekce"]').setValue('ab')
    expect(gonder.attributes('disabled')).toBeDefined()

    await wrapper.find('[data-test="onay-red-gerekce"]').setValue('Uygun değil')
    expect(gonder.attributes('disabled')).toBeUndefined()

    await gonder.trigger('click')
    await flushPromises()

    expect(onaylarAPI.karar).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakId: 12,
      karar: 'RED',
      gerekce: 'Uygun değil'
    })
    expect(wrapper.findAll('.onay-satir')).toHaveLength(1)
    expect(wrapper.find('[data-test="onay-satir-IZIN:12"]').exists()).toBe(false)
    expect(wrapper.find('[data-test="onay-red-modal"]').exists()).toBe(false)
    expect(toast.add).toHaveBeenCalledWith(
      expect.objectContaining({ severity: 'success', detail: i18n.global.t('dashboard.redBasarili') })
    )
    wrapper.unmount()
  })

  it('Escape paneli kapatır', async () => {
    const wrapper = mountEt()
    await flushPromises()

    await wrapper.find('[data-test="onay-drawer"]').trigger('keydown', { key: 'Escape' })
    expect(wrapper.emitted('update:visible')).toEqual([[false]])
    wrapper.unmount()
  })

  it('boş ve hata durumlarını gösterir', async () => {
    onaylarAPI.bekleyen.mockResolvedValueOnce({ data: [] })
    const bos = mountEt()
    await flushPromises()
    expect(bos.find('[data-test="onay-bos"]').exists()).toBe(true)
    expect(bos.find('[data-test="onay-bos"]').text()).toContain(
      i18n.global.t('dashboard.onayListeBos')
    )
    bos.unmount()

    onaylarAPI.bekleyen.mockRejectedValueOnce(new Error('ağ hatası'))
    const hatali = mountEt()
    await flushPromises()
    expect(hatali.find('.onay-durum--hata').exists()).toBe(true)
    expect(hatali.find('.onay-durum--hata').text()).toContain(
      i18n.global.t('dashboard.onayListeHata')
    )
    hatali.unmount()
  })

  it('sayfalı yanıtı (content) destekler', async () => {
    onaylarAPI.bekleyen.mockResolvedValueOnce({
      data: { content: [bekleyenVeri[0]], totalElements: 1 }
    })
    const wrapper = mountEt()
    await flushPromises()
    expect(wrapper.findAll('.onay-satir')).toHaveLength(1)
    wrapper.unmount()
  })
})
