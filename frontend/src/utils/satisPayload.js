import { kdvOrani, kalemTutar } from './faturaHesapla.js'

/**
 * Bir satis kalemini API'nin bekledigi tek bicime indirger. KDV orani ve satir
 * tutari tek kanonik kaynaktan (faturaHesapla.js) gelir; boylece HizliSatis,
 * Satis ve Faturalar ekranlari ayni sekilde hesaplar.
 */
export function normalizeKalem(k = {}) {
  const normalized = {
    stokId: k.stokId ?? null,
    aciklama: k.aciklama ?? k.ad ?? '',
    adet: k.adet ?? k.miktar ?? 1,
    birimFiyat: k.birimFiyat ?? k.fiyat ?? 0,
    iskontoOrani: k.iskontoOrani ?? 0,
    kdvOrani: kdvOrani(k)
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
