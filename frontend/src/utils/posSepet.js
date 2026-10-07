/**
 * POS sepet satiri kurallari — saf fonksiyonlar.
 *
 * SORUN: Sepet satirlarinin benzersizlik anahtari belirsizdi. "Ayni urun" diye
 * bulmak YETMIYORDU; cunku ayni urun FARKLI fiyat tipleriyle birden fazla
 * satirda bulunabilir (5 perakende + 3 toptan). Yalniz-id aramasi:
 *   - Karttan yeniden ekleme `find` ile SADECE ILK satiri buluyor ve
 *     kopyadaki miktari artirmiyordu ("ekledim ama artmadi"),
 *   - fiyat listesi / cari son-alis zenginlestirmesi YANLIS satira yaziliyordu,
 *   - karttaki adet rozeti tum satirlar yerine ilk satiri gosteriyordu,
 *   - "Cogalt" ayni fiyat tipini tekrarlayip yukaridaki karisikligi uretiyordu.
 *
 * KURAL: bir sepet satirinin kimligi `(stokId + fiyatTipi)` ciftidir.
 */

/** Sepette belirli bir urun + fiyat tipi satirini bulur. */
export const sepetSatiriBul = (sepet, id, fiyatTipi) =>
  sepet.find((i) => i.id === id && i.fiyatTipi === fiyatTipi)

/** Bir urunun sepette kullandigi fiyat tipleri kumesi. */
export const kullanilanFiyatTipleri = (sepet, id) =>
  new Set(sepet.filter((i) => i.id === id).map((i) => i.fiyatTipi))

/**
 * "Cogalt" icin kullanilabilecek ILK alternatif fiyat tipi.
 *
 * Ayni fiyat tipini tekrarlamak yeni bir satir anlamina gelmez (kuantum
 * birikmesi gerekir); cogaltmanin varlik sebebi ayni urunu BASKA bir fiyatla
 * satmak. Alternatif yoksa `null` doner ve cagiran taraf cogaltmamalidir.
 */
export const sonrakiFiyatTipi = (fiyatlar, kullanilanTipler) =>
  (fiyatlar || []).find((f) => !kullanilanTipler.has(f.ad)) || null

/** Ayni urunun tum satirlarindaki toplam adet (kart rozeti icin). */
export const sepetteToplamAdet = (sepet, id) =>
  sepet.filter((i) => i.id === id).reduce((t, i) => t + (Number(i.miktar) || 0), 0)
