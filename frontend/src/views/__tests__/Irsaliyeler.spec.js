import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { irsaliyeAPI, excelAPI } from '../../api/index.js'
import Irsaliyeler from '../Irsaliyeler.vue'

const satirlar = [
  {
    id: 1,
    irsaliyeNo: 'IRS-001',
    tarih: '2026-10-01',
    cariHesapId: 10,
    cariHesapAdi: 'Acme Ltd',
    tur: 'SATIS',
    durum: 'HAZIRLANDI',
    plaka: '34 ABC 123',
    soforId: 5,
    soforAd: 'Ali Veli',
    faturaId: null,
    faturaNumarasi: null,
    kalemler: []
  },
  {
    id: 2,
    irsaliyeNo: 'IRS-002',
    tarih: '2026-10-02',
    cariHesapId: 10,
    cariHesapAdi: 'Acme Ltd',
    tur: 'SATIS',
    durum: 'SEVK_EDILDI',
    plaka: '34 ABC 123',
    soforId: 5,
    soforAd: 'Ali Veli',
    faturaId: null,
    faturaNumarasi: null,
    kalemler: []
  },
  {
    id: 3,
    irsaliyeNo: 'IRS-003',
    tarih: '2026-10-03',
    cariHesapId: 11,
    cariHesapAdi: 'Beta AŞ',
    tur: 'ALIS',
    durum: 'TESLIM_EDILDI',
    plaka: '06 XYZ 9',
    soforAd: 'Veli Ali',
    faturaId: 90,
    faturaNumarasi: 'FTR-2026-0090',
    kalemler: []
  }
]

const filtrele = (params = {}) => {
  let sonuc = satirlar
  if (params.durum) sonuc = sonuc.filter((s) => s.durum === params.durum)
  if (params.faturali === false) sonuc = sonuc.filter((s) => !s.faturaId)
  if (params.faturali === true) sonuc = sonuc.filter((s) => !!s.faturaId)
  if (params.cariId) sonuc = sonuc.filter((s) => s.cariHesapId === params.cariId)
  if (params.tur) sonuc = sonuc.filter((s) => s.tur === params.tur)
  if (params.q) {
    const q = String(params.q).toLowerCase()
    sonuc = sonuc.filter((s) => s.irsaliyeNo.toLowerCase().includes(q) || s.cariHesapAdi.toLowerCase().includes(q))
  }
  return { content: sonuc, totalElements: sonuc.length }
}

vi.mock('../../api/index.js', () => ({
  irsaliyeAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    filtreli: vi.fn((params) => Promise.resolve({ data: filtrele(params) })),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: { taslak: 1, hazirlanan: 1, sevkEdilen: 1, teslimEdilenBuAy: 1, faturasiz: 2, iptal: 0 }
      })
    ),
    getById: vi.fn((id) => Promise.resolve({ data: satirlar.find((s) => s.id === id) })),
    create: vi.fn(() => Promise.resolve({ data: {} })),
    update: vi.fn(() => Promise.resolve({ data: {} })),
    hazirla: vi.fn(() => Promise.resolve({ data: {} })),
    sevk: vi.fn(() => Promise.resolve({ data: {} })),
    teslim: vi.fn(() => Promise.resolve({ data: {} })),
    sevkiGeriAl: vi.fn(() => Promise.resolve({ data: {} })),
    iptal: vi.fn(() => Promise.resolve({ data: {} })),
    faturayaDonustur: vi.fn(() => Promise.resolve({ data: {} })),
    topluFatura: vi.fn(() =>
      Promise.resolve({
        data: {
          basarili: [{ irsaliyeId: 1, irsaliyeNo: 'IRS-001', faturaId: 100, faturaNo: 'FTR-100' }],
          atlanan: [{ irsaliyeId: 2, irsaliyeNo: 'IRS-002', sebep: 'Canlı fatura var' }]
        }
      })
    ),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  },
  cariHesapAPI: {
    getAll: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 10, ad: 'Acme Ltd', adres: 'Adres 10' },
            { id: 11, ad: 'Beta AŞ', adres: 'Adres 11' }
          ]
        }
      })
    )
  },
  stokAPI: {
    getAll: vi.fn(() =>
      Promise.resolve({ data: { content: [{ id: 100, ad: 'Ürün A', barkod: 'BRK1', birim: 'Adet' }] } })
    )
  },
  siparisAPI: { getAll: vi.fn(() => Promise.resolve({ data: { content: [{ id: 7, siparisNo: 'SIP-7' }] } })) },
  depoAPI: { getAll: vi.fn(() => Promise.resolve({ data: [{ id: 1, ad: 'Merkez Depo' }] })) },
  siparisTakipAPI: { soforler: vi.fn(() => Promise.resolve({ data: [{ id: 5, ad: 'Ali Veli' }] })) },
  pdfAPI: { irsaliye: vi.fn(() => Promise.resolve({ data: new Blob() })) },
  excelAPI: { irsaliyeler: vi.fn(() => Promise.resolve({ data: new Blob() })) }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'description'],
    template: '<div class="ph">{{ title }}<slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>'
  },
  AppDataTable: {
    props: ['value', 'selection'],
    template: '<div class="tablo"><slot name="batch-actions" /><slot /><slot name="empty" /></div>'
  },
  Column: true,
  SatirEylemleri: true,
  EmptyState: true,
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  IrsaliyeDetayDrawer: {
    props: ['visible', 'kayit', 'gecmis', 'gecmisYukleniyor'],
    template: '<div class="drawer">{{ kayit ? kayit.irsaliyeNo : "" }}</div>'
  },
  BarcodeScannerModal: true,
  RouterLink: { props: ['to'], template: '<a class="router-link"><slot /></a>' },
  Button: { props: ['label'], template: '<button>{{ label }}<slot /></button>' },
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  Textarea: true,
  DatePicker: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Dialog: { template: '<div class="dialog"><slot /><slot name="footer" /></div>' },
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: {
    props: ['header'],
    template: '<div class="tab"><span class="tab-baslik">{{ header }}</span><slot /></div>'
  },
  Toast: true
}

