import { ref, computed, watch } from 'vue'
import { safeGet, safeSet } from '../utils/safeStorage.js'

/**
 * POS ekrani tercihleri — kalici kasa ayarlari.
 *
 * SORUN: `HizliSatis.vue` icinde 14 ayri `localStorage` anahtari vardi ve
 * kalicilik UC farkli sekilde yonetiliyordu:
 *   1) `watch(ref, v => localStorage.setItem(...))`  -> musteriAcik, teslimatAcik
 *   2) `@change="xKaydet"` ile elle cagri            -> buyukYazi, onayIste,
 *                                                       otomatikYazdir, ipucu
 *   3) `@click` icinde dogrudan `setItem`            -> ipucuKapat, sepetAcik
 *   4) hic yazilmayanlar                             -> fisAcik, detayAcik
 *
 * Ayni kuralin birden fazla uygulamasi, bir tercihin "kalici mi" oldugunu
 * bulmayi zorlastiriyordu; ipucu anahtari uc ayri yerde yaziliyordu.
 *
 * COZUM: `kalici()` yardimcisi. Deger ve anahtar ver; okuma, yazma ve
 * kalicilik otomatik olsun. "Bu tercih ne zaman kaydedilir?" sorusunun cevabi
 * artik her zaman ayni: degistigi anda.
 *
 * ONDIS FORMAT: `safeStorage` `JSON.stringify` kullandigi icin boolean'lar
 * `"true"`/`"false"` olarak yazilir — eski kodun `String(x)` + `=== 'true'`
 * okumasiyla ayni format. Operatorun kayitli ayarlari korunur.
 */

/** Tercih listesi: [ref adi, localStorage anahtari, varsayilan, aciklama].
 *  Varsayilanlar kasada "temiz" akis verir: musteri -> sepet -> odeme her
 *  zaman gorunur, gelismis alanlar (teslimat/fis/detay) kapali gelir. */
const TERCILER = [
  // --- gorunurluk / katlanir bolumler ---
  ['musteriAcik', 'raspel_pos_musteri_acik', true, 'Musteri blogu (adim 1)'],
  ['sepetAcik', 'raspel_pos_sepet_acik', true, 'Sepet blogu (adim 2)'],
  ['odemeAcik', 'raspel_pos_odeme_acik', true, 'Odeme blogu (adim 3)'],
  ['teslimatAcik', 'raspel_pos_teslimat_acik', false, 'Teslimat blogu (gelismis)'],
  ['fisAcik', 'raspel_pos_fis_acik', false, 'Fis onizleme blogu'],
  ['detayAcik', 'raspel_pos_detay_acik', false, 'Sepet icin ft3/indirim detayi'],

  // --- kullanilabilirlik / akis ---
  ['buyukYazi', 'raspel_pos_buyuk_yazi', false, 'Buyuk yazi modu (uzak mesafe)'],
  ['onayIste', 'raspel_pos_onay_iste', false, 'Satis oncesi onay adimi'],
  ['otomatikYazdir', 'raspel_pos_otomatik_yazdir', true, 'Satis sonrasi fis otomatik yazdirilir'],
  ['ipucuAcik', 'raspel_pos_ipucu_acik', false, 'Kisayol ipucu seridi']
]

/** Fis ayarlari: sunucudan da gelebilen alanlar (cihazlar arasi ayni olsun). */
const FIS_AYARLARI = [
  ['fisFiyatli', 'raspel_fis_fiyatli', true, 'Fiyatli fis (false = fiyatsiz)'],
  ['fisGenislik', 'raspel_fis_genislik', '80', 'Yazici genisligi (58 | 80)']
]

export const POS_TERCIH_LISTESI = TERCILER
export const POS_FIS_AYARLARI = FIS_AYARLARI

/**
 * @param {{ t?: Function }} [secenek] `t` = vue-i18n ceviri (fis alt notu varsayilani icin)
 */
export function usePosTercih({ t } = {}) {
  const refler = {}

  /** Bir tercihi kalici ref'e cevirir: okuma + degisince yazma. */
  const kalici = (ad, anahtar, varsayilan) => {
    const r = ref(safeGet(anahtar, varsayilan))
    watch(r, (v) => safeSet(anahtar, v))
    refler[ad] = r
    return r
  }

  for (const [ad, anahtar, varsayilan] of TERCILER) kalici(ad, anahtar, varsayilan)
  for (const [ad, anahtar, varsayilan] of FIS_AYARLARI) kalici(ad, anahtar, varsayilan)

  /**
   * Fis alt notu: metin oldugu icin JSON'a yazilir (bos string de gecerli bir
   * degerdir, `||` ile varsayilana dusmemelidir).
   */
  const fisAltNotu = ref(safeGet('raspel_fis_notu', '') || t?.('hizliSatis.fisAltNotVarsayilan') || '')
  watch(fisAltNotu, (v) => safeSet('raspel_fis_notu', v))

  /** Panel basliklarindaki katlanir oklar icin: degeri tersine cevirir. */
  const degistir = (ad) => {
    const r = refler[ad]
    r.value = !r.value
    return r.value
  }

  /** Ipucunu kapatir (`?` veya panel tusu ile tekrar acilabilir). */
  const ipucuKapat = () => {
    refler.ipucuAcik.value = false
  }

  /**
   * Sunucudan gelen fis ayarlarini uygular.
   *
   * Sirket ayarlarinda tanimli genislik/alt notu her cihazda ayni olsun istenir;
   * operatorin yerel degisikligi de korunmali. Bu yuzden yalniz `null`/
   * `undefined` gelen alanlar yazilir — sunucu deger gondermezse o alan oyuncunun
   * kendi secimi kalir.
   *
   * @param {object} a Sunucudan gelen ayarlar
   */
  const sunucuAyarlariniUygula = (a = {}) => {
    for (const [ad] of FIS_AYARLARI) {
      if (a[ad] !== null && a[ad] !== undefined) refler[ad].value = a[ad]
    }
    if (a.fisAltNotu) fisAltNotu.value = a.fisAltNotu
  }

  /** Tercih adlarini listeler (tanilama/ayar ekrani icin). */
  const tercihOzeti = computed(() =>
    TERCILER.map(([ad, anahtar, varsayilan, aciklama]) => ({
      ad,
      anahtar,
      varsayilan,
      aciklama,
      deger: refler[ad].value
    }))
  )

  return {
    ...refler,
    fisAltNotu,
    degistir,
    ipucuKapat,
    sunucuAyarlariniUygula,
    tercihOzeti
  }
}