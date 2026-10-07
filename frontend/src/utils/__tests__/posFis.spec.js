import { describe, it, expect } from 'vitest'
import { posFisHtml, agirlikMetni, teslimDurumEtiketi, fisStili } from '../posFis.js'

/**
 * POS FIS HTML URETIMI — birim testleri.
 *
 * `posFisHtml` saf bir fonksiyondur: state almez, HTML string'i dondurur.
 * Buradaki testler fisin YAZDIRMA KURALLARINI sabitler; ozellikle fiyatsiz
 * fis modu kritiktir (fiyatsiz fis yazdirilip fiyatsiz satis kaydedilmesinin
 * onu bu fonksiyonun ciktisidir).
 *
 * `t` gercek vue-i18n yerine karsi lastik fonksiyon verilir; boylece test
 * ceviri katmanindan bagimsiz calisir.
 */

const t = (anahtar) => `[${anahtar}]`

const temel = {
  t,
  sepet: [{ ad: 'Vida M8', miktar: 3, fiyat: 10 }],
  fiyatli: true,
  toplam: 30,
  genelToplam: 30,
  odenenTutar: 30,
  sirketAdi: 'Raspel Ltd',
  simdikiTarih: '05.10.2026 14:30',
  fisNo: 'FTR-2026-0001'
}

