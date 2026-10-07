import { ref } from 'vue'
import { faturaAPI } from '../api/index.js'
import { unwrapList } from '../api/utils/unwrap.js'
import { getLocalDateString } from '../utils/format.js'

/**
 * POS SATIS SONUCU ve GUNLUK GECMIS.
 *
 * NEDEN AYRI: Satis sonucu (ozet dialogu), gun icinde yapilan satislar ve
 * "son satisi iptal et" akisi `HizliSatis.vue` icinde dagilan bir state'ti.
 * Burada tek sorumluluk: "az once ne sattim, bugun ne sattim, geri alabilir
 * miyim?".
 *
 * NE BURADA DEGIL: Satisi OLUSTURAN orkestrasyon (`satisiTamamlaOnaysiz`,
 * `satisBasarili`) view'da kalir. Cunku o akis sepet + odeme + teslimat +
 * yazdirma + cari olmak uzere ~25 ayri birimi sirayla baglar; buradaki
 * composable'a tasinsaydi 25 bagimliligi enjekte eden bir "god object"
 * olurdu — yani cozmeye calistigimiz sorunun daha kotusu. View'in isi
 * zaten modulleri baglamaktir.
 *
 * @typedef {object} PosSatisGecmisiBagimliliklari
 * @property {Function} t vue-i18n ceviri
 * @property {{basarili: Function, uyari: Function, hata: Function}} bildir
 * @property {import('vue-router').Router} [router] Fatura detayina gitmek icin
 */

export function usePosSatisGecmisi({ t, bildir, router }) {
  /** Satis sonrasi ozet (ozet dialogunu besler). */
  const satisOzet = ref(null)
  const satisOzetDialog = ref(false)
  /** Son olusan fatura; iptal ve "yeniden yazdir" icin tutulur. */
  const sonSatis = ref(null)
  /** Bugun yapilan satislar (F7 dialogu). */
  const gunlukSatislar = ref([])

  /**
   * Ozet verisini uretir. Saf hesap: cagiran taraf guncel tutarlari verir.
   * @param {object} yanit `faturaAPI.create` yaniti
   * @param {object} tutarlar `genelToplam/odenenTutar/kalanTutar/odemeYontemi/paraUstu/fisModu`
   */
  const satisOzetiOlustur = (yanit, tutarlar) => ({
    faturaNo: yanit?.data?.faturaNumarasi,
    toplam: tutarlar.genelToplam,
    odenen: tutarlar.odenenTutar,
    kalan: tutarlar.kalanTutar,
    yontem: tutarlar.odemeYontemi,
    paraUstu: tutarlar.paraUstu,
    fisModu: tutarlar.fisModu
  })

  /** Basarili satistan sonra sonuc durumunu isler ve ozeti gosterir. */
  const satisSonucunuKaydet = (yanit, tutarlar) => {
    sonSatis.value = yanit?.data || null
    satisOzet.value = satisOzetiOlustur(yanit, tutarlar)
    satisOzetDialog.value = true
  }

  /** Ozet dialogunu kapatip durumu temizler (yeni satisa gecis). */
  const satisOzetiKapat = () => {
    satisOzetDialog.value = false
    satisOzet.value = null
  }

  /** Bugunun satislarini sunucudan ceker (bugun + SATIS, tek sayfa). */
  const gunlukSatislariYukle = async () => {
    try {
      const bugun = getLocalDateString()
      const r = await faturaAPI.getAll({ size: 200, tur: 'SATIS', bas: bugun, bit: bugun })
      gunlukSatislar.value = unwrapList(r)
    } catch {
      gunlukSatislar.value = []
    }
  }

  /**
   * Gunluk listeden faturayi acar (detay / yeniden yazdirma).
   * @param {{id?: number}} satis
   */
  const bugunkuSatisGoruntule = (satis) => {
    if (satis?.id) router?.push?.(`/faturalar/${satis.id}`)
  }

  /**
   * Son satisi iptal eder.
   *
   * Iptal sonrasi dis dunyada yapilmasi gerekenler (stok onbellegi tazeleme,
   * kasa listesi) `onIptal` geri cagrisiyla DISARIDA tutulur; boylece bu
   * composable stok/kasa store'larina bagimli olmaz.
   *
   * @param {Function} [onIptal] basarili iptalden sonra cagrilir
   * @returns {Promise<boolean>} iptal gerceklesti mi
   */
  const sonSatisiIptalEt = async (onIptal) => {
    if (!sonSatis.value?.id) {
      bildir.uyari(t('hizliSatis.geriAlinacakSatisYok'))
      return false
    }
    try {
      await faturaAPI.updateDurum(sonSatis.value.id, 'IPTAL')
    } catch (err) {
      bildir.hata(err?.response?.data?.message || t('hizliSatis.iptalBasarisiz'))
      return false
    }
    // Iptal GERCEKLESTI; bundan sonrasi yalniz tazeleme.
    bildir.basarili(t('hizliSatis.sonSatisIptalEdildi'))
    sonSatis.value = null
    await gunlukSatislariYukle()
    // Tazeleme geri cagrisi (stok onbellegi, kasa listesi…) hata verirse bu,
    // iptalin basarisiz oldugu anlamina GELMEZ; ayri ele alinir.
    try {
      await onIptal?.()
    } catch {
      /* tazeleme hatasi iptali etkilemez */
    }
    return true
  }

  return {
    satisOzet,
    satisOzetDialog,
    sonSatis,
    gunlukSatislar,
    satisOzetiOlustur,
    satisSonucunuKaydet,
    satisOzetiKapat,
    gunlukSatislariYukle,
    bugunkuSatisGoruntule,
    sonSatisiIptalEt
  }
}
