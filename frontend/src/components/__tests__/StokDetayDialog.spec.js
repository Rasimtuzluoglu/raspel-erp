import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { stokAPI } from '../../api/index.js'

vi.mock('../../api/index.js', () => ({
  stokAPI: {
    analiz: vi.fn(),
    islemGecmisiSayfali: vi.fn(),
    aylikFiyat: vi.fn(),
    musteriAnaliz: vi.fn(),
    tedarikciAnaliz: vi.fn()
  }
}))

vi.mock('vue-chartjs', () => ({
  Line: {
    props: ['data', 'options'],
    template: '<div class="chart-line">{{ data.labels.join(",") }} | {{ data.datasets.map((d) => d.label).join(",") }}</div>'
  },
  Bar: { template: '<div />' },
  Doughnut: { template: '<div />' }
}))

const DataTableStub = {
  name: 'DataTable',
  props: ['value', 'rows', 'first', 'totalRecords', 'lazy', 'loading', 'paginator'],
  emits: ['page'],
  template: '<div class="datatable"><slot /></div>'
}

const stubs = {
  Dialog: { template: '<div class="dialog"><slot /></div>' },
  TabView: { template: '<div class="tabview"><slot /></div>' },
  TabPanel: { template: '<div class="tabpanel"><slot /></div>' },
  DatePicker: { template: '<div class="datepicker"><slot /></div>' },
  DataTable: DataTableStub,
  Column: { template: '<div class="column"><slot /></div>' },
  EmptyState: {
    props: ['message'],
    template: '<div class="empty-state">{{ message }}</div>'
  }
}

