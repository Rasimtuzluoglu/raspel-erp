import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import KokpitTahsilatDialog from '../dashboard/kokpit/KokpitTahsilatDialog.vue'
import { hareketAPI, cariHesapAPI } from '../../api/index.js'

vi.mock('../../api/index.js', () => ({
  hareketAPI: { create: vi.fn() },
  cariHesapAPI: {
    filtreli: vi.fn(),
    getById: vi.fn()
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

const mountDialog = (props = {}) => mount(KokpitTahsilatDialog, {
  global: { plugins: [i18n] },
  props: {
    visible: true,
    tur: 'TAHSILAT',
    cariId: null,
    ...props
  }
})

describe('KokpitTahsilatDialog', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    cariHesapAPI.filtreli.mockResolvedValue({ data: [] })
    cariHesapAPI.getById.mockResolvedValue({
      data: { id: 5, ad: 'Müşteri A' }
    })
    hareketAPI.create.mockResolvedValue({ data: {} })
  })

  it('cariIdIleSeciliCariyiGosterir', async () => {
    const wrapper = mountDialog({ cariId: 5 })
    await flushPromises()

    expect(cariHesapAPI.getById).toHaveBeenCalledWith(5)
    expect(wrapper.text()).toContain('Müşteri A')
    expect(wrapper.text()).toContain('Hızlı Tahsilat')
  })

  it('tahsilatKaydederVeOlayYayar', async () => {
    const wrapper = mountDialog({ cariId: 5 })
    await flushPromises()

    await wrapper.find('[data-test="hizli-tutar"]').setValue('100')
    await wrapper.find('[data-test="hizli-kaydet"]').trigger('click')
    await flushPromises()

    expect(hareketAPI.create).toHaveBeenCalledWith(expect.objectContaining({
      cariHesapId: 5,
      tur: 'TAHSILAT',
      tutar: 100
    }))
    expect(wrapper.emitted('kaydedildi')[0][0]).toEqual({
      tur: 'TAHSILAT',
      tutar: 100,
      cariId: 5
    })
  })

  it('cariSecilmedenKaydetmez', async () => {
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('[data-test="hizli-tutar"]').setValue('100')
    await wrapper.find('[data-test="hizli-kaydet"]').trigger('click')

    expect(hareketAPI.create).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('Cari seçin')
  })

  it('odemeModundaBaslikDegisir', async () => {
    const wrapper = mountDialog({ cariId: 5, tur: 'ODEME' })
    await flushPromises()

    expect(wrapper.text()).toContain('Hızlı Ödeme')
  })

  it('cariOnerilerindenSecimYapar', async () => {
    cariHesapAPI.filtreli.mockResolvedValue({
      data: [
        { id: 7, ad: 'Müşteri B' },
        { id: 8, ad: 'Müşteri C' }
      ]
    })
    const wrapper = mountDialog()
    await flushPromises()

    await wrapper.find('[data-test="hizli-cari-7"]').trigger('click')
    expect(wrapper.text()).toContain('Müşteri B')
  })
})
