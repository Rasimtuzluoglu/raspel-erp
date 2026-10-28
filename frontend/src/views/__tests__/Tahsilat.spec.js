import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { tahsilatAPI, excelAPI } from '../../api/index.js'
import Tahsilat from '../Tahsilat.vue'

const cariler = [
  {
    cariId: 1,
    cariAd: 'A Ltd',
    telefon: '05555555555',
    email: 'a@x.com',
    toplamAlacak: 3000,
    gecikmisAlacak: 1000,
    faturaSayisi: 2,
    maxGecikmeGunu: 10,
    aralik: '0-30 Gün',
    faturalar: [
      { faturaId: 1, faturaNumarasi: 'F-1', vadeTarihi: '2026-08-19', kalanTutar: 1000, gecikmeGunu: 10 }
    ]
  },
  {
    cariId: 2,
    cariAd: 'B Ltd',
    telefon: null,
    email: null,
    toplamAlacak: 3000,
    gecikmisAlacak: 3000,
    faturaSayisi: 1,
    maxGecikmeGunu: 45,
    aralik: '31-60 Gün',
    faturalar: [
      { faturaId: 2, faturaNumarasi: 'F-2', vadeTarihi: '2026-07-15', kalanTutar: 3000, gecikmeGunu: 45 }
    ]
  }
]

const islem = {
  id: 9,
  tarih: '2026-10-01',
  cariId: 1,
  cariAd: 'A Ltd',
  tutar: 3000,
  odemeYontemi: 'NAKIT',
  kasaAd: 'Merkez Kasa',
  durum: 'AKTIF',
  satirSayisi: 1,
  dagilimlar: [
    { hareketId: 51, faturaId: 1, faturaNumarasi: 'F-1', tutar: 3000, aciklama: 'Tahsilat', iptal: false }
  ]
}

vi.mock('../../api/index.js', () => ({
  tahsilatAPI: {
    ozet: vi.fn(() => Promise.resolve({ data: {} })),
    filtreliOzet: vi.fn(() =>
      Promise.resolve({
        data: {
          toplamAlacak: 6000,
          vadesiGecmisToplam: 4000,
          vadesiYaklasanToplam: 2000,
          acikFaturaSayisi: 3,
          gecikmisCariSayisi: 2,
          cariler
        }
      })
    ),
    performans: vi.fn(() =>
      Promise.resolve({
        data: {
          hedef: 200000,
          tahsilEdilen: 150000,
          gerceklesmeYuzde: 75,
          tahsilatSayisi: 12,
          vadesiGecmisToplam: 90000,
          trend: [
            { ay: '2026-05', tahsilat: 100000, hedef: 200000 },
            { ay: '2026-06', tahsilat: 150000, hedef: 200000 }
          ]
        }
      })
    ),
    gecmisFiltreli: vi.fn(() =>
      Promise.resolve({ data: { content: [islem], totalElements: 1 } })
    ),
    hatirlat: vi.fn(() => Promise.resolve({ data: { gonderilen: 1 } })),
    topluHatirlat: vi.fn(() => Promise.resolve({ data: { gonderilen: 1, atlanan: 1, hatalar: [] } })),
    iptal: vi.fn(() => Promise.resolve({ data: { ...islem, durum: 'IPTAL' } })),
    hedefKaydet: vi.fn(() => Promise.resolve({ data: {} })),
    gir: vi.fn(() => Promise.resolve({ data: {} }))
  },
  excelAPI: {
    tahsilatYaslandirma: vi.fn(() => Promise.resolve({ data: new Blob() })),
    tahsilatGecmis: vi.fn(() => Promise.resolve({ data: new Blob() }))
  }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'description'],
    template: '<div class="ph"><span class="ph-title">{{ title }}</span><slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger', 'paraBirimi'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /><slot name="alt" /></div>'
  },
  AppDataTable: {
    props: ['value', 'selection', 'loading', 'totalRecords', 'expandedRows'],
    emits: ['update:selection', 'update:expandedRows', 'page', 'sort'],
    template:
      '<div class="tablo"><slot name="batch-actions" /><slot name="empty" /><div v-for="satir in value || []" :key="satir.id || satir.cariId" class="satir">{{ satir.cariAd || satir.cariId }}</div><slot /></div>'
  },
  Column: true,
  SatirEylemleri: true,
  EmptyState: true,
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: { template: '<div class="tab"><slot /></div>' },
  Button: { template: '<button><slot /></button>' },
  Select: true,
  InputText: true,
  InputNumber: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  DatePicker: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Dialog: {
    props: ['visible', 'header'],
    template: '<div class="dialog"><span class="dialog-header">{{ header }}</span><slot /><slot name="footer" /></div>'
  },
  Textarea: true,
  FormField: { props: ['label'], template: '<div><label>{{ label }}</label><slot /></div>' },
  TahsilatGirDialog: {
    props: ['visible', 'cariler', 'baslangicCariId'],
    template: '<div class="tahsilat-gir" />'
  },
  TahsilatCariDrawer: {
    props: ['visible', 'cari', 'sonTahsilatlar'],
    template: '<div class="cari-drawer">{{ cari ? cari.cariAd : "" }}</div>'
  }
}

