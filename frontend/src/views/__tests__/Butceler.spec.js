import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { butceAPI } from '../../api/index.js'
import Butceler from '../Butceler.vue'

vi.mock('../../api/index.js', () => ({
  butceAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    filtreli: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            {
              id: 1,
              ad: '2026 Pazarlama',
              yil: 2026,
              ay: 3,
              tur: 'GIDER',
              tutar: 50000,
              kategori: 'Pazarlama',
              aciklama: ''
            },
            {
              id: 2,
              ad: '2026 Kira Geliri',
              yil: 2026,
              ay: 3,
              tur: 'GELIR',
              tutar: 90000,
              kategori: 'Kira',
              aciklama: ''
            }
          ],
          totalElements: 2
        }
      })
    ),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: {
          butceToplam: 140000,
          gerceklesenToplam: 120000,
          sapma: -20000,
          kullanimYuzde: 85.7,
          gelirButce: 90000,
          giderButce: 50000,
          gelirGerceklesen: 80000,
          giderGerceklesen: 40000,
          kayitSayisi: 2,
          kategoriSayisi: 2,
          onayBekleyen: 0
        }
      })
    ),
    kategoriler: vi.fn(() => Promise.resolve({ data: ['Kira', 'Pazarlama'] })),
    gerceklesen: vi.fn(() =>
      Promise.resolve({
        data: {
          tur: 'GIDER',
          yil: 2026,
          ay: null,
          butceToplam: 1500,
          gerceklesenToplam: 1000,
          sapma: -500,
          kullanimYuzde: 66.7,
          satirlar: [
            { kategori: 'Pazarlama', butce: 1000, gerceklesen: 700, sapma: -300, kullanimYuzdesi: 70 },
            { kategori: 'Kira', butce: 500, gerceklesen: 300, sapma: -200, kullanimYuzdesi: 60 },
            { kategori: 'Plan', butce: 1000, gerceklesen: null, sapma: null, kullanimYuzdesi: null }
          ]
        }
      })
    ),
    gerceklesenPdf: vi.fn(() => Promise.resolve({ data: new Blob() })),
    matris: vi.fn(() =>
      Promise.resolve({
        data: {
          yil: 2026,
          tur: 'GIDER',
          satirlar: [
            {
              kategori: 'Pazarlama',
              aylar: [100, 200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
              toplam: 300
            }
          ],
          aylikToplam: [100, 200, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0],
          genelToplam: 300
        }
      })
    ),
    kopyala: vi.fn(() => Promise.resolve({ data: { kopyalanan: 4, atlanan: 2 } })),
    onayBekleyen: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            id: 3,
            ad: '2026 Yeni Bütçe',
            yil: 2026,
            ay: 4,
            tur: 'GIDER',
            kategori: 'Pazarlama',
            tutar: 75000,
            durum: 'TASLAK',
            revizyon: 1
          }
        ]
      })
    ),
    onayla: vi.fn(() => Promise.resolve({ data: {} })),
    reddet: vi.fn(() => Promise.resolve({ data: {} })),
    revizyonlar: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            id: 1,
            butceId: 1,
            revizyon: 1,
            islem: 'OLUSTUR',
            durum: 'TASLAK',
            aciklama: 'Bütçe oluşturuldu',
            tutar: 50000,
            olusturmaTarihi: '2026-01-01T10:00:00'
          }
        ]
      })
    ),
    create: vi.fn(() => Promise.resolve({ data: {} })),
    update: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  },
  excelAPI: {
    butceler: vi.fn(() => Promise.resolve({ data: new Blob() })),
    butceGerceklesen: vi.fn(() => Promise.resolve({ data: new Blob() })),
    butceMatris: vi.fn(() => Promise.resolve({ data: new Blob() }))
  }
}))

