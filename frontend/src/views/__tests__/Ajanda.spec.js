import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { ajandaAPI } from '../../api/index.js'
import Ajanda from '../Ajanda.vue'

const pushMock = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return { ...actual, useRouter: () => ({ push: pushMock }) }
})

vi.mock('../../api/index.js', () => ({
  ajandaAPI: {
    olaylar: vi.fn(() => Promise.resolve({ data: [] })),
    ozet: vi.fn(() =>
      Promise.resolve({
        data: { bugunOlay: 3, buAyVadeToplam: 12500, bekleyenGorev: 4, gecikmisGorev: 2 }
      })
    ),
    yaklasan: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            tarih: '2026-11-02',
            tip: 'VADE',
            baslik: 'FTR-9 vadesi',
            aciklama: 'Müşteri A · Kalan: 1500',
            kaynakId: 9,
            kaynakTip: 'FATURA',
            kaynakNo: 'FTR-9'
          },
          {
            tarih: '2026-11-05',
            tip: 'PROJE_GOREVI',
            baslik: 'Proje görevi',
            aciklama: 'Durum: YAPILACAK',
            kaynakId: 3,
            kaynakTip: 'PROJE_GOREVI'
          }
        ]
      })
    ),
    gorevler: vi.fn(() =>
      Promise.resolve({
        data: [
          {
            id: 1,
            baslik: 'Gecikmiş görev',
            durum: 'BEKLIYOR',
            oncelik: 'YUKSEK',
            bitisTarihi: '2020-01-01',
            aciklama: 'Eski iş'
          },
          {
            id: 2,
            baslik: 'Gelecek görev',
            durum: 'BEKLIYOR',
            oncelik: 'ORTA',
            bitisTarihi: '2099-01-01',
            aciklama: ''
          },
          {
            id: 3,
            baslik: 'Tamamlanan görev',
            durum: 'TAMAMLANDI',
            oncelik: 'DUSUK',
            bitisTarihi: '2099-01-01',
            aciklama: ''
          }
        ]
      })
    ),
    hatirlaticilar: vi.fn(() =>
      Promise.resolve({
        data: [{ id: 5, baslik: 'Toplantı', hatirlatmaZamani: '2099-01-01T10:00:00' }]
      })
    ),
    gorevOlustur: vi.fn(),
    gorevGuncelle: vi.fn(),
    gorevTamamla: vi.fn(),
    gorevSil: vi.fn(),
    hatirlaticiOlustur: vi.fn(),
    hatirlaticiSil: vi.fn()
  },
  excelAPI: { ajanda: vi.fn(() => Promise.resolve({ data: new Blob() })) }
}))