const kur = () =>
  mount(Tahsilat, {
    global: { plugins: [createPinia(), ToastService, ConfirmationService, i18n], stubs }
  })

describe('Tahsilat.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    window.URL.createObjectURL = vi.fn(() => 'blob:tahsilat')
    window.URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  it('5 KPI, sunucu filtreli özet ve performans verisini yükler', async () => {
    const w = kur()
    await flushPromises()

    expect(tahsilatAPI.filtreliOzet).toHaveBeenCalledWith({})
    expect(tahsilatAPI.performans).toHaveBeenCalledWith({
      yil: new Date().getFullYear(),
      ay: new Date().getMonth() + 1
    })
    expect(w.findAll('.kpi')).toHaveLength(5)
    expect(w.text()).toContain('Tahsilat Merkezi')
    expect(w.text()).toContain('Bu Ay Tahsilat')
    expect(w.text()).toContain('Hedef Gerçekleşme')
    expect(w.text()).toContain('75%')
    expect(w.vm.hedefGerceklesme).toBe(75)
    expect(w.vm.ayAdi('2026-05')).not.toBe('')
  })

  it('yaşlandırma filtrelerini sunucu parametrelerine ekler ve hafızaya yazar', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.aralik = '61-90'
    w.vm.sadeceGecikmis = false
    w.vm.minTutar = 500
    w.vm.q = 'A Ltd'
    w.vm.filtreDegisti()
    await flushPromises()

    expect(tahsilatAPI.filtreliOzet).toHaveBeenCalledWith({
      q: 'A Ltd',
      aralik: '61-90',
      minTutar: 500
    })
    const kayit = JSON.parse(localStorage.getItem('raspel_tahsilat_filtre'))
    expect(kayit.q).toBe('A Ltd')
    expect(kayit.aralik).toBe('61-90')
    expect(kayit.minTutar).toBe(500)
  })

  it('kayıtlı filtreyi ve sekmeyi geri yükler', async () => {
    localStorage.setItem(
      'raspel_tahsilat_filtre',
      JSON.stringify({ q: 'B', aralik: '90+', sadeceGecikmis: true })
    )
    localStorage.setItem('raspel_tahsilat_tab', '1')

    const w = kur()
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(1)
    expect(w.vm.q).toBe('B')
    expect(w.vm.aralik).toBe('90+')
    expect(tahsilatAPI.filtreliOzet).toHaveBeenCalledWith(
      expect.objectContaining({ q: 'B', aralik: '90+', sadeceGecikmis: true })
    )
    expect(tahsilatAPI.gecmisFiltreli).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 20, sort: 'tarih,desc' })
    )
  })

  it('KPI tıklaması ilgili sekmeye ve filtreye götürür', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.kpiTikla('gecikmis')
    await flushPromises()
    expect(w.vm.aktifSekme).toBe(0)
    expect(w.vm.sadeceGecikmis).toBe(true)
    expect(tahsilatAPI.filtreliOzet).toHaveBeenCalledWith({ sadeceGecikmis: true })

    w.vm.kpiTikla('hedef')
    expect(w.vm.aktifSekme).toBe(2)

    w.vm.kpiTikla('buAy')
    await flushPromises()
    expect(w.vm.aktifSekme).toBe(1)
    expect(tahsilatAPI.gecmisFiltreli).toHaveBeenLastCalledWith(
      expect.objectContaining({
        baslangic: expect.stringMatching(/^\d{4}-\d{2}-01$/),
        page: 0
      })
    )
  })

  it('geçmiş sunucu lazy sayfalama ve iptal akışını yönetir', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.aktifSekme = 1
    await flushPromises()
    expect(tahsilatAPI.gecmisFiltreli).toHaveBeenCalledWith(
      expect.objectContaining({ page: 0, size: 20, sort: 'tarih,desc' })
    )
    expect(w.vm.gecmis).toHaveLength(1)

    w.vm.gecmisDurum = 'IPTAL'
    w.vm.gecmisFiltreDegisti()
    await flushPromises()
    expect(tahsilatAPI.gecmisFiltreli).toHaveBeenLastCalledWith(
      expect.objectContaining({ durum: 'IPTAL' })
    )

    w.vm.iptalAc(islem)
    w.vm.iptalGerekce = 'ab'
    await w.vm.iptalKaydet()
    expect(tahsilatAPI.iptal).not.toHaveBeenCalled()

    w.vm.iptalGerekce = 'Hatalı kayıt'
    await w.vm.iptalKaydet()
    expect(tahsilatAPI.iptal).toHaveBeenCalledWith(9, 'Hatalı kayıt')
    expect(w.vm.iptalDialog).toBe(false)
  })

  it('seçili carilere toplu e-posta gönderir ve sonucu gösterir', async () => {
    const w = kur()
    await flushPromises()

    w.vm.seciliCariler = [cariler[0], cariler[1]]
    w.vm.topluEpostaAc()
    expect(w.vm.topluDialog).toBe(true)
    expect(w.vm.seciliEpostali).toHaveLength(1)
    expect(w.vm.seciliEpostasiz).toHaveLength(1)

    await w.vm.topluGonder()
    expect(tahsilatAPI.topluHatirlat).toHaveBeenCalledWith([1, 2])
    expect(w.vm.topluSonuc.gonderilen).toBe(1)
    expect(w.vm.topluSonuc.atlanan).toBe(1)
  })

  it('performans hedefini düzenler ve kaydeder', async () => {
    const w = kur()
    await flushPromises()
    w.vm.aktifSekme = 2
    await flushPromises()
    vi.clearAllMocks()

    w.vm.hedefDuzenle()
    expect(w.vm.hedefDialog).toBe(true)
    expect(w.vm.hedefTutar).toBe(200000)

    w.vm.hedefTutar = 250000
    await w.vm.hedefKaydet()

    expect(tahsilatAPI.hedefKaydet).toHaveBeenCalledWith({
      yil: new Date().getFullYear(),
      ay: new Date().getMonth() + 1,
      tutar: 250000
    })
    expect(w.vm.hedefDialog).toBe(false)
    expect(tahsilatAPI.performans).toHaveBeenCalled()
  })

  it('cari drawer açılınca son tahsilatları yükler', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    await w.vm.cariAc(cariler[0])

    expect(tahsilatAPI.gecmisFiltreli).toHaveBeenCalledWith({
      cariId: 1,
      page: 0,
      size: 5,
      sort: 'tarih,desc'
    })
    expect(w.vm.drawerAcik).toBe(true)
    expect(w.vm.sonTahsilatlar).toHaveLength(1)
    expect(w.find('.cari-drawer').text()).toContain('A Ltd')
  })

  it('yaşlandırma Excel’ini filtrelerle indirir', async () => {
    const w = kur()
    await flushPromises()

    w.vm.aralik = '0-30'
    await w.vm.excelYaslandirmaIndir()

    expect(excelAPI.tahsilatYaslandirma).toHaveBeenCalledWith({ aralik: '0-30' })
  })
})
