import { describe, it, expect } from 'vitest'
import { normalizeKalem, satisPayloadUret } from '../satisPayload.js'

describe('satisPayload.js', () => {
  it('normalizeKalem farkli alan adlarini tek bicime indirger', () => {
    const k = normalizeKalem({ ad: 'Ürün', miktar: 2, fiyat: 60 })
    expect(k.aciklama).toBe('Ürün')
    expect(k.adet).toBe(2)
    expect(k.birimFiyat).toBe(60)
    expect(k.kdvOrani).toBe(20)
    expect(k.stokId).toBeNull()
    // KDV dahil satir tutari
    expect(k.tutar).toBe(120)
  })

  // KRITIK: Iskonto kullanicinin GIRMEDIGI durumda null gonderilmelidir. Backend
  // (`FaturaService`) null gordugunde `IskontoMotoruService` calistirir; `0`
  // gondermek motoru tamamen atlar ve kademeli indirim kurallari hic uygulanmaz.
  it('normalizeKalem iskonto girilmediginde alani HIC gondermez (motor calissin)', () => {
    const k = normalizeKalem({ ad: 'Ürün', miktar: 2, fiyat: 60 })
    expect('iskontoOrani' in k).toBe(false)
    // Ekran tutari yine de iskontosuz brut olur (motor uygulanmamis varsayim)
    expect(k.tutar).toBe(120)
  })

  it('normalizeKalem yalnizca ANLAMLI iskonto degerini gonderir', () => {
    // Kullanici bilerek sifir indirim istiyor -> 0 gonderilir
    expect(normalizeKalem({ iskontoOrani: 0 }).iskontoOrani).toBe(0)
    // Elle indirim -> deger gonderilir
    expect(normalizeKalem({ iskontoOrani: 15 }).iskontoOrani).toBe(15)
    // Bos string (input temizlenmis) -> motor calissin
    expect('iskontoOrani' in normalizeKalem({ iskontoOrani: '' })).toBe(false)
    // null -> motor calissin
    expect('iskontoOrani' in normalizeKalem({ iskontoOrani: null })).toBe(false)
  })

  it('normalizeKalem kalem kimligini yalnizca varsa tasir', () => {
    expect(normalizeKalem({ aciklama: 'x', adet: 1, birimFiyat: 10 }).id).toBeUndefined()
    expect(normalizeKalem({ id: 7, aciklama: 'x', adet: 1, birimFiyat: 10 }).id).toBe(7)
  })

  it('satisPayloadUret kalemleri normalize eder ve toplam alanlarini gondermez', () => {
    const payload = satisPayloadUret({
      cariHesapId: 3,
      tur: 'SATIS',
      durum: 'KESILDI',
      tarih: '2026-01-01',
      aciklama: 'test',
      kalemler: [{ aciklama: 'A', adet: 1, birimFiyat: 120 }]
    })
    expect(payload.cariHesapId).toBe(3)
    expect(payload.kalemler).toHaveLength(1)
    expect(payload.kalemler[0].tutar).toBe(120)
    expect(payload.araToplam).toBeUndefined()
    expect(payload.kdv).toBeUndefined()
    expect(payload.genelToplam).toBeUndefined()
  })

  it('opsiyonel alanlari yalnizca verilince ekler', () => {
    const yok = satisPayloadUret({ kalemler: [] })
    expect('cariHesapAdi' in yok).toBe(false)
    expect('indirim' in yok).toBe(false)
    expect('genelIskontoTutari' in yok).toBe(false)

    const var_ = satisPayloadUret({ cariHesapAdi: 'Perakende', indirim: 5, genelIskontoTutari: 0, kalemler: [] })
    expect(var_.cariHesapAdi).toBe('Perakende')
    expect(var_.indirim).toBe(5)
    expect(var_.genelIskontoTutari).toBe(0)
  })

  it('ekstra alanlari payload ile birlestirir', () => {
    const payload = satisPayloadUret({
      tur: 'SATIS',
      kalemler: [],
      ekstra: { kasaId: 9, kartaBankaAktar: true }
    })
    expect(payload.kasaId).toBe(9)
    expect(payload.kartaBankaAktar).toBe(true)
  })
})