describe('posFisHtml — belge iskeleti', () => {
  it('tam HTML dokumani dondurur', () => {
    const html = posFisHtml(temel)
    expect(html.startsWith('<!DOCTYPE html>')).toBe(true)
    expect(html).toContain('<meta charset="UTF-8">')
    expect(html.trimEnd().endsWith('</html>')).toBe(true)
  })

  it('yazdir ve kapat dugmeleri barindirir', () => {
    const html = posFisHtml(temel)
    expect(html).toContain('window.print()')
    expect(html).toContain('[hizliSatis.yazdir]')
    expect(html).toContain('window.close()')
  })

  it('sirket adi verilmezse varsayilan baslik yazilir', () => {
    expect(posFisHtml({ ...temel, sirketAdi: '' })).toContain('RASPEL ERP')
  })

  it('fis genisligi 58mm/80mm olarak stile gecer', () => {
    expect(fisStili('58')).toContain('58mm')
    expect(fisStili('80')).toContain('80mm')
    // Gecersiz genislik 80'e duser
    expect(fisStili('42')).toContain('80mm')
  })

  it('yazdirilirken arac cubugu gizlenir (fise kirletmesin)', () => {
    expect(fisStili('80')).toMatch(/@media print\s*\{[\s\S]*?\.aracubuk\s*\{\s*display:\s*none !important/)
  })
})

describe('posFisHtml — kalemler', () => {
  it('kalem ad, adet ve tutarini yazar', () => {
    const html = posFisHtml(temel)
    expect(html).toContain('Vida M8 x3')
    expect(html).toContain('30,00')
  })

  it('urun adindaki HTML karakterleri escelenir (fis kirilmaz)', () => {
    const html = posFisHtml({
      ...temel,
      sepet: [{ ad: '<script>alert(1)</script>', miktar: 1, fiyat: 1 }]
    })
    expect(html).not.toContain('<script>alert(1)</script>')
    expect(html).toContain('&lt;script&gt;')
  })

  it('fiyatsiz modda kalem tutari gizlenir ama ad/adet gorunur', () => {
    const html = posFisHtml({ ...temel, fiyatli: false })
    expect(html).toContain('Vida M8 x3')
    expect(html).not.toContain('30,00')
  })

  it('agirlik toplami varsa satir eklenir', () => {
    expect(posFisHtml({ ...temel, toplamAgirlik: 12.5 })).toContain('12.50 kg')
    // Agirlik yoksa satir hic yazilmaz (bos "0.00 kg" satiri fisleri kirletir).
    expect(posFisHtml({ ...temel, toplamAgirlik: 0 })).not.toContain('[hizliSatis.toplamAgirlik]')
  })
})

describe('posFisHtml — fiyatli ozet', () => {
  it('ara toplam, genel toplam ve odenen yazilir', () => {
    const html = posFisHtml(temel)
    expect(html).toContain('[hizliSatis.araToplam]')
    expect(html).toContain('[hizliSatis.fisGenelToplam]')
    expect(html).toContain('[hizliSatis.fisOdenen]')
  })

  it('indirim degeri pozitifse indirim satiri yazilir', () => {
    const html = posFisHtml({ ...temel, indirimDegeri: 5, indirimTipi: 'tutar', indirimTutari: 5 })
    expect(html).toContain('[hizliSatis.indirim]')
    // Indirim eksi isaretli gosterilir: `-` + formatCurrency(5) = `-₺5,00`
    expect(html).toContain('-₺5,00')
  })

  it('yuzde indirimde yuzde isareti yazilir', () => {
    const html = posFisHtml({ ...temel, indirimDegeri: 10, indirimTipi: 'yuzde', indirimTutari: 3 })
    expect(html).toContain('(10%)')
  })

  it('indirim yoksa indirim satiri yazilmaz', () => {
    expect(posFisHtml({ ...temel, indirimDegeri: 0 })).not.toContain('[hizliSatis.indirim]')
  })

  it('kalan tutar pozitifse kalan satiri yazilir', () => {
    const html = posFisHtml({ ...temel, odenenTutar: 20, kalanTutar: 10 })
    expect(html).toContain('[hizliSatis.fisKalan]')
    expect(html).toContain('10,00')
  })

  it('kalan tutar yoksa kalan satiri yazilmaz', () => {
    const html = posFisHtml({ ...temel, odenenTutar: 30, kalanTutar: 0 })
    expect(html).not.toContain('[hizliSatis.fisKalan]')
  })
})

describe('posFisHtml — fiyatsiz ozet (KRITIK kural)', () => {
  const fiyatsiz = { ...temel, fiyatli: false }

  it('hicbir tutar satiri yazilmaz', () => {
    const html = posFisHtml({ ...fiyatsiz, toplam: 30, odenenTutar: 20, kalanTutar: 10, indirimDegeri: 5 })
    expect(html).not.toContain('[hizliSatis.araToplam]')
    expect(html).not.toContain('[hizliSatis.fisGenelToplam]')
    expect(html).not.toContain('[hizliSatis.fisOdenen]')
    expect(html).not.toContain('[hizliSatis.fisKalan]')
    expect(html).not.toContain('[hizliSatis.indirim]')
  })

  it('urun sayisi ve odeme durumu yine yazilir (fis tutarsiz olmamali)', () => {
    const html = posFisHtml({ ...fiyatsiz, odemeDurumText: 'Taksitli' })
    expect(html).toContain('[hizliSatis.toplamUrun]')
    expect(html).toContain('Taksitli')
  })
})

describe('posFisHtml — musteri, teslimat ve irtibat', () => {
  it('musteri adi varsa fis ustunde yazilir', () => {
    expect(posFisHtml({ ...temel, musteriAdi: 'ABC Ltd' })).toContain('[hizliSatis.fisMusteri] ABC Ltd')
  })

  it('perakende satis musterisiz olabilir', () => {
    expect(posFisHtml({ ...temel, musteriAdi: '' })).not.toContain('[hizliSatis.fisMusteri]')
  })

  it('teslim eden ve teslim notu yazilir', () => {
    const html = posFisHtml({ ...temel, teslimEden: 'Ali V.', teslimNotu: 'Kapida birakildi' })
    expect(html).toContain('[hizliSatis.teslimEden]: Ali V.')
    expect(html).toContain('[hizliSatis.not]: Kapida birakildi')
  })

  it('teslim durumu BEKLIYOR degilse etiketiyle yazilir', () => {
    const yolda = posFisHtml({ ...temel, teslimDurumu: 'YOLDA' })
    expect(yolda).toContain('[hizliSatis.teslimEtiketi]: [faturalar.durumYolda]')

    const bekliyor = posFisHtml({ ...temel, teslimDurumu: 'BEKLIYOR' })
    expect(bekliyor).not.toContain('[hizliSatis.teslimEtiketi]')
  })

  it('satici adi fis altinda yazilir, yoksa tire konur', () => {
    expect(posFisHtml({ ...temel, saticiAdi: 'Kasi' })).toContain('[hizliSatis.islemYapan] Kasi')
    expect(posFisHtml({ ...temel, saticiAdi: '' })).toContain('[hizliSatis.islemYapan] -')
  })

  it('logo varsa gorsel eklenir', () => {
    expect(posFisHtml({ ...temel, sirketLogosu: 'data:image/png;base64,AAA' })).toContain('<img class="logo"')
    expect(posFisHtml(temel)).not.toContain('<img class="logo"')
  })

  it('logo kaynagi escelenir', () => {
    const html = posFisHtml({ ...temel, sirketLogosu: 'x" onerror="alert(1)' })
    expect(html).not.toContain('onerror="alert(1)"')
    expect(html).toContain('&quot;')
  })
})

describe('agirlikMetni', () => {
  it('1 ton alti kg olarak yazilir', () => {
    expect(agirlikMetni(12.5)).toBe('12.50 kg')
    expect(agirlikMetni(999.999)).toBe('1000.00 kg')
  })

  it('1 ton ve uzeri ton olarak yazilir', () => {
    expect(agirlikMetni(1000)).toBe('1.000 ton')
    expect(agirlikMetni(2500)).toBe('2.500 ton')
  })

  it('gecersiz girdi 0 kg verir', () => {
    expect(agirlikMetni(null)).toBe('0.00 kg')
    expect(agirlikMetni('abc')).toBe('0.00 kg')
  })
})

describe('teslimDurumEtiketi', () => {
  it('bilinen durumlari etiket cevirir', () => {
    expect(teslimDurumEtiketi('BEKLIYOR', t)).toBe('[faturalar.durumBekliyor]')
    expect(teslimDurumEtiketi('YOLDA', t)).toBe('[faturalar.durumYolda]')
    expect(teslimDurumEtiketi('TESLIM_EDILDI', t)).toBe('[faturalar.durumTeslimEdildi]')
  })

  it('bilinmeyen durum oldugu gibi doner (veri kaybi yok)', () => {
    expect(teslimDurumEtiketi('OTURUMDA', t)).toBe('OTURUMDA')
  })

  it('t verilmezse kod degeri doner', () => {
    expect(teslimDurumEtiketi('YOLDA')).toBe('YOLDA')
  })
})