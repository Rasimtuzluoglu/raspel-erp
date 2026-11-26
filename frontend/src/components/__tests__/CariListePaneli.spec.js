import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CariListePaneli from '../cari/CariListePaneli.vue'

const cariler = [
  {
    id: 1,
    ad: 'Pozitif Cari',
    tur: 'Musteri',
    telefon: '0555 111 22 33',
    yetkiliKisi: 'Ali',
    temsilciAd: 'Temsilci A',
    bakiye: 1000,
    etiketler: 'vip'
  },
  {
    id: 2,
    ad: 'Negatif Cari',
    tur: 'Tedarikci',
    bakiye: -500,
    etiketler: ''
  }
]

const stubs = {
  AppDataTable: {
    template: '<div><slot /><slot name="empty" /><slot name="batch-actions" /></div>'
  },
  DataTable: { template: '<div><slot /></div>' },
  Column: true,
  Tag: true,
  Button: {
    props: ['label', 'icon'],
    emits: ['click'],
    template: '<button class="test-button" @click="$emit(\'click\')">{{ label }}</button>'
  },
  EmptyState: {
    props: ['actionLabel'],
    emits: ['action'],
    template: '<button class="empty-action" @click="$emit(\'action\')">{{ actionLabel }}</button>'
  },
  SatirEylemleri: true,
  Paginator: true
}

const mountPanel = (props = {}) => mount(CariListePaneli, {
  global: { plugins: [i18n], stubs },
  props: {
    cariHesaplar: cariler,
    toplamKayit: 2,
    secili: [],
    yukleniyor: false,
    ilkYukleme: false,
    yogunluk: 'comfortable',
    sayfa: 0,
    sayfaBoyutu: 10,
    siralama: { field: 'ad', order: 1 },
    kolonlar: Array.from({ length: 10 }, () => ({ visible: true })),
    isAdmin: true,
    eylemler: () => [],
    kopyala: vi.fn(),
    ...props
  }
})

describe('CariListePaneli', () => {
  it('mobil kartlarda cari bilgilerini ve bakiye rozetlerini gosterir', () => {
    const wrapper = mountPanel()

    expect(wrapper.findAll('.cari-kart')).toHaveLength(2)
    expect(wrapper.findAll('.bakiye-rozet')).toHaveLength(2)
    expect(wrapper.findAll('.bakiye-rozet')[0].classes()).toContain('borc')
    expect(wrapper.findAll('.bakiye-rozet')[1].classes()).toContain('alacak')
    expect(wrapper.text()).toContain('Pozitif Cari')
  })

  it('telefon kopyalama ve tahsilat olaylarini yayar', async () => {
    const kopyala = vi.fn()
    const wrapper = mountPanel({ kopyala })

    await wrapper.find('.cari-kart-alt .kopyalanabilir').trigger('click')
    expect(kopyala).toHaveBeenCalledWith('0555 111 22 33', expect.any(String))

    await wrapper.find('[data-test="cari-tahsilat"]').trigger('click')
    expect(wrapper.emitted('tahsilat')[0][0].id).toBe(1)
  })

  it('batch aksiyonlarini yukari iletir', async () => {
    const wrapper = mountPanel()

    const butonlar = wrapper.findAll('.test-button')
    const eposta = butonlar.find((b) => b.text().includes('Toplu E-posta'))
    const sil = butonlar.find((b) => b.text().includes('Toplu Sil'))
    const csv = butonlar.find((b) => b.text().includes('CSV'))

    await eposta.trigger('click')
    await sil.trigger('click')
    await csv.trigger('click')

    expect(wrapper.emitted('toplu-email')).toHaveLength(1)
    expect(wrapper.emitted('toplu-sil')).toHaveLength(1)
    expect(wrapper.emitted('csv')).toHaveLength(1)
  })

  it('bos durumda yeni cari olayini yayar', async () => {
    const wrapper = mountPanel({ cariHesaplar: [], toplamKayit: 0 })

    await wrapper.find('.empty-action').trigger('click')
    expect(wrapper.emitted('yeni')).toHaveLength(1)
  })

  it('ilk yuklemede liste govdesini gostermez', () => {
    const wrapper = mountPanel({ ilkYukleme: true })

    expect(wrapper.find('.table-container').exists()).toBe(false)
    expect(wrapper.find('.cari-kart-liste').exists()).toBe(false)
  })
})
