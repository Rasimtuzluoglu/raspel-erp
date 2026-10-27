import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import IrsaliyeDetayDrawer from '../IrsaliyeDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer"><div class="baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  RouterLink: { props: ['to'], template: '<a class="router-link" :data-to="to"><slot /></a>' },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: { props: ['label'], template: '<button class="btn">{{ label }}</button>' },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' }
}

const kayit = {
  id: 1,
  irsaliyeNo: 'IRS-2026-0001',
  tarih: '2026-10-01',
  cariHesapId: 10,
  cariHesapAdi: 'Acme Ltd',
  tur: 'SATIS',
  durum: 'HAZIRLANDI',
  aciklama: 'Sabah sevkiyatı',
  plaka: '34 ABC 123',
  soforId: 5,
  soforAd: 'Ali Veli',
  tasiyiciFirma: 'Hızlı Lojistik',
  sevkTarihi: null,
  teslimAlan: null,
  teslimTarihi: null,
  teslimatAdresi: 'Atatürk Cad. No:1',
  koliAdedi: 4,
  desi: 12.5,
  brutKg: 80,
  teslimatId: 77,
  teslimatDurum: 'YOLDA',
  siparisNo: 'SIP-7',
  faturaId: null,
  faturaNumarasi: null,
  kalemler: [
    { id: 1, stokId: 100, stokAdi: 'Ürün A', miktar: 5, birim: 'Adet', aciklama: 'Kutu' },
    { id: 2, stokId: 101, stokAdi: 'Ürün B', miktar: 2, birim: 'Kg', aciklama: '' }
  ]
}

const gecmis = [
  { id: 1, eskiDurum: null, yeniDurum: 'TASLAK', aciklama: 'İrsaliye oluşturuldu', olusturmaTarihi: '2026-10-01T09:00:00' },
  { id: 2, eskiDurum: 'TASLAK', yeniDurum: 'HAZIRLANDI', aciklama: 'Depoda hazırlandı', olusturmaTarihi: '2026-10-01T10:00:00' }
]

const kur = (props = {}) =>
  mount(IrsaliyeDetayDrawer, {
    props: { visible: true, kayit, gecmis, ...props },
    global: { plugins: [i18n], stubs }
  })

const etiketler = (w) => w.findAll('.btn').map((b) => b.text())

describe('IrsaliyeDetayDrawer', () => {
  it('irsaliye bilgilerini, sevk/teslim alanlarını ve bağlantıları gösterir', () => {
    const w = kur()
    const metin = w.text()

    expect(metin).toContain('İrsaliye Detayı')
    expect(metin).toContain('IRS-2026-0001')
    expect(metin).toContain('Acme Ltd')
    expect(metin).toContain('Hazırlandı')
    expect(metin).toContain('34 ABC 123')
    expect(metin).toContain('Ali Veli')
    expect(metin).toContain('Hızlı Lojistik')
    expect(metin).toContain('Atatürk Cad. No:1')
    expect(metin).toContain('SIP-7')
    expect(metin).toContain('#77')
    const baglanti = w.find('.router-link')
    expect(baglanti.attributes('data-to')).toBe('/teslimatlar')
  })

  it('fatura bağlantısını fatura detayına yönlendirir', () => {
    const w = kur({ kayit: { ...kayit, faturaId: 90, faturaNumarasi: 'FTR-0090' } })
    const baglanti = w.findAll('.router-link').find((a) => a.attributes('data-to') === '/faturalar/90')
    expect(baglanti).toBeTruthy()
    expect(baglanti.text()).toContain('FTR-0090')
    expect(etiketler(w)).not.toContain('Faturaya Dönüştür')
  })

  it('kalem tablosunu ve durum zaman çizelgesini gösterir', () => {
    const w = kur()
    expect(w.findAll('.kalem-tablo tbody tr')).toHaveLength(2)
    expect(w.text()).toContain('Ürün A')
    expect(w.text()).toContain('Ürün B')

    expect(w.findAll('.zc-adim')).toHaveLength(2)
    expect(w.find('.zc-adim.taslak').exists()).toBe(true)
    expect(w.find('.zc-adim.uretimde').exists()).toBe(true)
    expect(w.text()).toContain('Depoda hazırlandı')
  })

  it('kayit.gecmis yoksa boş durum, yüklenirken iskelet gösterir', () => {
    const bos = kur({ gecmis: [], kayit: { ...kayit, gecmis: [] } })
    expect(bos.findAll('.zc-adim')).toHaveLength(0)
    expect(bos.text()).toContain('Kayıt bulunamadı')

    const yukleniyor = kur({ gecmis: [], gecmisYukleniyor: true })
    expect(yukleniyor.find('.gecmis-yukleniyor[aria-busy="true"]').exists()).toBe(true)
    expect(yukleniyor.find('.skeleton-stub').exists()).toBe(true)
  })

  it('duruma göre aksiyonları gösterir', () => {
    expect(etiketler(kur())).toEqual(
      expect.arrayContaining(['Düzenle', 'Sevk Et', 'Faturaya Dönüştür', 'İptal Et'])
    )
    expect(etiketler(kur())).not.toContain('Hazırla')
    expect(etiketler(kur())).not.toContain('Teslim Et')

    const taslak = etiketler(kur({ kayit: { ...kayit, durum: 'TASLAK' } }))
    expect(taslak).toEqual(expect.arrayContaining(['Düzenle', 'Hazırla', 'Faturaya Dönüştür', 'İptal Et']))
    expect(taslak).not.toContain('Sevk Et')

    const sevk = etiketler(kur({ kayit: { ...kayit, durum: 'SEVK_EDILDI' } }))
    expect(sevk).toEqual(expect.arrayContaining(['Teslim Et', 'Sevki Geri Al', 'Faturaya Dönüştür', 'İptal Et']))
    expect(sevk).not.toContain('Düzenle')

    const teslim = etiketler(kur({ kayit: { ...kayit, durum: 'TESLIM_EDILDI' } }))
    expect(teslim).toEqual(expect.arrayContaining(['Faturaya Dönüştür', 'İptal Et']))
    expect(teslim).not.toContain('Teslim Et')

    const iptal = etiketler(kur({ kayit: { ...kayit, durum: 'IPTAL' } }))
    expect(iptal).toHaveLength(0)
  })

  it('aksiyon olaylarını yayar', async () => {
    const w = kur()
    const sevk = w.findAll('.btn').find((b) => b.text() === 'Sevk Et')
    await sevk.trigger('click')
    expect(w.emitted('sevk')?.[0]?.[0]?.id).toBe(1)

    const taslak = kur({ kayit: { ...kayit, durum: 'TASLAK' } })
    await taslak.findAll('.btn').find((b) => b.text() === 'Hazırla').trigger('click')
    expect(taslak.emitted('hazirla')?.[0]?.[0]?.id).toBe(1)

    const sevkli = kur({ kayit: { ...kayit, durum: 'SEVK_EDILDI' } })
    await sevkli.findAll('.btn').find((b) => b.text() === 'Sevki Geri Al').trigger('click')
    expect(sevkli.emitted('sevki-geri-al')?.[0]?.[0]?.id).toBe(1)
    await sevkli.findAll('.btn').find((b) => b.text() === 'İptal Et').trigger('click')
    expect(sevkli.emitted('iptal')?.[0]?.[0]?.id).toBe(1)
  })
})
