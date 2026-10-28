import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import i18n from '../../i18n.js'
import CekSenetDetayDrawer from '../CekSenetDetayDrawer.vue'

const stubs = {
  Drawer: {
    props: ['visible', 'header'],
    template: '<div class="drawer"><div class="baslik">{{ header }}</div><slot /><slot name="footer" /></div>'
  },
  Tag: { props: ['value'], template: '<span class="tag">{{ value }}</span>' },
  Button: { props: ['label'], template: '<button class="btn">{{ label }}</button>' },
  SkeletonLoader: { template: '<div class="skeleton-stub" />' }
}

const kayit = {
  id: 1,
  tur: 'CEK',
  durum: 'PORTFOY',
  cekNo: 'C-1001',
  cariHesapId: 7,
  cariHesapAdi: 'Acme Ltd.',
  bankaAdi: 'Ziraat',
  sube: 'Merkez',
  hesapNo: '123-456',
  vadeTarihi: '2026-11-10',
  kesinmeTarihi: '2026-11-11',
  tutar: 12500,
  aciklama: 'Müşteri çeki',
  faturaNo: 'FTR-2026-001',
  ciroCariAd: 'Delta Ticaret',
  ciroTarihi: '2026-10-20T10:00:00',
  ciroNotu: 'Tedarikçiye ciro',
  teminatTarihi: '2026-10-21T10:00:00',
  teminatAciklama: 'Banka teminatı',
  protestoTarihi: null,
  tahsilatHareketId: 12,
  kasaAd: 'Merkez Kasa',
  bankaAd: 'Ziraat Vadesiz',
  gecmis: [
    {
      id: 1,
      islem: 'OLUSTUR',
      eskiDurum: null,
      yeniDurum: 'PORTFOY',
      aciklama: 'Kayıt oluşturuldu',
      olusturmaTarihi: '2026-10-01T10:00:00'
    },
    {
      id: 2,
      islem: 'CIRO',
      eskiDurum: 'PORTFOY',
      yeniDurum: 'CIRO_EDILDI',
      aciklama: 'Delta Ticaret',
      olusturmaTarihi: '2026-10-20T10:00:00'
    },
    {
      id: 3,
      islem: 'TAHSIL',
      eskiDurum: 'CIRO_EDILDI',
      yeniDurum: 'TAHSIL_EDILDI',
      aciklama: 'Tahsil edildi',
      olusturmaTarihi: '2026-10-22T10:00:00'
    }
  ]
}

const kur = (props = {}) =>
  mount(CekSenetDetayDrawer, {
    props: { visible: true, kayit, ...props },
    global: { plugins: [i18n], stubs }
  })

const etiketler = (w) => w.findAll('.btn').map((b) => b.text())

