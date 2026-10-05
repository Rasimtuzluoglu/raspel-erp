import { kdvOrani, kalemTutar } from './faturaHesapla.js'

/**
 * Bir satis kalemini API'nin bekledigi tek bicime indirger. KDV orani ve satir
 * tutari tek kanonik kaynaktan (faturaHesapla.js) gelir; boylece HizliSatis,
 * Satis ve Faturalar ekranlari ayni sekilde hesaplar.
 *
 * ISKONTO ALANI KRITIK: Kullanici iskonto girmediyse `iskontoOrani` payload'a
 * HIC YAZILMAZ (null kalir). Backend'de `FaturaService` null gordugunde
 * `IskontoMotoruService.iskontoHesapla(...)` calistirir; yani sirketin kademeli
 * indirim kurallari (kategori / cari / adet esikleri) devreye girer.
 *
 * Once burada `?? 0` vardi ve her ekran `0` gonderiyordu. Backend null bekledigi
 * icin indirim motoru HIC CALISMIYORDU — kurallar tanimli olsa bile satista
 * uygulanmazdi. `0` ile `null`in farki kritiktir; sifirdan buyuk bir deger
 * gonderilirse motor yine devreye girmez (elle indirim kastedilmiştir).
 */
export function normalizeKalem(k = {}) {
  const normalized = {
    stokId: k.stokId ?? null,
    aciklama: k.aciklama ?? k.ad ?? '',
    adet: k.adet ?? k.miktar ?? 1,
    birimFiyat: k.birimFiyat ?? k.fiyat ?? 0,
    kdvOrani: kdvOrani(k)
  }
  // Iskonto yalnizca ANLAMLI bir deger varsa gonderilir: null/'' -> motor calisir,
  // 0 -> kullanici bilerek sifir indirim istiyor, >0 -> elle indirim.
  const iskonto = k.iskontoOrani
  if (iskonto !== null && iskonto !== undefined && iskonto !== '') {
    normalized.iskontoOrani = Number(iskonto)
  }
  // Kalem kimligi yalnizca duzenlemede gonderilir (stok id ile karismasin).
  if (k.id != null) normalized.id = k.id
  normalized.tutar = kalemTutar(normalized)
  return normalized
}

/**
 * Fatura/satis payload'ini uretir. Toplamlar (araToplam/kdv/genelToplam) backend'de
 * kanonik olarak kalemlerden yeniden hesaplandigi icin gonderilmez; yalnizca cekirdek
 * alanlar, normalize kalemler ve cagirana ozel `ekstra` alanlar tasinir.
 */
export function satisPayloadUret({
  kalemler = [],
  cariHesapAdi,
  genelIskontoTutari,
  indirim,
  ekstra = {},
  ...cekirdek
} = {}) {
  const payload = { ...cekirdek }
  if (cariHesapAdi != null) payload.cariHesapAdi = cariHesapAdi
  if (genelIskontoTutari !== undefined) payload.genelIskontoTutari = genelIskontoTutari
  if (indirim !== undefined) payload.indirim = indirim
  payload.kalemler = kalemler.map(normalizeKalem)
  Object.assign(payload, ekstra)
  return payload
}
