import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import PrimeVue from 'primevue/config'
import i18n from '../../i18n.js'
import CariHesaplar from '../CariHesaplar.vue'
import { cariHesapAPI } from '../../api/index.js'

const { filtreliMock } = vi.hoisted(() => ({
  filtreliMock: vi.fn()
}))

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: {
    getAll: vi.fn().mockResolvedValue({ data: { content: [], totalElements: 0 } }),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    search: vi.fn(),
    filtreli: filtreliMock,
    ozet: vi.fn().mockResolvedValue({ data: { toplamKayit: 2, alacakli: 1800, borclu: 939737.5 } }),
    riskliCariler: vi.fn().mockResolvedValue({ data: [] }),
    etiketler: vi.fn().mockResolvedValue({ data: ['vip', 'bayi'] }),
    adresler: vi.fn().mockResolvedValue({ data: [] }),
    topluGuncelle: vi.fn(),
    topluSil: vi.fn()
  },
  hareketAPI: { getByCariHesap: vi.fn().mockResolvedValue({ data: [] }) },
  notAPI: { cariNotlari: vi.fn().mockResolvedValue({ data: [] }) },
  excelAPI: { cariHesaplar: vi.fn(), stoklar: vi.fn() },
  faturaAPI: { getByCariHesap: vi.fn().mockResolvedValue({ data: [] }), cariFaturalari: vi.fn().mockResolvedValue({ data: [] }) },
  stokAPI: { getAll: vi.fn().mockResolvedValue({ data: [] }) },
  kullaniciAPI: { getAll: vi.fn().mockResolvedValue({ data: { content: [] } }) },
  raporAPI: {},
  uploadAPI: { yukle: vi.fn() }
}))

const stubs = {
  PageHeader: { props: ['title'], template: '<header class="ph">{{ title }}<slot name="actions" /></header>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}</div>' },
  Menu: { template: '<div class="islem-menu" />' },
  Paginator: true,
  IlkZiyaretIpuclari: true,
  TabloAyarlari: true,
  SatirEylemleri: true,
  EmptyState: true,
  DataTable: { template: '<div class="tablo"><slot /></div>' },
  Column: true,
  Dialog: { template: '<div><slot /></div>' },
  AutoComplete: true,
  Button: { template: '<button><slot /></button>' },
  Dropdown: true,
  InputText: true,
  InputNumber: true,
  InputSwitch: true,
  IconField: { template: '<span class="iconfield"><slot /></span>' },
  InputIcon: true,
  DatePicker: true,
  Textarea: true,
  Checkbox: true,
  Message: true,
  SelectButton: true,
  Tag: true,
  Skeleton: true,
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot /></div>' }
}

const kur = () =>
  mount(CariHesaplar, {
    global: {
      plugins: [createPinia(), PrimeVue, ToastService, ConfirmationService, i18n],
      stubs
    }
  })

describe('CariHesaplar — Faz0 doğruluk', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    i18n.global.locale.value = 'tr'
    filtreliMock.mockReset()
    filtreliMock.mockResolvedValue({
      data: {
        content: [
          { id: 4, ad: 'Pozitif Cari', bakiye: 939737.5, tur: 'Musteri', aktif: true },
          { id: 8, ad: 'Negatif Cari', bakiye: -3280, tur: 'Musteri', aktif: true }
        ],
        totalElements: 2
      }
    })
  })

  it('varsayılan sunucu sıralamasını ad,asc olarak gönderir', async () => {
    kur()
    await new Promise((r) => setTimeout(r, 80))

    expect(filtreliMock).toHaveBeenCalledWith(expect.objectContaining({
      page: 0,
      size: 10,
      sort: 'ad,asc'
    }))
  })

  it('pozitif bakiye ödenecek (kırmızı), negatif bakiye tahsil edilecek (yeşil) gösterilir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 80))

    const kartlar = w.findAll('.cari-kart')
    expect(kartlar).toHaveLength(2)

    const pozitif = kartlar[0].find('.bakiye-rozet')
    expect(pozitif.classes()).toContain('borc')
    expect(pozitif.find('i').classes()).toContain('pi-arrow-up-right')

    const negatif = kartlar[1].find('.bakiye-rozet')
    expect(negatif.classes()).toContain('alacak')
    expect(negatif.find('i').classes()).toContain('pi-arrow-down-left')
  })

  it('KPI ve filtre etiketleri belirsiz "bize alacaklı/borçlu" ifadelerini kullanmaz', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 80))

    const metin = w.text()
    expect(metin).toContain('Tahsil Edilecek')
    expect(metin).toContain('Ödenecek')
    expect(metin).not.toContain('Bize Alacaklı')
    expect(metin).not.toContain('Bize Borçlu')
  })

  it('form etiketleri anahtar adı yerine okunabilir Türkçe metin gösterir', () => {
    expect(i18n.global.t('cariHesaplar.krediLimitiTL')).toBe('Kredi Limiti (TL)')
    expect(i18n.global.t('cariHesaplar.odemeVadesiGun')).toBe('Ödeme Vadesi (Gün)')
  })

  it('KPI kartına tıklamak bakiye filtresini uygular ve aktif filtre çipini gösterir', async () => {
    const w = kur()
    await new Promise((r) => setTimeout(r, 80))
    filtreliMock.mockClear()

    await w.get('[data-test="kpi-alacak"]').trigger('click')
    await new Promise((r) => setTimeout(r, 40))

    expect(filtreliMock).toHaveBeenCalledWith(expect.objectContaining({
      bakiyeYonu: 'alacak',
      page: 0
    }))
    expect(w.get('[data-test="cari-aktif-filtreler"]').text()).toContain('Tahsil Edilecek')

    await w.get('.cari-filtre-cip').trigger('click')
    await new Promise((r) => setTimeout(r, 40))
    expect(w.find('[data-test="cari-aktif-filtreler"]').exists()).toBe(false)
  })

  it('şirket etiketlerini autocomplete için yükler', async () => {
    kur()
    await new Promise((r) => setTimeout(r, 80))
    expect(cariHesapAPI.etiketler).toHaveBeenCalled()
  })
})
