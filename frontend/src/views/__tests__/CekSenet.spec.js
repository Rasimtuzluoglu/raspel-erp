import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'
import { cekSenetAPI, excelAPI } from '../../api/index.js'
import CekSenet from '../CekSenet.vue'

const gunStr = (fark = 0) => {
  const d = new Date()
  d.setHours(0, 0, 0, 0)
  d.setDate(d.getDate() + fark)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const liste = [
  {
    id: 1,
    tur: 'CEK',
    cariHesapId: 7,
    cariHesapAdi: 'Acme Ltd.',
    bankaAdi: 'Ziraat',
    cekNo: 'C-1001',
    vadeTarihi: gunStr(10),
    tutar: 12500,
    durum: 'PORTFOY'
  },
  {
    id: 2,
    tur: 'SENET',
    cariHesapId: 8,
    cariHesapAdi: 'Beta A.Ş.',
    bankaAdi: '',
    cekNo: 'S-2002',
    vadeTarihi: gunStr(-40),
    tutar: 8000,
    durum: 'TAHSIL_EDILDI'
  }
]

// Yaşlandırma: 90+ gecikme, 31-60 gecikme, bu hafta ve kapanmış kayıt.
const agingKayitlar = [
  { id: 11, tur: 'CEK', cariHesapAdi: 'Gecikmiş 90', vadeTarihi: gunStr(-100), tutar: 100, durum: 'PORTFOY' },
  { id: 12, tur: 'CEK', cariHesapAdi: 'Gecikmiş 45', vadeTarihi: gunStr(-45), tutar: 200, durum: 'TEMINATTA' },
  { id: 13, tur: 'SENET', cariHesapAdi: 'Bu hafta', vadeTarihi: gunStr(3), tutar: 300, durum: 'PORTFOY' },
  { id: 14, tur: 'CEK', cariHesapAdi: 'Tahsil edildi', vadeTarihi: gunStr(-200), tutar: 999, durum: 'TAHSIL_EDILDI' }
]

const detay = (id) => {
  const kayit = liste.find((k) => k.id === id) || {}
  return {
    ...kayit,
    faturaId: 55,
    faturaNo: 'FTR-2026-001',
    gecmis: [
      {
        id: 1,
        islem: 'OLUSTUR',
        eskiDurum: null,
        yeniDurum: 'PORTFOY',
        aciklama: 'Kayıt oluşturuldu',
        olusturmaTarihi: `${gunStr(-2)}T10:00:00`
      }
    ]
  }
}

vi.mock('../../api/index.js', () => ({
  cekSenetAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [] } })),
    filtreli: vi.fn((params = {}) => {
      const veri = params.size === 500 ? agingKayitlar : liste
      return Promise.resolve({ data: { content: veri, totalElements: veri.length } })
    }),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: {
          portfoy: 12,
          teminatta: 3,
          vadesiGecen: 2,
          buAyVadesiGelen: 4,
          protesto: 1,
          toplamTutar: 250000
        }
      })
    ),
    takvim: vi.fn(() =>
      Promise.resolve({
        data: [{ tarih: gunStr(0), adet: 2, tutar: 5000, kayitlar: [] }]
      })
    ),
    getById: vi.fn((id) => Promise.resolve({ data: detay(id) })),
    create: vi.fn(() => Promise.resolve({ data: {} })),
    update: vi.fn(() => Promise.resolve({ data: {} })),
    durumGuncelle: vi.fn(() => Promise.resolve({ data: {} })),
    tahsilIptal: vi.fn(() => Promise.resolve({ data: {} })),
    ciro: vi.fn(() => Promise.resolve({ data: {} })),
    ciroIade: vi.fn(() => Promise.resolve({ data: {} })),
    iadeAl: vi.fn(() => Promise.resolve({ data: {} })),
    teminat: vi.fn(() => Promise.resolve({ data: {} })),
    teminatIade: vi.fn(() => Promise.resolve({ data: {} })),
    protesto: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  },
  cariHesapAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [{ id: 7, ad: 'Acme Ltd.' }, { id: 8, ad: 'Beta A.Ş.' }] } }))
  },
  kasaAPI: { getAll: vi.fn(() => Promise.resolve({ data: [{ id: 9, ad: 'Merkez Kasa' }] })) },
  bankaAPI: { getAll: vi.fn(() => Promise.resolve({ data: [{ id: 5, ad: 'Ziraat Vadesiz' }] })) },
  faturaAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: [{ id: 55, faturaNo: 'FTR-2026-001' }] } }))
  },
  excelAPI: { cekSenet: vi.fn(() => Promise.resolve({ data: new Blob() })) }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'description'],
    template: '<div class="ph">{{ title }}<slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="alt" /></div>'
  },
  AppDataTable: { props: ['value'], template: '<div class="tablo"><slot /></div>' },
  DataTable: { props: ['value'], template: '<div><slot /></div>' },
  Column: true,
  SatirEylemleri: true,
  EmptyState: true,
  SkeletonLoader: { template: '<div class="skeleton-stub" />' },
  CekSenetDetayDrawer: {
    props: ['visible', 'kayit'],
    template: '<div class="drawer">{{ kayit ? kayit.cekNo : "" }}</div>'
  },
  Button: { props: ['label'], template: '<button class="btn">{{ label }}</button>' },
  Dropdown: true,
  Select: true,
  InputText: true,
  InputNumber: true,
  Textarea: true,
  DatePicker: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Dialog: { template: '<div class="dialog"><slot /><slot name="footer" /></div>' },
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: { template: '<div class="tab"><slot /></div>' },
  Toast: true
}