const stubs = {
  PageHeader: { props: ['title', 'description'], template: '<div class="ph">{{ title }}<slot name="actions" /></div>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>' },
  AppDataTable: { props: ['value'], template: '<div class="tablo"><slot /></div>' },
  DataTable: { props: ['value'], template: '<div><slot /></div>' },
  Column: true,
  SatirEylemleri: true,
  EmptyState: true,
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  ButceDetayDrawer: {
    props: ['visible', 'kayit', 'revizyonlar'],
    template: '<div class="drawer">{{ kayit ? kayit.ad : "" }}</div>'
  },
  Button: { template: '<button><slot /></button>' },
  Dropdown: true,
  Select: true,
  InputText: true,
  InputNumber: true,
  Textarea: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  Card: { template: '<div><slot name="title" /><slot name="content" /></div>' },
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: { template: '<div class="tab"><slot /></div>' },
  Toast: true
}

const kur = () =>
  mount(Butceler, {
    global: { plugins: [ToastService, ConfirmationService, i18n], stubs }
  })

describe('Butceler.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    window.URL.createObjectURL = vi.fn(() => 'blob:butce')
    window.URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  it('varsayılan olarak gider türüyle sunucu filtreli listeyi ve KPI’ları yükler', async () => {
    const w = kur()
    await flushPromises()

    expect(butceAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ yil: new Date().getFullYear(), tur: 'GIDER', page: 0, size: 25 })
    )
    expect(butceAPI.ozet).toHaveBeenCalled()
    expect(butceAPI.kategoriler).toHaveBeenCalled()
    expect(w.vm.list).toHaveLength(2)
    expect(w.vm.toplam).toBe(2)
    expect(w.text()).toContain('Bütçe:140000')
    expect(w.text()).toContain('85.7%')
    expect(w.find('.tur-seg button.aktif').text()).toBe('Gider')
  })

  it('tür değişince filtreyi sıfırlar ve yeniden yükler', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.turSec('GELIR')
    await flushPromises()

    expect(butceAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ tur: 'GELIR', page: 0 })
    )
    expect(w.vm.kategori).toBeNull()
  })

  it('filtreleri localStorage’a yazar', async () => {
    const w = kur()
    await flushPromises()
    w.vm.kategori = 'Pazarlama'
    w.vm.q = 'kira'
    await flushPromises()

    const kayit = JSON.parse(localStorage.getItem('raspel_butce_filtre'))
    expect(kayit.kategori).toBe('Pazarlama')
    expect(kayit.q).toBe('kira')
    expect(kayit.tur).toBe('GIDER')
  })

  it('kayıtlardan localStorage’daki filtreyi geri yükler', async () => {
    localStorage.setItem(
      'raspel_butce_filtre',
      JSON.stringify({ yil: 2025, ay: 4, tur: 'GELIR', kategori: 'Kira', q: '' })
    )
    const w = kur()
    await flushPromises()

    expect(w.vm.yil).toBe(2025)
    expect(w.vm.ay).toBe(4)
    expect(w.vm.tur).toBe('GELIR')
    expect(w.vm.kategori).toBe('Kira')
    expect(butceAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ yil: 2025, ay: 4, tur: 'GELIR', kategori: 'Kira' })
    )
  })

  it('liste sonucunu ekran okuyucuya duyurur ve iskelet gösterir', async () => {
    const w = kur()
    expect(w.vm.ozetIskelet).toBe(true)
    await flushPromises()
    expect(w.vm.ozetIskelet).toBe(false)
    const canli = w.find('.sr-only[aria-live="polite"]')
    expect(canli.exists()).toBe(true)
    expect(canli.text()).toContain('bütçe kaydından 2 tanesi')
    expect(w.find('.tur-seg').attributes('role')).toBe('group')
  })

  it('gerçekleşen sekmesi açılınca tür ayrımlı raporu yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(butceAPI.gerceklesen).not.toHaveBeenCalled()

    w.vm.aktifSekme = 1
    await flushPromises()

    expect(butceAPI.gerceklesen).toHaveBeenCalledWith({
      yil: new Date().getFullYear(),
      tur: 'GIDER'
    })
    expect(w.vm.ger.satirlar).toHaveLength(3)
    expect(w.text()).toContain('66.7%')
    expect(w.vm.ger.satirlar[2].gerceklesen).toBeNull()
  })

  it('gerçekleşen türü değişince raporu yeniler ve PDF/Excel indirir', async () => {
    const { excelAPI } = await import('../../api/index.js')
    const w = kur()
    await flushPromises()
    w.vm.aktifSekme = 1
    await flushPromises()

    vi.clearAllMocks()
    w.vm.gerTurSec('GELIR')
    await flushPromises()
    expect(butceAPI.gerceklesen).toHaveBeenCalledWith(expect.objectContaining({ tur: 'GELIR' }))

    await w.vm.gerExcelIndir()
    expect(excelAPI.butceGerceklesen).toHaveBeenCalledWith(expect.objectContaining({ tur: 'GELIR' }))

    await w.vm.gerPdfIndir()
    expect(butceAPI.gerceklesenPdf).toHaveBeenCalledWith(expect.objectContaining({ tur: 'GELIR' }))
  })

  it('son açık sekmeyi geri yükler ve açılışta raporu yükler', async () => {
    localStorage.setItem('raspel_butce_tab', '1')
    const w = kur()
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(1)
    expect(butceAPI.gerceklesen).toHaveBeenCalled()
  })

  it('matris sekmesi 12 aylık dağılımı yükler ve Excel indirir', async () => {
    const { excelAPI } = await import('../../api/index.js')
    const w = kur()
    await flushPromises()
    expect(butceAPI.matris).not.toHaveBeenCalled()

    w.vm.aktifSekme = 2
    await flushPromises()

    expect(butceAPI.matris).toHaveBeenCalledWith({ yil: new Date().getFullYear(), tur: 'GIDER' })
    expect(w.vm.matris.satirlar).toHaveLength(1)
    expect(w.vm.ayEtiketleri).toHaveLength(12)
    expect(w.vm.hucre(0)).toBe('–')
    expect(w.vm.hucre(200)).toContain('200')
    expect(w.text()).toContain('Pazarlama')

    await w.vm.matrisExcelIndir()
    expect(excelAPI.butceMatris).toHaveBeenCalledWith({ yil: new Date().getFullYear(), tur: 'GIDER' })
  })

  it('önceki yıldan kopyalama hedef yılı matrise uygular', async () => {
    const w = kur()
    await flushPromises()
    w.vm.aktifSekme = 2
    await flushPromises()

    w.vm.kopyalaDialogAc()
    expect(w.vm.kopyalaForm.kaynakYil).toBe(new Date().getFullYear() - 1)
    await w.vm.kopyalaKaydet()

    expect(butceAPI.kopyala).toHaveBeenCalledWith(
      expect.objectContaining({
        kaynakYil: new Date().getFullYear() - 1,
        hedefYil: new Date().getFullYear(),
        tur: 'GIDER'
      })
    )
    expect(butceAPI.matris).toHaveBeenLastCalledWith(
      expect.objectContaining({ yil: new Date().getFullYear() })
    )
  })

  it('durum filtresini sunucu parametrelerine ekler', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.durum = 'TASLAK'
    w.vm.filtreDegisti()
    await flushPromises()

    expect(butceAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ durum: 'TASLAK' })
    )
  })

  it('onay sekmesi taslak bütçeleri yükler ve red gerekçesini gönderir', async () => {
    const w = kur()
    await flushPromises()
    expect(butceAPI.onayBekleyen).not.toHaveBeenCalled()

    w.vm.aktifSekme = 3
    await flushPromises()

    expect(butceAPI.onayBekleyen).toHaveBeenCalledWith({ yil: new Date().getFullYear() })
    expect(w.vm.onayListe).toHaveLength(1)
    expect(w.text()).toContain('2026 Yeni Bütçe')

    w.vm.reddetAc(w.vm.onayListe[0])
    w.vm.redGerekce = 'Tutar yüksek'
    await w.vm.reddetKaydet()

    expect(butceAPI.reddet).toHaveBeenCalledWith(3, 'Tutar yüksek')
    expect(w.vm.redDialog).toBe(false)
    expect(butceAPI.onayBekleyen).toHaveBeenCalledTimes(2)
  })

  it('detay drawer revizyon geçmişini yükler', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.detayAc(w.vm.list[0])

    expect(butceAPI.revizyonlar).toHaveBeenCalledWith(1)
    expect(w.vm.detayDrawer).toBe(true)
    expect(w.vm.detayKayit.ad).toBe('2026 Pazarlama')
    expect(w.vm.detayRevizyonlar[0].islem).toBe('OLUSTUR')
  })
})
