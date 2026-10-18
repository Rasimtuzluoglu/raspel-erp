import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import i18n from '../../i18n.js'
import { raporAPI } from '../../api/index.js'
import Gorunumler360 from '../Gorunumler360.vue'

vi.mock('vue-router', () => ({
  useRouter: () => ({ push: vi.fn() })
}))

vi.mock('../../api/index.js', () => ({
  raporAPI: {
    stokKar360: vi.fn(() =>
      Promise.resolve({
        data: {
          urunler: [
            {
              stokId: 1,
              ad: 'Masa',
              stokKodu: 'M1',
              kategori: 'Mobilya',
              satisAdet: 2,
              ciro: 100,
              maliyet: 60,
              brutKar: 40,
              marj: 40,
              stokMiktar: 5
            }
          ],
          toplamCiro: 100,
          toplamKar: 40,
          genelMarj: 40,
          negatifMarjAdet: 0,
          toplamUrun: 1
        }
      })
    ),
    calisanPerformans360: vi.fn(() =>
      Promise.resolve({
        data: {
          calisanlar: [
            { id: 1, ad: 'Ali', rol: 'DRIVER', toplamTeslimat: 4, tamamlananTeslimat: 3, bekleyenTeslimat: 1, teslimTutari: 500 }
          ],
          toplamTeslimat: 4,
          tamamlananTeslimat: 3
        }
      })
    ),
    musteriSegment: vi.fn(() =>
      Promise.resolve({
        data: {
          segmentOzeti: [{ segment: 'VIP', adet: 1, toplamCiro: 1000 }],
          musteriler: [{ cariId: 1, ad: 'Müşteri A', segment: 'VIP', ciro: 1000, kalanTutar: 0, faturaAdet: 2, sonFaturaGunOnce: 5 }]
        }
      })
    )
  },
  excelAPI: {
    gorunum360StokKar: vi.fn(() => Promise.resolve({ data: new Blob([]) })),
    gorunum360Calisan: vi.fn(() => Promise.resolve({ data: new Blob([]) })),
    gorunum360Segment: vi.fn(() => Promise.resolve({ data: new Blob([]) }))
  }
}))

const stubs = {
  PageHeader: { props: ['title', 'subtitle', 'icon'], template: '<div class="ph">{{ title }}<slot name="actions" /></div>' },
  KpiKart: { props: ['baslik', 'deger'], template: '<div class="kpi">{{ baslik }}:{{ deger }}<slot name="deger" /></div>' },
  EmptyState: true,
  SkeletonLoader: true,
  AppDataTable: { props: ['value'], template: '<div class="tbl"><slot name="loading" /><slot /></div>' },
  Column: true,
  TabView: { template: '<div><slot /></div>' },
  TabPanel: { template: '<div><slot name="header" /><slot /></div>' },
  Button: { template: '<button><slot /></button>' },
  DatePicker: true,
  Dropdown: true,
  IconField: { template: '<div><slot /></div>' },
  InputIcon: true,
  InputText: true,
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' }
}

const kur = () =>
  mount(Gorunumler360, {
    global: {
      plugins: [createPinia(), ToastService, i18n],
      stubs,
      directives: { tooltip: {} }
    }
  })

describe('Gorunumler360.vue', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    localStorage.clear()
  })

  it('üç 360 veri kaynağını yükler ve KPI render eder', async () => {
    const w = kur()
    await flushPromises()
    expect(raporAPI.stokKar360).toHaveBeenCalledTimes(1)
    expect(raporAPI.calisanPerformans360).toHaveBeenCalledTimes(1)
    expect(raporAPI.musteriSegment).toHaveBeenCalledTimes(1)
    expect(w.text()).toContain('Toplam Ciro')
  })

  it('hatırlanan sekmeyi geri yükler', async () => {
    localStorage.setItem('raspel_gorunum360_tab', '2')
    const w = kur()
    await flushPromises()
    expect(w.vm.aktifSekme).toBe(2)
  })

  it('Excel indirme blob oluşturur ve bildirim verir', async () => {
    const w = kur()
    await flushPromises()
    const { excelAPI } = await import('../../api/index.js')
    const createUrl = vi.fn(() => 'blob:test')
    const revokeUrl = vi.fn()
    global.URL.createObjectURL = createUrl
    global.URL.revokeObjectURL = revokeUrl
    await w.vm.stokExcelIndir()
    expect(excelAPI.gorunum360StokKar).toHaveBeenCalled()
    expect(createUrl).toHaveBeenCalled()
  })
})
