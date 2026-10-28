import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import TahsilatCariDrawer from '../TahsilatCariDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer"><div class="drawer-baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: {
    props: ['label'],
    emits: ['click'],
    template: '<button class="btn" @click="$emit(\'click\')">{{ label }}</button>'
  },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' }
}

const cari = {
  cariId: 1,
  cariAd: 'A Ltd',
  telefon: '05555555555',
  email: 'a@x.com',
  toplamAlacak: 3000,
  gecikmisAlacak: 1000,
  faturaSayisi: 2,
  aralik: '31-60 Gün',
  faturalar: [
    { faturaId: 1, faturaNumarasi: 'F-1', vadeTarihi: '2026-08-19', kalanTutar: 1000, gecikmeGunu: 10 },
    { faturaId: 2, faturaNumarasi: 'F-2', vadeTarihi: '2026-11-01', kalanTutar: 2000, gecikmeGunu: -5 }
  ]
}

const sonTahsilatlar = [
  {
    id: 9,
    tarih: '2026-10-01',
    tutar: 1500,
    odemeYontemi: 'NAKIT',
    durum: 'AKTIF'
  },
  {
    id: 10,
    tarih: '2026-09-15',
    tutar: 500,
    odemeYontemi: 'HAVALE',
    durum: 'IPTAL'
  }
]

const kur = (props = {}) =>
  mount(TahsilatCariDrawer, {
    props: { visible: true, cari, sonTahsilatlar, ...props },
    global: { plugins: [i18n], stubs }
  })

describe('TahsilatCariDrawer', () => {
  it('cari iletişim, alacak ve yaş aralığı bilgilerini gösterir', () => {
    const w = kur()
    const metin = w.text()

    expect(metin).toContain('Cari Tahsilat Detayı')
    expect(metin).toContain('A Ltd')
    expect(metin).toContain('05555555555')
    expect(metin).toContain('a@x.com')
    expect(metin).toContain('31-60 Gün')
    expect(w.find('.detay-grid').text()).toContain('3.000')
  })

  it('açık faturaları vade ve gecikme bilgisiyle listeler', () => {
    const w = kur()
    const satirlar = w.findAll('.fatura-satir')

    expect(satirlar).toHaveLength(2)
    expect(satirlar[0].text()).toContain('F-1')
    expect(satirlar[0].text()).toContain('10 gün gecikti')
    expect(satirlar[1].text()).toContain('5 gün kaldı')
  })

  it('son tahsilatları yöntem ve durum rozetiyle gösterir', () => {
    const w = kur()
    const satirlar = w.findAll('.tahsilat-satir')

    expect(satirlar).toHaveLength(2)
    expect(satirlar[0].text()).toContain('Nakit')
    expect(satirlar[0].text()).toContain('Aktif')
    expect(satirlar[1].text()).toContain('Havale')
    expect(satirlar[1].text()).toContain('İptal')
  })

  it('tahsilat, WhatsApp ve e-posta aksiyonlarını yayar', async () => {
    const w = kur()
    const butonlar = w.findAll('.btn')

    await butonlar[0].trigger('click')
    await butonlar[1].trigger('click')
    await butonlar[2].trigger('click')

    expect(w.emitted('tahsilat')?.[0]?.[0].cariId).toBe(1)
    expect(w.emitted('whatsapp')?.[0]?.[0].cariId).toBe(1)
    expect(w.emitted('eposta')?.[0]?.[0].cariId).toBe(1)
  })

  it('e-posta yoksa e-posta aksiyonunu göstermez', () => {
    const w = kur({ cari: { ...cari, email: null } })
    const etiketler = w.findAll('.btn').map((b) => b.text())

    expect(etiketler).toContain('WhatsApp')
    expect(etiketler).not.toContain('E-posta Gönder')
  })

  it('son tahsilatlar yüklenirken iskelet gösterir', () => {
    const w = kur({ yukleniyor: true })

    expect(w.find('[aria-busy="true"]').exists()).toBe(true)
    expect(w.find('.skeleton-stub').exists()).toBe(true)
    expect(w.findAll('.tahsilat-satir')).toHaveLength(0)
  })

  it('son tahsilat yoksa boş durum metni gösterir', () => {
    const w = kur({ sonTahsilatlar: [] })
    expect(w.text()).toContain('Henüz tahsilat kaydı yok')
  })
})