const toLocaleTarih = (d) => {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const g = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${g}`
}

describe('StokDetayDialog.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    stokAPI.analiz.mockResolvedValue({
      data: {
        stokId: 1,
        stokAd: 'PVC Bant Beyaz',
        birim: 'Rulo',
        alisOzet: {
          toplamAlisTutari: 1000,
          stokMiktar: 193,
          toplamAlisMiktar: 100
        },
        satisOzet: {
          toplamSatisTutari: 315,
          toplamSatisMiktar: 7
        },
        karlilik: {
          stokMaliyeti: 8685,
          ortalamaMaliyet: 45,
          ortalamaSatisFiyati: 45,
          toplamBrutKar: 0,
          brutKarMarji: 0,
          satilanMiktar: 7,
          satisIadeTutari: 0
        }
      }
    })
    stokAPI.islemGecmisiSayfali.mockResolvedValue({
      data: {
        satirlar: [
          { tarih: '2026-09-09', belgeNo: 'FTR-001', tur: 'SATIS', miktar: 1, birimFiyat: 45, tutar: 45 }
        ],
        toplam: 1,
        sayfa: 0,
        boyut: 10,
        toplamSayfa: 1
      }
    })
    stokAPI.aylikFiyat.mockResolvedValue({
      data: [{ yil: 2026, ay: 9, ortalamaAlisFiyati: 30, ortalamaSatisFiyati: 45, toplamAlisMiktar: 100, toplamSatisMiktar: 7 }]
    })
    stokAPI.musteriAnaliz.mockResolvedValue({
      data: [{ cariHesapAd: 'Müşteri A', toplamMiktar: 7, toplamTutar: 315, islemSayisi: 3 }]
    })
    stokAPI.tedarikciAnaliz.mockResolvedValue({ data: [] })
  })

  const countAnalizCalls = () =>
    stokAPI.analiz.mock.calls.length +
    stokAPI.islemGecmisiSayfali.mock.calls.length +
    stokAPI.aylikFiyat.mock.calls.length +
    stokAPI.musteriAnaliz.mock.calls.length +
    stokAPI.tedarikciAnaliz.mock.calls.length

  it('visible olduğunda analiz verilerini yükler ve özeti gösterir', async () => {
    const StokDetayDialog = (await import('../StokDetayDialog.vue')).default
    const wrapper = mount(StokDetayDialog, {
      props: { visible: false, stok: { id: 1, ad: 'PVC Bant Beyaz', birim: 'Rulo' } },
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    expect(countAnalizCalls()).toBe(0)
    await wrapper.setProps({ visible: true })
    await flushPromises()

    expect(stokAPI.analiz).toHaveBeenCalledWith(1, expect.objectContaining({ baslangic: `${new Date().getFullYear()}-01-01` }))
    expect(stokAPI.islemGecmisiSayfali).toHaveBeenCalledWith(1, expect.objectContaining({ baslangic: `${new Date().getFullYear()}-01-01`, sayfa: 0, boyut: 10 }))
    expect(stokAPI.aylikFiyat).toHaveBeenCalledWith(1, expect.objectContaining({ bitis: toLocaleTarih(new Date()) }))
    const text = wrapper.text()
    expect(text).toContain('Toplam Satış Tutarı')
    expect(text).toContain('315,00')
    expect(text).toContain('İşlem Geçmişi')
    expect(text).toContain('Müşteri Bazlı')
    expect(text).toContain('Tedarikçi Bazlı')
    expect(text).toContain('Aylık Fiyat Serisi')
  })

  it('aylık fiyat serisini grafik verisine dönüştürür', async () => {
    const StokDetayDialog = (await import('../StokDetayDialog.vue')).default
    const wrapper = mount(StokDetayDialog, {
      props: { visible: false, stok: { id: 1, ad: 'PVC Bant Beyaz', birim: 'Rulo' } },
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()
    const chart = wrapper.find('.chart-line')
    expect(chart.exists()).toBe(true)
    expect(chart.text()).toContain('9/2026')
    expect(chart.text()).toContain('Ort. Alış')
    expect(chart.text()).toContain('Ort. Satış')
  })

  it('stok yoksa veri çekmez', async () => {
    const StokDetayDialog = (await import('../StokDetayDialog.vue')).default
    const wrapper = mount(StokDetayDialog, {
      props: { visible: false, stok: null },
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()
    expect(countAnalizCalls()).toBe(0)
  })

  it('analiz yüklenemezse hata mesajı gösterir', async () => {
    stokAPI.analiz.mockRejectedValue({ response: { data: { message: 'Analiz hatası' } } })
    const StokDetayDialog = (await import('../StokDetayDialog.vue')).default
    const wrapper = mount(StokDetayDialog, {
      props: { visible: false, stok: { id: 2, ad: 'MDF', birim: 'Adet' } },
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()
    expect(wrapper.text()).toContain('Analiz hatası')
  })

  it('işlem geçmişini sunucu taraflı sayfalar', async () => {
    stokAPI.islemGecmisiSayfali.mockResolvedValue({
      data: {
        satirlar: [
          { tarih: '2026-09-09', belgeNo: 'FTR-001', tur: 'SATIS', miktar: 1, birimFiyat: 45, tutar: 45 }
        ],
        toplam: 30,
        sayfa: 0,
        boyut: 10,
        toplamSayfa: 3
      }
    })
    const StokDetayDialog = (await import('../StokDetayDialog.vue')).default
    const wrapper = mount(StokDetayDialog, {
      props: { visible: false, stok: { id: 1, ad: 'PVC Bant Beyaz', birim: 'Rulo' } },
      global: { stubs, plugins: [createPinia(), ToastService, i18n] }
    })
    await wrapper.setProps({ visible: true })
    await flushPromises()
    expect(stokAPI.islemGecmisiSayfali).toHaveBeenLastCalledWith(1, expect.objectContaining({ sayfa: 0, boyut: 10 }))
    const tablo = wrapper.findComponent(DataTableStub)
    tablo.vm.$emit('page', { page: 2, rows: 10 })
    await flushPromises()
    expect(stokAPI.islemGecmisiSayfali).toHaveBeenLastCalledWith(1, expect.objectContaining({ sayfa: 2, boyut: 10 }))
  })
})