const stubs = {
  PageHeader: {
    props: ['title', 'subtitle', 'icon'],
    template: '<div class="ph">{{ title }}<slot name="actions" /></div>'
  },
  KpiKart: {
    props: ['baslik', 'deger'],
    template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>'
  },
  TabView: { template: '<div class="tabs"><slot /></div>' },
  TabPanel: { template: '<div class="tab"><slot /></div>' },
  EmptyState: true,
  SkeletonLoader: true,
  Checkbox: true,
  Column: true,
  AppDataTable: { props: ['value'], template: '<div class="adt"><slot /></div>' },
  SatirEylemleri: {
    props: ['gorunur', 'items', 'baslik'],
    template: '<div class="satir-eylem"><button class="duzenle" @click="$emit(\'duzenle\')">d</button><button class="sil" @click="$emit(\'sil\')">s</button></div>'
  },
  AjandaGorevDetayDrawer: {
    props: ['visible', 'kayit', 'kayitTuru'],
    template: '<div class="drawer">{{ kayit ? kayit.baslik : "" }}</div>'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: { template: '<button><slot /></button>' },
  Dialog: { template: '<div><slot /><slot name="footer" /></div>' },
  InputText: true,
  Textarea: true,
  Dropdown: true,
  DatePicker: true
}

const kur = () =>
  mount(Ajanda, {
    global: {
      plugins: [ToastService, i18n],
      stubs
    }
  })

describe('Ajanda.vue', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.URL.createObjectURL = vi.fn(() => 'blob:ajanda')
    window.URL.revokeObjectURL = vi.fn()
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => {})
  })

  it('olayları, görevleri, hatırlatıcıları, yaklaşanları ve KPI özetini yükler', async () => {
    const w = kur()
    await flushPromises()
    expect(ajandaAPI.olaylar).toHaveBeenCalledTimes(1)
    expect(ajandaAPI.gorevler).toHaveBeenCalledTimes(1)
    expect(ajandaAPI.hatirlaticilar).toHaveBeenCalledTimes(1)
    expect(ajandaAPI.yaklasan).toHaveBeenCalledWith(30)
    expect(ajandaAPI.ozet).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Bugünkü Olay:3')
    expect(w.text()).toContain('Bu Ay Vadesi Gelen:12500')
    expect(w.text()).toContain('Bekleyen Görev:4')
    expect(w.text()).toContain('Gecikmiş Görev:2')
  })

  it('tip filtresi seçilince yaklaşan listesini süzer', async () => {
    const w = kur()
    await flushPromises()
    expect(w.vm.yaklasanOlaylar).toHaveLength(2)
    w.vm.yaklasanTip = 'VADE'
    await flushPromises()
    expect(w.vm.yaklasanOlaylar).toHaveLength(1)
    expect(w.vm.yaklasanOlaylar[0].baslik).toBe('FTR-9 vadesi')
  })

  it('fatura olayında fatura numarasıyla faturalara gider', async () => {
    const w = kur()
    await flushPromises()
    w.vm.olayaGit(w.vm.yaklasanOlaylar[0])
    expect(pushMock).toHaveBeenCalledWith({ path: '/faturalar', query: { no: 'FTR-9' } })
  })

  it('proje görevinde projelere, kişisel görevde görev sekmesine gider', async () => {
    const w = kur()
    await flushPromises()
    w.vm.olayaGit(w.vm.yaklasanOlaylar[1])
    expect(pushMock).toHaveBeenCalledWith('/projeler')
    w.vm.olayaGit({ kaynakTip: 'KISISEL_GOREV' })
    expect(w.vm.aktifTab).toBe(2)
    w.vm.olayaGit({ kaynakTip: 'HATIRLATICI' })
    expect(w.vm.aktifTab).toBe(3)
  })

  it('Excel indirirken gün ve tip parametrelerini gönderir', async () => {
    const { excelAPI } = await import('../../api/index.js')
    const w = kur()
    await flushPromises()
    w.vm.yaklasanGun = 60
    w.vm.yaklasanTip = 'VADE'
    await w.vm.excelIndir()
    expect(excelAPI.ajanda).toHaveBeenCalledWith({ gun: 60, tip: 'VADE' })
  })

  it('görevleri gecikmiş önce sıralar ve durum filtresi uygular', async () => {
    const w = kur()
    await flushPromises()
    expect(w.vm.filtrelenmisGorevler).toHaveLength(3)
    expect(w.vm.filtrelenmisGorevler[0].id).toBe(1)

    w.vm.gorevDurumFiltre = 'GECIKMIS'
    await flushPromises()
    expect(w.vm.filtrelenmisGorevler).toHaveLength(1)
    expect(w.vm.filtrelenmisGorevler[0].baslik).toBe('Gecikmiş görev')

    w.vm.gorevDurumFiltre = 'TAMAMLANDI'
    await flushPromises()
    expect(w.vm.filtrelenmisGorevler).toHaveLength(1)
    expect(w.vm.filtrelenmisGorevler[0].id).toBe(3)
  })

  it('görev araması başlık ve açıklamada çalışır', async () => {
    const w = kur()
    await flushPromises()
    w.vm.gorevArama = 'eski'
    await flushPromises()
    expect(w.vm.filtrelenmisGorevler).toHaveLength(1)
    expect(w.vm.filtrelenmisGorevler[0].id).toBe(1)
  })

  it('görev düzenleme formu doldurur ve PUT ile kaydeder', async () => {
    const w = kur()
    await flushPromises()
    w.vm.gorevDuzenleAc(w.vm.gorevler[0])
    expect(w.vm.duzenlenenGorevId).toBe(1)
    expect(w.vm.gorevForm.baslik).toBe('Gecikmiş görev')
    expect(w.vm.gorevForm.oncelik).toBe('YUKSEK')
    w.vm.gorevForm.baslik = 'Güncel görev'
    await w.vm.gorevKaydet()
    expect(ajandaAPI.gorevGuncelle).toHaveBeenCalledWith(1, {
      baslik: 'Güncel görev',
      bitisTarihi: '2020-01-01',
      oncelik: 'YUKSEK',
      aciklama: 'Eski iş'
    })
    expect(w.vm.gorevDialogAc).toBe(false)
    expect(w.vm.duzenlenenGorevId).toBeNull()
  })

  it('görev detay drawer’ı açılır ve tamamla durumu yansır', async () => {
    const w = kur()
    await flushPromises()
    w.vm.gorevDetayAc(w.vm.gorevler[1])
    expect(w.vm.detayDrawerAc).toBe(true)
    expect(w.vm.detayKayit.id).toBe(2)
    await w.vm.gorevDurumDegistir(w.vm.detayKayit)
    expect(ajandaAPI.gorevTamamla).toHaveBeenCalledWith(2)
    expect(w.vm.detayKayit.durum).toBe('TAMAMLANDI')
  })

  it('kişisel görev olayında detay drawer’ı açılır', async () => {
    const w = kur()
    await flushPromises()
    w.vm.olayaGit({ kaynakTip: 'KISISEL_GOREV', kaynakId: 2 })
    expect(w.vm.detayDrawerAc).toBe(true)
    expect(w.vm.detayKayitTuru).toBe('gorev')
    expect(w.vm.detayKayit.id).toBe(2)
  })

  it('takvim tip çipleri olay listesini süzer', async () => {
    const w = kur()
    await flushPromises()
    expect(w.vm.takvimTipleri).toHaveLength(4)
    w.vm.takvimTipSec('VADE')
    await flushPromises()
    expect(w.vm.takvimTipFiltre).toBe('VADE')
    w.vm.takvimTipSec('VADE')
    await flushPromises()
    expect(w.vm.takvimTipFiltre).toBe('TUMU')
  })

  it('takvim haftalara ayrılır ve gün hücreleri klavyeyle seçilebilir', async () => {
    const w = kur()
    await flushPromises()
    expect(w.vm.haftalar.length).toBe(Math.ceil(w.vm.gunler.length / 7))
    expect(w.vm.haftalar.every((h) => h.length <= 7)).toBe(true)

    const hucreler = w.findAll('.gun-hucre[tabindex="0"]')
    expect(hucreler.length).toBeGreaterThan(0)
    await hucreler[0].trigger('keydown', { key: 'Enter' })
    expect(w.vm.seciliGun).toBeTruthy()
    expect(hucreler[0].attributes('role')).toBe('gridcell')
  })

  it('ekran okuyucu için gün özeti canlı bölgede duyurulur', async () => {
    const w = kur()
    await flushPromises()
    const canli = w.find('.sr-only[aria-live="polite"]')
    expect(canli.exists()).toBe(true)
    expect(canli.text()).toContain('olay')
    expect(w.find('[role="grid"]').exists()).toBe(true)
    expect(w.findAll('[role="row"]').length).toBeGreaterThan(0)
    expect(w.findAll('[role="columnheader"]').length).toBe(7)
  })
})
