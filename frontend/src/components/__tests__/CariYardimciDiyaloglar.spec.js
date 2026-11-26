import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CariYardimciDiyaloglar from '../cari/CariYardimciDiyaloglar.vue'

const { riskliCariler, adresler, adresEkle, adresSil, topluGuncelle, acilis } = vi.hoisted(() => ({
  riskliCariler: vi.fn(),
  adresler: vi.fn(),
  adresEkle: vi.fn(),
  adresSil: vi.fn(),
  topluGuncelle: vi.fn(),
  acilis: vi.fn()
}))

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: {
    riskliCariler,
    adresler,
    adresEkle,
    adresSil,
    topluGuncelle
  },
  hareketAPI: { acilis }
}))

vi.mock('../../composables/useToastBildirim.js', () => ({
  useToastBildirim: () => ({
    basarili: vi.fn(),
    hata: vi.fn(),
    uyari: vi.fn()
  })
}))

const DialogStub = {
  props: ['visible', 'header'],
  template: '<div v-if="visible" class="p-dialog"><slot name="header" /><slot /><slot name="footer" /></div>'
}

const ButtonStub = {
  props: ['label'],
  emits: ['click'],
  template: '<button class="test-button" @click="$emit(\'click\')">{{ label }}</button>'
}

const stubs = {
  Dialog: DialogStub,
  DataTable: { template: '<div><slot /><slot name="empty" /></div>' },
  Column: true,
  Divider: true,
  SkeletonLoader: true,
  InputText: true,
  InputNumber: true,
  Textarea: true,
  DatePicker: true,
  Dropdown: true,
  Checkbox: true,
  Button: ButtonStub
}

const mountDialog = (props = {}) => mount(CariYardimciDiyaloglar, {
  global: { plugins: [i18n], stubs },
  props: {
    seciliCariHesaplar: [{ id: 1, ad: 'A', email: 'a@test.com' }, { id: 2, ad: 'B' }],
    cariTurSecenekleri: [{ label: 'Müşteri', value: 'Musteri' }],
    aktifSecenekleri: [{ label: 'Aktif', value: true }],
    ...props
  }
})

describe('CariYardimciDiyaloglar', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    riskliCariler.mockResolvedValue({ data: [{ cariId: 1, cariAd: 'Riskli', borc: 1000 }] })
    adresler.mockResolvedValue({ data: [{ id: 1, baslik: 'Merkez', adres: 'Adres' }] })
    adresEkle.mockResolvedValue({ data: {} })
    adresSil.mockResolvedValue({ data: {} })
    topluGuncelle.mockResolvedValue({ data: {} })
    acilis.mockResolvedValue({ data: {} })
  })

  it('riskli cariler dialogunu acar ve listeyi yukler', async () => {
    const wrapper = mountDialog()

    await wrapper.vm.riskliAc()
    await flushPromises()

    expect(riskliCariler).toHaveBeenCalledTimes(1)
    expect(wrapper.find('.p-dialog').exists()).toBe(true)
    expect(wrapper.text()).toContain('Kredi Limitini Aşan Cariler')
  })

  it('adres dialogunu acar ve adresleri yukler', async () => {
    const wrapper = mountDialog()

    await wrapper.vm.adresAc({ id: 1, ad: 'Cari A' })
    await flushPromises()

    expect(adresler).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('Adresler')
  })

  it('acilis dialogunu cari bilgisiyle acar', async () => {
    const wrapper = mountDialog()

    wrapper.vm.acilisAc({ id: 1, ad: 'Cari A' })
    await flushPromises()

    expect(wrapper.text()).toContain('Açılış Fişi')
    expect(wrapper.text()).toContain('Cari A')
  })

  it('toplu e-posta dialogunu secili sayisiyla acar', async () => {
    const wrapper = mountDialog()

    wrapper.vm.emailAc()
    await flushPromises()

    expect(wrapper.text()).toContain('Toplu E-posta')
    expect(wrapper.text()).toContain('2')
  })

  it('toplu guncelleme dialogunu acar', async () => {
    const wrapper = mountDialog()

    wrapper.vm.topluAc()
    await flushPromises()

    expect(wrapper.text()).toContain('Toplu Güncelle')
  })
})
