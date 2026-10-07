import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import PosFisOnizleme from '../PosFisOnizleme.vue'
import i18n from '../../i18n.js'

/**
 * POS FIS ONIZLEME — sozlesme ve fis kurali testleri.
 *
 * Ekanda gorunen fis, yazdirilan fis ile ayni kurallara uyar. En onemli kural
 * FIYATSUZ FIS modudur: `fisFiyatli === false` iken kalem tutarlari, ara
 * toplam, genel toplam ve odenen tutar HIC gosterilmez. Yanlislikla fiyatsiz
 * fis yazdirilip fiyatsiz satis kaydedilmesinin onu bu panelden okunur.
 */

const kur = (props = {}, pinia = createPinia()) =>
  mount(PosFisOnizleme, { props, global: { plugins: [i18n, pinia] } })

const sepet = [
  { id: 1, ad: 'Vida M8', miktar: 3, fiyat: 10 },
  { id: 2, ad: 'Somun M8', miktar: 2, fiyat: 5 }
]

const temel = {
  sirketAdi: 'Raspel Ltd',
  simdikiTarih: '05.10.2026 14:30',
  fisNo: 'FTR-2026-0001',
  sepet,
  toplam: 40,
  genelToplam: 40,
  odenenTutar: 40
}

describe('PosFisOnizleme — fis basligi', () => {
  it('sirket adi, tarih ve fis numarasi gosterilir', () => {
    const w = kur(temel)
    expect(w.find('.fis-baslik').text()).toBe('Raspel Ltd')
    expect(w.find('.fis-tarih').text()).toBe('05.10.2026 14:30')
    expect(w.find('.fis-fisno').text()).toContain('FTR-2026-0001')
  })

  it('sirket adi verilmemisse varsayilan baslik yazilir', () => {
    const w = kur({ ...temel, sirketAdi: '' })
    expect(w.find('.fis-baslik').text()).toBe('RASPEL ERP')
  })

  it('fis no yoksa tire gosterilir (bos fis numarasi dikkat cekmeli)', () => {
    const w = kur({ ...temel, fisNo: '' })
    expect(w.find('.fis-fisno').text()).toContain('-------')
  })

  it('logo verilmediyse gorsel render edilmez', () => {
    const w = kur({ ...temel, sirketLogosu: '' })
    expect(w.find('.fis-logo').exists()).toBe(false)
  })

  it('logo verildiyse gorsel render edilir', () => {
    const w = kur({ ...temel, sirketLogosu: 'data:image/png;base64,AAA' })
    expect(w.find('.fis-logo').attributes('src')).toBe('data:image/png;base64,AAA')
  })

  it('musteri seciliyse adi fis ustunde gorunur', () => {
    const w = kur({ ...temel, musteriAdi: 'ABC Ltd' })
    expect(w.find('.fis-musteri').text()).toContain('ABC Ltd')
  })

  it('perakende satis musterisiz olabilir (satir gizlenir)', () => {
    const w = kur({ ...temel, musteriAdi: '' })
    expect(w.find('.fis-musteri').exists()).toBe(false)
  })
})

describe('PosFisOnizleme — kalemler', () => {
  it('her kalem ad, adet ve satir tutari ile listelenir', () => {
    const w = kur(temel)
    const kalemler = w.findAll('.fis-kalem')
    expect(kalemler).toHaveLength(2)
    expect(kalemler[0].text()).toContain('Vida M8')
    expect(kalemler[0].text()).toContain('x3')
    expect(kalemler[0].find('.fis-kalem-tutar').text()).toContain('30,00')
  })

  it('bos sepet fis kalemi gostermez', () => {
    const w = kur({ ...temel, sepet: [] })
    expect(w.findAll('.fis-kalem')).toHaveLength(0)
  })
})

