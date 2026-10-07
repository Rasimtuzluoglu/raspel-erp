/**
 * POS adet (miktar) kurallari — saf fonksiyonlar.
 *
 * SORUN: Sepetteki miktar alani `v-model.number` ile dogrudan state'e
 * yaziliyordu. Bunun sonucu:
 *   - `0` veya `-3` yazilabiliyordu (negatif toplam, satir kaybolmasi),
 *   - `min="1"` sadece tarayici spinner'ini kisitliyordu, elle yazimi degil,
 *   - `Enter` tusu hicbir sey yapmiyordu (odak kaybi sessizce gecidiyordu),
 *   - depoda 3 olan urun sepete 500 olarak eklenebiliyordu.
 *
 * Buradaki fonksiyonlar o kuralin TEK yeridir: hem gorsel (buton/giris),
 * hem klavyeden (Alt+ok) gelen HER miktar degisikligi ayni kuradan gecer.
 *
 * ONDALIK: Kg/m2 gibi olculerek satilan urunlerde 0.5'lik adim mantiklidir
 * (yarim kg beton, 1.5 m2 kereste). Tam sayi birimlerde adim 1'dir.
 *
 * STOK TAVANI: Backend `FaturaService` satista `Stok.miktar` ile karsilastirir
 * ve yetersizse "Yetersiz stok!" ile islemi reddeder. Buradaki tavan ayni
 * sayiyi kullanir; amac sunucuya gitmeden hatayi kasada gormektir. (Stok
 * baska bir cihazda degistiyse anlik gecersiz olabilir; son söz sunucudur.)
 */

/**
 * Olculerek satilan birimler: bu birimlerde adim 0.5.
 * Kucuk harf, Turkce ve kisa yazim normalize edilerek eslesir.
 */
const OLCULEN_BIRIMLER = new Set([
  'kg', 'kilo', 'kilogram', 'gr', 'gram', 'ton',
  'lt', 'l', 'litre', 'ml',
  'm', 'metre', 'm2', 'm3', 'm²', 'm³', 'dm3', 'cm3', 'cm2',
  'metrekare', 'metrekup', 'metre_kare', 'metre_kup'
])

/** Birim adini normalize eder: kucuk harf + bosluk/tire sadelestirme. */
const birimNormalize = (birim) =>
  String(birim ?? '')
    .trim()
    .toLocaleLowerCase('tr-TR')
    .replace(/\s+/g, '')
    .replace(/-/g, '_')

/**
 * Birim icin miktar adimi.
 * @param {string} [birim]
 * @returns {number} 0.5 veya 1
 */
export function adimBirimIcin(birim) {
  return OLCULEN_BIRIMLER.has(birimNormalize(birim)) ? 0.5 : 1
}

/**
 * Birim icin en kucuk satilabilir miktar (adimla ayni).
 * @param {string} [birim]
 * @returns {number}
 */
export function minMiktarBirimIcin(birim) {
  return adimBirimIcin(birim)
}

/** Sayiyi adimin kati olan en yakin degerle yuvarlar (ondalik hata onlemi). */
export const adimaYuvarla = (deger, adim = 1) => {
  if (!Number.isFinite(deger)) return adim
  const katsayi = Math.round(1 / adim)
  return Math.round(deger * katsayi) / katsayi
}

/** Adet sorunlarinin makine tarafindan okunabilir kodlari. */
export const ADET_SORUN = {
  GECERSIZ: 'gecersiz',
  ASIM: 'asim',
  ADIM_UYUSMUYOR: 'adimUyusmuyor'
}

/**
 * Miktar degistigini dogrular ve guvenli degere indirger.
 *
 * @param {object} p
 * @param {number|string} p.istenen Kullanici/girdi degeri
 * @param {number} [p.stokMiktari] Depodaki mevcut miktar (null = bilinmiyor, tavan yok)
 * @param {string} [p.birim] Urun birimi (adim icin)
 * @returns {{ miktar: number, sorun: string|null, asim: boolean, degisti: boolean, enFazla: number|null }}
 *   `miktar` her zaman GECERLI ve uygulanabilir bir degerdir; cagiran taraf
 *   `sorun` null ise sessizce uygulayabilir, degilse `miktar` sinirlandirilmis
 *   degerdir ve kullaniciya aciklanmalidir.
 */
export function miktarDogrula({ istenen, stokMiktari = null, birim = 'adet' }) {
  const adim = adimBirimIcin(birim)
  const min = minMiktarBirimIcin(birim)

  const hamSayi = typeof istenen === 'number' ? istenen : parseFloat(String(istenen ?? '').replace(',', '.'))
  // NaN / Infinity / negatif / sifir: gecersiz
  if (!Number.isFinite(hamSayi) || hamSayi <= 0) {
    return { miktar: min, sorun: ADET_SORUN.GECERSIZ, asim: false, degisti: true, enFazla: stokMiktari ?? null }
  }

  // Ondalik adim kurali: adim 1 ise deger zaten tam sayiya yuvarlanir.
  const yuvarlanmis = adimaYuvarla(hamSayi, adim)
  const adimUymadi = yuvarlanmis !== hamSayi

  let miktar = yuvarlanmis
  if (miktar < min) miktar = min

  let asim = false
  if (stokMiktari != null && Number.isFinite(Number(stokMiktari))) {
    const tavan = adimaYuvarla(Number(stokMiktari), adim)
    if (miktar > tavan) {
      miktar = tavan > 0 ? tavan : min
      asim = true
    }
  }

  return {
    miktar,
    sorun: asim ? ADET_SORUN.ASIM : adimUymadi ? ADET_SORUN.ADIM_UYUSMUYOR : null,
    asim,
    degisti: true,
    enFazla: stokMiktari ?? null
  }
}

/**
 * Adim kadar artirir/azaltir; sonuc `miktarDogrula` ile gecerlendirir.
 * Artis sonunda tavani asarsa DEGER DEGISTIRMEZ (sessizce klip edilmez),
 * cunki cagiran taraf kullaniciya aciklamali.
 *
 * @param {object} p
 * @param {number} p.mevcut Mevcut miktar
 * @param {1|-1} p.yon
 * @param {string} [p.birim]
 * @param {number|null} [p.stokMiktari]
 * @returns {{ miktar: number, sorun: string|null, asim: boolean, degisti: boolean, enFazla: number|null }}
 *   `degisti:false` => uygulanacak degisiklik yok.
 */
export function adimla({ mevcut, yon, birim = 'adet', stokMiktari = null }) {
  const adim = adimBirimIcin(birim)
  const hedef = Number(mevcut || 0) + adim * yon
  const sonuc = miktarDogrula({ istenen: hedef, stokMiktari, birim })
  // Ayni degere varildiysa (tavan/min) "degisiklik yok" de.
  const ayni = adimaYuvarla(Number(mevcut || 0), adim) === sonuc.miktar
  return { ...sonuc, degisti: !ayni }
}