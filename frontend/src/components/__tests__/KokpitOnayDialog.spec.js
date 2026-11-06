import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitOnayDialog from '../dashboard/kokpit/KokpitOnayDialog.vue'
import { onaylarAPI } from '../../api/index.js'

vi.mock('../../api/index.js', () => ({
  onaylarAPI: {
    bekleyen: vi.fn(),
    karar: vi.fn()
  }
}))

vi.mock('../../composables/useToastBildirim.js', () => ({
  useToastBildirim: () => ({
    basarili: vi.fn(),
    hata: vi.fn(),
    uyari: vi.fn(),
    bilgi: vi.fn()
  })
}))

const mountDialog = () => mount(KokpitOnayDialog, {
  global: { plugins: [i18n] },
  props: { visible: true }
})

describe('KokpitOnayDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    onaylarAPI.bekleyen.mockResolvedValue({
      data: {
        content: [
          {
            tur: 'IZIN',
            kaynakId: 11,
            baslik: 'Yıllık izin talebi',
            altBaslik: 'Ali Veli',
            tutar: null,
            tarih: '2026-11-06'
          }
        ]
      }
    })
    onaylarAPI.karar.mockResolvedValue({ data: {} })
  })

  it('bekleyenOnaylariListeler', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    expect(onaylarAPI.bekleyen).toHaveBeenCalledWith({ page: 0, size: 20 })
    expect(wrapper.text()).toContain('Yıllık izin talebi')
    expect(wrapper.text()).toContain('Ali Veli')
    expect(wrapper.text()).toContain('İzin Onayı')
  })

  it('onaylarVeGuncellendiYayar', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('[data-test="kokpit-onayla-IZIN:11"]').trigger('click')
    await flushPromises()

    expect(onaylarAPI.karar).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakId: 11,
      karar: 'ONAY',
      gerekce: ''
    })
    expect(wrapper.emitted('guncellendi')[0][0].karar).toBe('ONAY')
  })

  it('redGerekcesiyleReddeder', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('[data-test="kokpit-reddet-IZIN:11"]').trigger('click')
    await wrapper.find('[data-test="kokpit-onay-gerekce"]').setValue('Bütçe uygun değil')
    await wrapper.find('[data-test="kokpit-onay-red-gonder"]').trigger('click')
    await flushPromises()

    expect(onaylarAPI.karar).toHaveBeenCalledWith({
      tur: 'IZIN',
      kaynakId: 11,
      karar: 'RED',
      gerekce: 'Bütçe uygun değil'
    })
    expect(wrapper.emitted('guncellendi')[0][0].karar).toBe('RED')
  })

  it('bekleyenYoksaBosDurumGosterir', async () => {
    onaylarAPI.bekleyen.mockResolvedValue({ data: { content: [] } })
    const wrapper = mountDialog()
    await flushPromises()

    expect(wrapper.find('[data-test="kokpit-onay-bos"]').exists()).toBe(true)
  })
})