const kur = () =>
  mount(CekSenet, {
    global: { plugins: [ToastService, ConfirmationService, i18n], stubs }
  })

describe('CekSenet.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    window.URL.createObjectURL = vi.fn(() => 'blob:cek-senet')
    window.URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  it('Portföy sekmesiyle açılır; 5 KPI ve sunucu filtreli listeyi yükler', async () => {
    const w = kur()
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(0)
    expect(cekSenetAPI.ozet).toHaveBeenCalled()
    expect(cekSenetAPI.filtreli).toHaveBeenCalledWith({
      page: 0,
      size: 25,
      sort: 'vadeTarihi,asc'
    })
    expect(w.vm.list).toHaveLength(2)
    expect(w.vm.toplam).toBe(2)

    const metin = w.text()
    expect(metin).toContain('Portföy:12')
    expect(metin).toContain('Teminatta:3')
    expect(metin).toContain('Vadesi Geçen:2')
    expect(metin).toContain('Bu Ay Vadesi Gelen:4')
    expect(metin).toContain('Protesto:1')
    expect(metin).toContain('Toplam tutar')
  })

  it('KPI iskeleti özet gelene kadar gösterilir ve liste duyurusu aria-live ile verilir', async () => {
    const w = kur()
    expect(w.vm.ozetIskelet).toBe(true)
    await flushPromises()
    expect(w.vm.ozetIskelet).toBe(false)

    const canli = w.find('.sr-only[aria-live="polite"]')
    expect(canli.exists()).toBe(true)
    expect(canli.text()).toContain('2 çek/senet kaydı')
    expect(w.find('.tur-seg').attributes('role')).toBe('group')
    expect(w.findAll('.tur-seg button')[0].attributes('aria-pressed')).toBe('true')
  })

  it('sekmeleri yükler: vade takvimi ve geçmiş', async () => {
    const w = kur()
    await flushPromises()
    expect(cekSenetAPI.takvim).not.toHaveBeenCalled()

    w.vm.aktifSekme = 1
    await flushPromises()
    expect(cekSenetAPI.takvim).toHaveBeenCalledWith(
      expect.objectContaining({ baslangic: expect.any(String), bitis: expect.any(String) })
    )
    expect(w.vm.takvimVerisi).toHaveLength(1)

    w.vm.aktifSekme = 2
    await flushPromises()
    await flushPromises()
    expect(cekSenetAPI.getById).toHaveBeenCalled()
    expect(w.vm.gecmisOlaylari.length).toBeGreaterThan(0)
    expect(w.vm.gecmisOlaylari[0].islem).toBe('OLUSTUR')
  })

  it('son açık sekmeyi hafızadan geri yükler', async () => {
    localStorage.setItem('raspel_cek_senet_tab', '1')
    const w = kur()
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(1)
    expect(cekSenetAPI.takvim).toHaveBeenCalled()
  })

  it('filtreleri sunucu parametrelerine ekler ve hafızaya yazar', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.tur = 'CEK'
    w.vm.durum = 'PORTFOY'
    w.vm.cariId = 7
    w.vm.minTutar = 1000
    w.vm.vadeAralik = '31-60'
    w.vm.sadeceProtesto = true
    w.vm.filtreDegisti()
    await flushPromises()

    expect(cekSenetAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({
        tur: 'CEK',
        durum: 'PORTFOY',
        cariId: 7,
        minTutar: 1000,
        vadeAralik: '31-60',
        protesto: true,
        page: 0
      })
    )

    const kayit = JSON.parse(localStorage.getItem('raspel_cek_senet_filtre'))
    expect(kayit).toMatchObject({
      tur: 'CEK',
      durum: 'PORTFOY',
      cariId: 7,
      minTutar: 1000,
      vadeAralik: '31-60',
      protesto: true
    })
  })

  it('kayıtlı filtreyi geri yükler ve vade aralığını gönderir', async () => {
    localStorage.setItem(
      'raspel_cek_senet_filtre',
      JSON.stringify({
        tur: 'SENET',
        q: 'S-2002',
        baslangic: '2026-01-01',
        bitis: '2026-01-31',
        minTutar: 500,
        protesto: true
      })
    )
    const w = kur()
    await flushPromises()

    expect(w.vm.tur).toBe('SENET')
    expect(w.vm.q).toBe('S-2002')
    expect(cekSenetAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({
        tur: 'SENET',
        q: 'S-2002',
        baslangic: '2026-01-01',
        bitis: '2026-01-31',
        minTutar: 500,
        protesto: true
      })
    )
  })

  it('filtre temizleme tüm sunucu filtrelerini sıfırlar', async () => {
    const w = kur()
    await flushPromises()
    w.vm.q = 'abc'
    w.vm.tur = 'SENET'
    w.vm.durum = 'PORTFOY'
    w.vm.filtreTemizle()
    await flushPromises()

    expect(w.vm.q).toBe('')
    expect(w.vm.tur).toBe('TUMU')
    expect(w.vm.durum).toBeNull()
    expect(cekSenetAPI.filtreli).toHaveBeenLastCalledWith({
      page: 0,
      size: 25,
      sort: 'vadeTarihi,asc'
    })
  })

  it('sayfa ve sıralama değişimini sunucuya iletir', async () => {
    const w = kur()
    await flushPromises()
    vi.clearAllMocks()

    w.vm.sayfaDegisti({ page: 2, rows: 50 })
    await flushPromises()
    expect(cekSenetAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ page: 2, size: 50 })
    )

    w.vm.siralaDegisti({ sortField: 'tutar', sortOrder: -1 })
    await flushPromises()
    expect(cekSenetAPI.filtreli).toHaveBeenLastCalledWith(
      expect.objectContaining({ page: 0, sort: 'tutar,desc' })
    )
  })

  it('tahsilatı fatura ve kasa ile kaydeder', async () => {
    const w = kur()
    await flushPromises()

    w.vm.tahsilDialogAc(w.vm.list[0])
    await flushPromises()
    expect(w.vm.tahsilDialog).toBe(true)
    expect(w.vm.tahsilFaturalar).toHaveLength(1)

    w.vm.tahsilHesapTipi = 'KASA'
    w.vm.tahsilKasaId = 9
    w.vm.tahsilFaturaId = 55
    await w.vm.tahsilEt()

    expect(cekSenetAPI.durumGuncelle).toHaveBeenCalledWith(1, {
      durum: 'TAHSIL_EDILDI',
      faturaId: 55,
      kasaId: 9,
      bankaId: null
    })
    expect(w.vm.tahsilDialog).toBe(false)
  })

  it('tahsil iptali gerekçe doğrulaması yapar ve API çağırır', async () => {
    const w = kur()
    await flushPromises()

    w.vm.iptalDialogAc(w.vm.list[1])
    w.vm.iptalGerekce = 'ab'
    await w.vm.iptalKaydet()
    expect(cekSenetAPI.tahsilIptal).not.toHaveBeenCalled()
    expect(w.vm.iptalHata).not.toBe('')

    w.vm.iptalGerekce = 'Mükerrer tahsilat'
    await w.vm.iptalKaydet()
    expect(cekSenetAPI.tahsilIptal).toHaveBeenCalledWith(2, 'Mükerrer tahsilat')
    expect(w.vm.iptalDialog).toBe(false)
  })

  it('ciro hedefi zorunludur ve ciro/teminat/protesto uçlarını çağırır', async () => {
    const w = kur()
    await flushPromises()

    w.vm.ciroDialogAc(w.vm.list[0])
    await w.vm.ciroKaydet()
    expect(cekSenetAPI.ciro).not.toHaveBeenCalled()
    expect(w.vm.ciroHata).not.toBe('')

    w.vm.ciroForm.cariId = 8
    w.vm.ciroForm.not = 'Tedarikçiye'
    await w.vm.ciroKaydet()
    expect(cekSenetAPI.ciro).toHaveBeenCalledWith(1, { cariId: 8, not: 'Tedarikçiye' })

    w.vm.teminatDialogAc(w.vm.list[0])
    w.vm.teminatAciklama = 'Bankaya'
    await w.vm.teminatKaydet()
    expect(cekSenetAPI.teminat).toHaveBeenCalledWith(1, 'Bankaya')

    w.vm.protestoDialogAc(w.vm.list[0])
    w.vm.protestoAciklama = 'Karşılıksız'
    await w.vm.protestoKaydet()
    expect(cekSenetAPI.protesto).toHaveBeenCalledWith(1, 'Karşılıksız')
  })

  it('vade takvimi yaşlandırma kovalarını hesaplar ve güne tıklayınca listeyi filtreler', async () => {
    const w = kur()
    await flushPromises()

    w.vm.aktifSekme = 1
    await flushPromises()

    const y = w.vm.yaslandirma
    expect(w.vm.agingKartlari).toHaveLength(6)
    expect(y.buHafta.adet).toBe(1)
    expect(y.geciken.adet).toBe(2)
    expect(y.g30.adet).toBe(1)
    expect(y.g90.adet).toBe(1)
    expect(y.geciken.tutar).toBe(300)

    expect(w.vm.takvimHucreleri.length % 7).toBe(0)
    expect(w.vm.doluGunler).toHaveLength(1)

    vi.clearAllMocks()
    w.vm.gunSec({ tarih: gunStr(0) })
    await flushPromises()

    expect(w.vm.aktifSekme).toBe(0)
    expect(cekSenetAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ baslangic: gunStr(0), bitis: gunStr(0), page: 0 })
    )
  })

  it('yaşlandırma kartına tıklayınca ilgili vade aralığı filtrelenir', async () => {
    const w = kur()
    await flushPromises()
    w.vm.aktifSekme = 1
    await flushPromises()

    vi.clearAllMocks()
    const geciken = w.vm.agingKartlari.find((k) => k.anahtar === 'geciken')
    w.vm.agingSec(geciken)
    await flushPromises()

    expect(w.vm.vadeAralik).toBe('GECIKEN')
    expect(w.vm.aktifSekme).toBe(0)
    expect(cekSenetAPI.filtreli).toHaveBeenCalledWith(
      expect.objectContaining({ vadeAralik: 'GECIKEN', page: 0 })
    )

    const g90 = w.vm.agingKartlari.find((k) => k.anahtar === 'g90')
    w.vm.agingSec(g90)
    await flushPromises()
    expect(w.vm.vadeAralik).toBeNull()
    expect(cekSenetAPI.filtreli).toHaveBeenLastCalledWith(
      expect.objectContaining({ bitis: gunStr(-91), page: 0 })
    )
  })

  it('Excel indirir ve hata durumunda bildirir', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.excelIndir()
    expect(excelAPI.cekSenet).toHaveBeenCalledWith({})

    excelAPI.cekSenet.mockRejectedValueOnce(new Error('boom'))
    await w.vm.excelIndir()
    expect(excelAPI.cekSenet).toHaveBeenCalledTimes(2)
  })

  it('detay drawer için kaydı getById ile zenginleştirir', async () => {
    const w = kur()
    await flushPromises()

    await w.vm.detayAc(w.vm.list[0])

    expect(cekSenetAPI.getById).toHaveBeenCalledWith(1)
    expect(w.vm.detayDrawer).toBe(true)
    expect(w.vm.detayKayit.gecmis).toHaveLength(1)
    expect(w.vm.detayYukleniyor).toBe(false)
  })
})