const kur = () =>
  mount(Irsaliyeler, {
    global: { plugins: [ToastService, ConfirmationService, i18n], stubs }
  })

describe('Irsaliyeler.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    window.URL.createObjectURL = vi.fn(() => 'blob:irsaliye')
    window.URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  it('varsayılan Hazırlanan sekmesiyle sunucu filtreli listeyi ve KPI’ları yükler', async () => {
    const w = kur()
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(1)
    expect(irsaliyeAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ durum: 'HAZIRLANDI', page: 0, size: 25, sort: 'tarih,desc' })
    )
    expect(irsaliyeAPI.ozet).toHaveBeenCalled()
    expect(w.vm.list).toHaveLength(1)
    expect(w.vm.list[0].irsaliyeNo).toBe('IRS-001')
    expect(w.findAll('.kpi')).toHaveLength(4)
    expect(w.text()).toContain('Hazırlanan:1')
    expect(w.text()).toContain('Faturasız:2')
    expect(w.text()).toContain('IRS-001')
  })

  it('son açık sekmeyi ve filtreleri hafızadan geri yükler', async () => {
    localStorage.setItem('raspel_irsaliye_tab', '2')
    localStorage.setItem(
      'raspel_irsaliye_filtre',
      JSON.stringify({ q: 'acme', cariId: 10, tur: 'SATIS', baslangic: '2026-10-01', bitis: '2026-10-31' })
    )
    const w = kur()
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(2)
    expect(w.vm.q).toBe('acme')
    expect(w.vm.cariId).toBe(10)
    expect(w.vm.tur).toBe('SATIS')
    expect(irsaliyeAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({
        q: 'acme',
        cariId: 10,
        tur: 'SATIS',
        durum: 'SEVK_EDILDI',
        baslangic: '2026-10-01',
        bitis: '2026-10-31'
      })
    )
  })

  it('KPI tıklaması sekmeyi/filreyi değiştirir ve hafızaya yazar', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    await w.findAll('.kpi')[1].trigger('click')
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(2)
    expect(irsaliyeAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ durum: 'SEVK_EDILDI', page: 0 })
    )
    expect(localStorage.getItem('raspel_irsaliye_tab')).toBe('2')
    expect(w.findAll('.kpi')[1].attributes('aria-pressed')).toBe('true')
  })

  it('faturasız KPI tıklaması faturali=false filtresini uygular', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    await w.findAll('.kpi')[3].trigger('click')
    await flushPromises()

    expect(w.vm.faturasiz).toBe(true)
    expect(w.vm.aktifSekme).toBe(0)
    expect(irsaliyeAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ faturali: false, page: 0 })
    )
    expect(w.findAll('.kpi')[3].attributes('aria-pressed')).toBe('true')
  })

  it('filtre değişikliklerini sunucu parametrelerine ve localStorage’a yazar', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.cariId = 10
    w.vm.tur = 'SATIS'
    w.vm.filtreDegisti()
    await flushPromises()

    expect(irsaliyeAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ cariId: 10, tur: 'SATIS', page: 0 })
    )
    const kayit = JSON.parse(localStorage.getItem('raspel_irsaliye_filtre'))
    expect(kayit.cariId).toBe(10)
    expect(kayit.tur).toBe('SATIS')
  })

  it('durum filtresi sekmeyi eşleştirir, filtre temizleme Tümü’ye döner', async () => {
    const w = kur()
    await flushPromises()

    w.vm.durum = 'TESLIM_EDILDI'
    w.vm.durumDegisti()
    await flushPromises()
    expect(w.vm.aktifSekme).toBe(3)

    w.vm.filtreTemizle()
    await flushPromises()
    expect(w.vm.aktifSekme).toBe(0)
    expect(w.vm.durum).toBeNull()
    expect(w.vm.q).toBe('')
  })

  it('hazırla/sevk/teslim/sevki geri al/iptal API çağrılarını yapar', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.hazirlaKaydet(satirlar[0])
    expect(irsaliyeAPI.hazirla).toHaveBeenCalledWith(1)

    w.vm.sevkAc({ ...satirlar[0], soforId: null })
    expect(w.vm.sevkForm.teslimatAdresi).toBe('Adres 10')
    await w.vm.sevkKaydet()
    expect(irsaliyeAPI.sevk).not.toHaveBeenCalled()
    expect(w.vm.sevkHata).toBeTruthy()

    w.vm.sevkForm.soforId = 5
    w.vm.sevkForm.plaka = '34 ABC 123'
    await w.vm.sevkKaydet()
    expect(irsaliyeAPI.sevk).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ soforId: 5, plaka: '34 ABC 123', teslimatAdresi: 'Adres 10' })
    )

    w.vm.teslimAc(satirlar[1])
    await w.vm.teslimKaydet()
    expect(irsaliyeAPI.teslim).not.toHaveBeenCalled()
    expect(w.vm.teslimHata).toBeTruthy()

    w.vm.teslimForm.teslimAlan = 'Ayşe Kaya'
    await w.vm.teslimKaydet()
    expect(irsaliyeAPI.teslim).toHaveBeenCalledWith(
      2,
      expect.objectContaining({ teslimAlan: 'Ayşe Kaya' })
    )

    await w.vm.sevkiGeriAlKaydet(satirlar[1])
    expect(irsaliyeAPI.sevkiGeriAl).toHaveBeenCalledWith(2)

    w.vm.iptalAc(satirlar[1])
    w.vm.iptalAciklama = 'Müşteri vazgeçti'
    await w.vm.iptalKaydet()
    expect(irsaliyeAPI.iptal).toHaveBeenCalledWith(2, 'Müşteri vazgeçti')
  })

  it('toplu faturada aynı cari seçiminde API’yi çağırır ve sonucu gösterir', async () => {
    const w = kur()
    await flushPromises()

    w.vm.secimDegisti([satirlar[0], satirlar[1]])
    expect(w.vm.farkliCari).toBe(false)
    expect(w.vm.topluUygun).toBe(true)

    await w.vm.topluFaturaKaydet()
    await flushPromises()

    expect(irsaliyeAPI.topluFatura).toHaveBeenCalledWith([1, 2])
    expect(w.vm.sonucDialog).toBe(true)
    expect(w.vm.sonuc.basarili).toHaveLength(1)
    expect(w.vm.sonuc.atlanan).toHaveLength(1)
    expect(w.vm.secili).toHaveLength(0)
  })

  it('farklı cari seçiminde toplu fatura butonunu devre dışı bırakır', async () => {
    const w = kur()
    await flushPromises()

    w.vm.secimDegisti([satirlar[0], satirlar[2]])
    await flushPromises()

    expect(w.vm.farkliCari).toBe(true)
    expect(w.vm.topluUygun).toBe(false)
    const toplu = w.findAll('.tablo button').find((b) => b.text().includes('Toplu Faturaya Dönüştür'))
    expect(toplu).toBeTruthy()
    expect(toplu.attributes('disabled')).toBeDefined()

    await w.vm.topluFaturaKaydet()
    expect(irsaliyeAPI.topluFatura).not.toHaveBeenCalled()
    expect(w.find('.toplu-uyari').text()).toContain('aynı cariye')
  })

  it('satır eylemleri duruma göre filtrelenir', async () => {
    const w = kur()
    await flushPromises()

    const etiketler = (satir) => w.vm.satirEylemleri(satir).map((i) => i.etiket)

    expect(etiketler({ durum: 'TASLAK' })).toEqual(
      expect.arrayContaining(['Detay', 'Düzenle', 'Hazırla', 'Faturaya Dönüştür', 'İptal Et', 'PDF'])
    )
    expect(etiketler({ durum: 'HAZIRLANDI' })).toEqual(
      expect.arrayContaining(['Düzenle', 'Sevk Et'])
    )
    expect(etiketler({ durum: 'SEVK_EDILDI' })).toEqual(
      expect.arrayContaining(['Teslim Et', 'Sevki Geri Al'])
    )
    expect(etiketler({ durum: 'TESLIM_EDILDI', faturaId: 90 })).not.toContain('Faturaya Dönüştür')
    expect(etiketler({ durum: 'IPTAL' })).not.toContain('İptal Et')
    expect(etiketler({ durum: 'IPTAL' })).not.toContain('Düzenle')
  })

  it('Excel indirir, mobil kart ve erişilebilirlik çıktıları üretir', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.listeExcelIndir()
    expect(excelAPI.irsaliyeler).toHaveBeenCalledWith(expect.objectContaining({ durum: 'HAZIRLANDI' }))

    expect(w.find('.sr-only[aria-live="polite"]').exists()).toBe(true)
    expect(w.find('.sr-only[aria-live="polite"]').text()).toContain('irsaliye gösteriliyor')
    expect(w.find('.irsaliye-mobil').exists()).toBe(true)
    expect(w.findAll('.irs-mobil-kart').length).toBe(w.vm.list.length)
    expect(w.find('.faturasiz-cip').attributes('aria-pressed')).toBe('false')

    await w.find('.faturasiz-cip').trigger('click')
    await flushPromises()
    expect(w.find('.faturasiz-cip').attributes('aria-pressed')).toBe('true')
  })
})