describe('PosFisOnizleme — fiyatli fis', () => {
  it('ara toplam, genel toplam ve odenen tutar gosterilir', () => {
    const w = kur(temel)
    expect(w.find('.fis-toplam').exists()).toBe(true)
    expect(w.find('.fis-toplam-deger').text()).toContain('40,00')
    expect(w.find('.fis-odeme-satir').exists()).toBe(true)
  })

  it('indirim degeri pozitifse indirim satiri gosterilir', () => {
    const w = kur({ ...temel, indirimDegeri: 10, indirimTipi: 'tutar', indirimTutari: 10 })
    const satir = w.find('.fis-indirim')
    expect(satir.exists()).toBe(true)
    expect(satir.text()).toContain('10,00')
  })

  it('indirim yoksa indirim satiri gizlenir', () => {
    const w = kur({ ...temel, indirimDegeri: 0 })
    expect(w.find('.fis-indirim').exists()).toBe(false)
  })

  it('yuzde indirimde yuzde isareti yazilir', () => {
    const w = kur({ ...temel, indirimDegeri: 10, indirimTipi: 'yuzde', indirimTutari: 4 })
    expect(w.find('.fis-indirim').text()).toContain('10%')
  })

  it('kalan tutar pozitifse kalan satiri gosterilir', () => {
    const w = kur({ ...temel, odenenTutar: 25, kalanTutar: 15 })
    expect(w.text()).toContain('15,00')
    // odenen + kalan + urun sayisi + durum
    expect(w.findAll('.fis-odeme-satir')).toHaveLength(4)
  })

  it('kalan tutar yoksa kalan satiri gizlenir', () => {
    const w = kur({ ...temel, odenenTutar: 40, kalanTutar: 0 })
    // odenen + urun sayisi + durum
    expect(w.findAll('.fis-odeme-satir')).toHaveLength(3)
  })

  it('urun sayisi ve odeme durumu fis altinda gosterilir', () => {
    const w = kur({ ...temel, odemeDurumText: 'Ödendi' })
    const durumlar = w.findAll('.fis-odeme-durum')
    expect(durumlar).toHaveLength(2)
    expect(w.text()).toContain('Ödendi')
  })
})

describe('PosFisOnizleme — fiyatsiz fis (Kritik kural)', () => {
  const fiyatsiz = { ...temel, fisFiyatli: false }

  it('kalem tutarlari gosterilmez', () => {
    const w = kur(fiyatsiz)
    expect(w.find('.fis-kalem-tutar').exists()).toBe(false)
  })

  it('ara toplam, indirim ve genel toplam bloklari gizlenir', () => {
    const w = kur(fiyatsiz)
    expect(w.find('.fis-toplam').exists()).toBe(false)
    expect(w.find('.fis-genel-toplam').exists()).toBe(false)
    expect(w.find('.fis-indirim').exists()).toBe(false)
  })

  it('odenen tutar satiri gizlenir', () => {
    const w = kur(fiyatsiz)
    // Yalnizca urun sayisi + durum satirlari kalmali.
    expect(w.findAll('.fis-odeme-satir')).toHaveLength(2)
  })

  it('urun adlari ve adetleri yine gorunur (fis tutarsiz olmamali)', () => {
    const w = kur(fiyatsiz)
    expect(w.findAll('.fis-kalem')).toHaveLength(2)
    expect(w.text()).toContain('Vida M8')
  })
})

describe('PosFisOnizleme — fis alt notu', () => {
  it('ozel alt notu kullanilir', () => {
    const w = kur({ ...temel, fisAltNotu: 'Bizi tercih ettiginiz icin tesekkurler' })
    expect(w.find('.fis-tesekkur').text()).toBe('Bizi tercih ettiginiz icin tesekkurler')
  })

  it('ozel not yoksa varsayilan tesekkur metni yazilir', () => {
    const w = kur({ ...temel, fisAltNotu: '' })
    expect(w.find('.fis-tesekkur').text().length).toBeGreaterThan(0)
  })
})