describe('CekSenetDetayDrawer', () => {
  it('çek/senet bilgilerini, cari/fatura ve ciro/teminat alanlarını gösterir', () => {
    const w = kur()
    const metin = w.text()

    expect(metin).toContain('Çek/Senet Detayı')
    expect(metin).toContain('C-1001')
    expect(metin).toContain('Çek')
    expect(metin).toContain('Portföy')
    expect(metin).toContain('Acme Ltd.')
    expect(metin).toContain('FTR-2026-001')
    expect(metin).toContain('Ziraat')
    expect(metin).toContain('123-456')
    expect(metin).toContain('Müşteri çeki')
    expect(metin).toContain('Delta Ticaret')
    expect(metin).toContain('Tedarikçiye ciro')
    expect(metin).toContain('Banka teminatı')
  })

  it('tahsil bağlarını (hareket, kasa, banka) gösterir', () => {
    const w = kur()
    const metin = w.text()

    expect(metin).toContain('Tahsil Bağları')
    expect(metin).toContain('#12')
    expect(metin).toContain('Merkez Kasa')
    expect(metin).toContain('Ziraat Vadesiz')
  })

  it('ciro/teminat ve tahsil verisi yoksa ilgili bölümleri gizler', () => {
    const w = kur({
      kayit: {
        ...kayit,
        ciroCariAd: null,
        ciroTarihi: null,
        ciroNotu: null,
        teminatTarihi: null,
        teminatAciklama: null,
        tahsilatHareketId: null,
        kasaAd: null,
        bankaAd: null
      }
    })
    expect(w.text()).not.toContain('Ciro / Teminat')
    expect(w.text()).not.toContain('Tahsil Bağları')
  })

  it('zaman çizelgesinde durum geçişlerini işlem türüne göre renklendirir', () => {
    const w = kur()
    const adimlar = w.findAll('.zc-adim')

    expect(adimlar).toHaveLength(3)
    expect(w.find('.zc-adim.taslak').exists()).toBe(true)
    expect(w.find('.zc-adim.uretimde').exists()).toBe(true)
    expect(w.find('.zc-adim.tamam').exists()).toBe(true)
    expect(w.text()).toContain('Oluşturuldu')
    expect(w.text()).toContain('Tahsil Edildi')
  })

  it('portföy durumunda tahsil/ciro/teminat/protesto ve düzenle/sil aksiyonlarını gösterir', () => {
    const liste = etiketler(kur())
    expect(liste).toEqual(
      expect.arrayContaining(['Tahsil Et', 'Ciro Et', 'Teminat Ver', 'Protesto', 'Düzenle', 'Sil'])
    )
  })

  it('teminatta tahsil, teminat iadesi ve protesto; ciro aksiyonu yok', () => {
    const liste = etiketler(kur({ kayit: { ...kayit, durum: 'TEMINATTA' } }))
    expect(liste).toEqual(expect.arrayContaining(['Tahsil Et', 'Teminat İadesi', 'Protesto']))
    expect(liste).not.toContain('Ciro Et')
    expect(liste).not.toContain('Teminat Ver')
  })

  it('ciro edilmiş kayıtta ciro iadesi gösterir', () => {
    const liste = etiketler(kur({ kayit: { ...kayit, durum: 'CIRO_EDILDI' } }))
    expect(liste).toContain('Ciro İadesi')
    expect(liste).not.toContain('Tahsil Et')
  })

  it('iade kaydında portföye al aksiyonu gösterir', () => {
    const liste = etiketler(kur({ kayit: { ...kayit, durum: 'IADE' } }))
    expect(liste).toContain('Portföye Al')
  })

  it('tahsil edilmiş kayıtta yalnız tahsil iptali gösterir; düzenle/sil göstermez', () => {
    const liste = etiketler(kur({ kayit: { ...kayit, durum: 'TAHSIL_EDILDI' } }))
    expect(liste).toContain('Tahsil İptali')
    expect(liste).not.toContain('Düzenle')
    expect(liste).not.toContain('Sil')
    expect(liste).not.toContain('Tahsil Et')
  })

  it('protesto terminal durumunda hiçbir yaşam döngüsü aksiyonu göstermez', () => {
    const liste = etiketler(kur({ kayit: { ...kayit, durum: 'PROTESTO' } }))
    expect(liste).not.toContain('Tahsil İptali')
    expect(liste).not.toContain('Sil')
    expect(liste).not.toContain('Düzenle')
    expect(liste).not.toContain('Tahsil Et')
  })

  it('yüklenirken iskelet gösterir', () => {
    const w = kur({ yukleniyor: true })
    expect(w.find('.detay-iskelet[aria-busy="true"]').exists()).toBe(true)
    expect(w.find('.skeleton-stub').exists()).toBe(true)
  })

  it('aksiyon olaylarını yayar', async () => {
    const w = kur()
    await w.findAll('.btn').find((b) => b.text() === 'Tahsil Et').trigger('click')
    expect(w.emitted('tahsil')?.[0]?.[0].id).toBe(1)

    await w.findAll('.btn').find((b) => b.text() === 'Sil').trigger('click')
    expect(w.emitted('sil')?.[0]?.[0].id).toBe(1)

    await w.findAll('.btn').find((b) => b.text() === 'Düzenle').trigger('click')
    expect(w.emitted('duzenle')?.[0]?.[0].id).toBe(1)
  })
})
