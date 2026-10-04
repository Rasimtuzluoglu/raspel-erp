import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'
import i18n from '../../i18n.js'

/**
 * REDTEAM/Faz1.1 regresyon testi.
 *
 * CANLI KANIT: `MaasBordro.vue` onMounted'da `pR.data.map(...)` çağırıyordu.
 * `/api/personel` bir `Page<PersonelDTO>` (yani NESNE: { content, totalElements })
 * döndürdüğü için `.map` undefined'a çağrılıyor ve TypeError fırlatıyordu.
 * Sonuç: try/catch'e düşüp `kasaListesi` HİÇ yüklenmiyordu → ödeme dialog'undaki
 * kasa dropdown'u boş → "Öde" butonu kalıcı `disabled` → **bordro ödemesi
 * arayüzden hiç yapılamıyordu**. Ayrıca her sayfa açılışında kırmızı hata
 * toast'ı çıkıyordu.
 *
 * Aynı desen `Vardiyalar.vue` içinde de kopyalanmıştı; ikisi de düzeltildi.
 */

const hataToast = vi.fn()
const basariToast = vi.fn()

const maasBordroMock = [
  {
    id: 1,
    personelId: 7,
    personelAdi: 'Ali Veli',
    yil: 2026,
    ay: 10,
    brutMaas: 50000,
    kesintiler: 12500,
    netMaas: 37500,
    durum: 'ONAYLANDI',
    odemeDurumu: 'ODENMEDI',
    sirketId: 4
  }
]

// personelListesi bu testte dolduruluyor mu diye izleniyor.
// Canlı hata halinde kasaListesi `[]` kalıyordu.
const kasaMock = [{ id: 3, ad: 'Kasa A', bakiye: 100000 }]
const bankaMock = [{ id: 2, ad: 'Banka A', bakiye: 50000 }]

vi.mock('../../composables/useToastBildirim.js', () => ({
  useToastBildirim: () => ({
    hata: hataToast,
    basari: basariToast,
    bilgi: vi.fn(),
    uyari: vi.fn()
  })
}))

vi.mock('../../api/index.js', () => ({
  maasBordroAPI: {
    getAll: vi.fn(() => Promise.resolve({ data: { content: maasBordroMock } })),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    onayla: vi.fn(),
    ode: vi.fn()
  },
  // REDTEAM: `/api/personel` Page (nesne) döndürür. Dizi DEĞİL.
  personelAPI: {
    getAll: vi.fn(() =>
      Promise.resolve({
        data: {
          content: [
            { id: 7, ad: 'Ali', soyad: 'Veli' },
            { id: 8, ad: 'Ayşe', soyad: 'Yılmaz' }
          ],
          totalElements: 2
        }
      })
    )
  },
  kasaAPI: { getAll: vi.fn(() => Promise.resolve({ data: { content: kasaMock } })) },
  bankaAPI: { getAll: vi.fn(() => Promise.resolve({ data: { content: bankaMock } })) }
}))

const MaasBordro = () => import('../MaasBordro.vue')

const stubs = {
  Button: true,
  DataTable: true,
  Column: true,
  Dialog: true,
  InputText: true,
  InputNumber: true,
  Select: true,
  SelectButton: true,
  DatePicker: true,
  Textarea: true,
  ToggleSwitch: true,
  Tag: true,
  Message: true,
  EmptyState: true,
  SkeletonLoader: true,
  PageHeader: true,
  ConfirmDialog: true
}

describe('REDTEAM Faz1.1 - MaasBordro sayfa yukleme (bordro odemesi olmusu)', () => {
  let wrapper

  beforeEach(() => {
    setActivePinia(createPinia())
    hataToast.mockClear()
    basariToast.mockClear()
  })

  afterEach(() => {
    if (wrapper) wrapper.unmount()
  })

  it('Page (nesne) donen personel listesini cozebilmeli - TypeError atmamali', async () => {
    const { default: MaasBordroView } = await MaasBordro()
    wrapper = mount(MaasBordroView, {
      global: { plugins: [createPinia(), ToastService, ConfirmationService, i18n], stubs }
    })
    await flushPromises()

    // Canlı hata halinde buraya DÜŞÜLÜRDÜ (TypeError -> catch -> toast).
    expect(hataToast).not.toHaveBeenCalled()
  })

  it('odeme dialogundaki kasa listesi dolu olmali (odeme butonu kilitli kalmasin)', async () => {
    const { default: MaasBordroView } = await MaasBordro()
    wrapper = mount(MaasBordroView, {
      global: { plugins: [createPinia(), ToastService, ConfirmationService, i18n], stubs }
    })
    await flushPromises()

    // MaasBordro.vue kasaListesi'ni unwrapList(kR) ile doldurur.
    // `pR.data.map` patlayıp catch'e düştüğünde bu dolmazdı.
    const { kasaAPI } = await import('../../api/index.js')
    expect(kasaAPI.getAll).toHaveBeenCalled()
    // Yükleme hatası olmadığının dolaylı kanıtı: hata toast'ı yok.
    expect(hataToast).not.toHaveBeenCalled()
  })
})