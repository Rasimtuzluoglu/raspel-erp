import { ref, computed } from 'vue'
import { miktarDogrula, adimla, adimBirimIcin, ADET_SORUN } from '../utils/posAdet.js'
import { sepetSatiriBul as posSatiriBul, kullanilanFiyatTipleri, sonrakiFiyatTipi, sepetteToplamAdet } from '../utils/posSepet.js'

/**
 * POS SEPET CEKIRDEGI — satir durumu, islemler, geri alma ve toplamlar.
 *
 * NEDEN AYRI: Bu mantik `HizliSatis.vue` icinde 400+ satirdi ve view'in geri
 * kalaniyla ic ice girmisti; sepet davranisini anlamak icin 2900 satirlik
 * dosyada gezinmek gerekiyordu. Burada sepet TEK bir sorumluluk: satirlari
 * tutmak ve uzerinde islem yapmak.
 *
 * ADET KURALLARI: dogrulama/stok tavani `utils/posAdet.js` icinde saf
 * fonksiyonlardir; buradaki `miktarDegistir` TEK giris noktasidir (buton,
 * girdi ve klavye ayni yoldan gecer).
 *
 * GERI ALMA: 8 sn'lik iki pencere vardir — "satir" (sil/ cogalt) ve "sepet"
 * (tumunu temizle). `geriAlYap` once SATIR penceresini dener; yeni islem
 * daha kisa omurlu oldugu icin oncelik bilinclidir.
 *
 * @typedef {object} PosSepetBagimliliklari
 * @property {Function} t vue-i18n ceviri
 * @property {{uyari: Function}} bildir toast kanali
 * @property {import('vue').Ref} seciliMusteri cari (fiyat gecmisi icin)
 * @property {Function} urunFiyatlariniYukleTek stok fiyat listesi getirir
 * @property {Function} cariUrunFiyatGecmisi cari urun fiyat gecmisi API'si
 */

const GERI_AL_PENCERE_MS = 8000

export function usePosSepet({ t, bildir, seciliMusteri, urunFiyatlariniYukleTek, cariUrunFiyatGecmisi }) {
  // --- state ---
  const sepet = ref([])
  const aktifSatir = ref(-1)
  const vurguluId = ref(null)
  const geriAlSatir = ref(null)
  const geriAlSepet = ref(null)
  const suruklenenIdx = ref(null)
  /** Su an uzerine birakilacak satir (yazim: `suruklenen`). */
  const suruklenenUzerinde = ref(null)
  let geriAlSatirZamanlayici = null
  let geriAlZamanlayici = null

  // --- toplamlar ---
  const toplam = computed(() => sepet.value.reduce((t2, i) => t2 + i.miktar * i.fiyat, 0))

  const toplamFt3 = computed(() =>
    sepet.value.reduce((t2, i) => {
      const hacim = i.birimHacim || 1
      return t2 + i.miktar * hacim
    }, 0)
  )

  const toplamAgirlik = computed(() =>
    sepet.value.reduce((t2, i) => t2 + (Number(i.agirlik) || 0) * (Number(i.miktar) || 0), 0)
  )

  const agirlikVarMi = computed(() => toplamAgirlik.value > 0)

  // --- yardimcilar ---
  const satiriVurgula = (id) => {
    vurguluId.value = id
    setTimeout(() => {
      if (vurguluId.value === id) vurguluId.value = null
    }, 700)
  }

  const sepetteAdet = (id) => sepetteToplamAdet(sepet.value, id)
  const sepetSatiriBul = (id, fiyatTipi) => posSatiriBul(sepet.value, id, fiyatTipi)
  const urunFiyatTipleri = (id) => kullanilanFiyatTipleri(sepet.value, id)

  /** Aktif satiri hizalar; tum satir eylemleri buradan gecer. */
  const satirAktifYap =
    (fn) =>
    (idx, ...kalan) => {
      aktifSatir.value = idx
      return fn(idx, ...kalan)
    }

  /** Stoktan fazla adet istendiginde kullaniciyi bilgilendirir. */
  const stokAsimiUyar = (urun, dogrulama) => {
    bildir.uyari(
      t('hizliSatis.adetStokAsimi', {
        ad: urun.ad,
        n: dogrulama.enFazla,
        birim: urun.birim || t('hizliSatis.adetBirimi')
      })
    )
  }

  // --- sepete ekleme ---

  // Sepet satiri benzersizligi: (stokId + fiyatTipi). Ayni urunun farkli fiyat
  // tipleri AYRI satirdir; karttan ekleme daima varsayilan (ilk) fiyat tipiyle
  // yapilir ve o satira birikir.
  const sepeteEkle = async (u, adet = 1) => {
    const birim = u.birim || 'adet'
    // Sepete giren her satir depodaki MIKTARI biriktirir. Backend
    // `FaturaService` satista `Stok.miktar` ile karsilastirip yetersizse
    // reddeder; ayni sayiyi tutmak hatayi kasada gormeyi saglar.
    const stokMiktari = Number(u.miktar) || 0

    if (stokMiktari <= 0) {
      // Barkod ile okutmada once `stokYokMu` kontrolu yoktu; stoksuz urun
      // sepete girebiliyordu ve hata ancak satis aninda cikiyordu.
      bildir.uyari(t('hizliSatis.stokYokUyari', { ad: u.ad }))
      return
    }

    const hedefFiyatTipi = u.fiyatlar?.[0]?.ad || t('hizliSatis.fiyatPerakende')
    const varOlan = sepetSatiriBul(u.id, hedefFiyatTipi)
    if (varOlan) {
      // Tavan TOPLAM uzerinden kontrol edilir (sepette 2, stok 3 -> 5 istenirse 3).
      varOlan.stokMiktari = stokMiktari
      const dogrulama = miktarDogrula({
        istenen: varOlan.miktar + (Number(adet) || 1),
        stokMiktari,
        birim
      })
      if (dogrulama.asim) stokAsimiUyar(u, dogrulama)
      varOlan.miktar = dogrulama.miktar
      satiriVurgula(u.id)
      return
    }

    const ilkDogrulama = miktarDogrula({ istenen: adet, stokMiktari, birim })
    if (ilkDogrulama.asim) stokAsimiUyar(u, ilkDogrulama)
    const miktar = ilkDogrulama.miktar
    const stdFiyat = Number(u.satisFiyati || u.fiyat || 0)
    const cokluFiyatVar = !!(u.fiyatlar && u.fiyatlar.length > 0)
    const temelFiyatlar = cokluFiyatVar
      ? u.fiyatlar.map((f) => ({ ad: f.ad, fiyat: f.fiyat }))
      : [
          { ad: t('hizliSatis.fiyatPerakende'), fiyat: stdFiyat },
          { ad: t('hizliSatis.fiyatToptan'), fiyat: Math.round(stdFiyat * 0.9 * 100) / 100 },
          { ad: t('hizliSatis.fiyatOzel'), fiyat: Math.round(stdFiyat * 0.8 * 100) / 100 }
        ]

    // IYIMSER EKLEME: satir aninda sepete girer (gecikme/cift tiklama sorunu
    // yok); fiyat listesi ve cari gecmisi arka planda zenginlestirilir.
    const yeniItem = {
      id: u.id,
      ad: u.ad,
      stokKodu: u.stokKodu,
      barkod: u.barkod,
      miktar,
      fiyat: temelFiyatlar[0]?.fiyat ?? stdFiyat,
      fiyatlar: temelFiyatlar,
      fiyatTipi: temelFiyatlar[0]?.ad ?? t('hizliSatis.fiyatPerakende'),
      birim: u.birim || 'adet',
      stokMiktari,
      // Stogun KDV orani sepete tasinir; tanimli degilse 0 kalir.
      kdvOrani: u.kdvOrani != null ? Number(u.kdvOrani) : 0,
      birimHacim: u.birimHacim || 1,
      agirlik: Number(u.agirlik) || 0,
      sonAldigiFiyat: null,
      sonAldigiTarih: null,
      sonAldigiBilgisiYukleniyor: false
    }
    sepet.value.push(yeniItem)
    satiriVurgula(u.id)

    try {
      let fiyatlar = temelFiyatlar
      if (!cokluFiyatVar) {
        const tckilen = await urunFiyatlariniYukleTek(u)
        if (tckilen && tckilen.length > 0) fiyatlar = tckilen
      }
      // Kullanici bu arada satiri sildiyse dokunma. Yalniz-id aramasi, ayni
      // urun FARKLI fiyat tipleriyle iki satir halindeyken YANLIS satiri
      // zenginlestirirdi; eklenen satir (id + fiyatTipi) ile bulunur.
      const guncel = sepetSatiriBul(u.id, hedefFiyatTipi)
      if (!guncel) return
      guncel.fiyatlar = fiyatlar
      if (!fiyatlar.some((f) => f.ad === guncel.fiyatTipi)) {
        guncel.fiyat = fiyatlar[0]?.fiyat ?? stdFiyat
        guncel.fiyatTipi = fiyatlar[0]?.ad ?? t('hizliSatis.fiyatPerakende')
      }
      // Secili musteri varsa urunu en son hangi fiyata aldigini sor.
      if (seciliMusteri?.value?.id) {
        guncel.sonAldigiBilgisiYukleniyor = true
        try {
          const r = await cariUrunFiyatGecmisi(seciliMusteri.value.id, u.id)
          const data = r.data
          if (data && data.sonFiyat != null) {
            guncel.sonAldigiFiyat = data.sonFiyat
            const enSon = (data.gecmis || [])[0]
            guncel.sonAldigiTarih = enSon?.tarih || null
            guncel.fiyat = data.sonFiyat
            if (!guncel.fiyatlar.some((f) => f.ad === t('hizliSatis.fiyatSonAldigi'))) {
              guncel.fiyatlar.unshift({ ad: t('hizliSatis.fiyatSonAldigi'), fiyat: data.sonFiyat })
            }
            guncel.fiyatTipi = guncel.fiyatlar[0]?.ad ?? t('hizliSatis.fiyatPerakende')
          }
        } catch {
          /* cari fiyat gecmisi alinamadi */
        } finally {
          guncel.sonAldigiBilgisiYukleniyor = false
        }
      }
    } catch {
      /* zenginlestirme opsiyonel; temel fiyatla devam */
    }
  }

  const fiyatTipiDegisti = (item) => {
    const secili = item.fiyatlar?.find((f) => f.ad === item.fiyatTipi)
    if (secili) item.fiyat = secili.fiyat
  }

  // Musteri secilince sepetteki tum urunlere cari bazli fiyati uygular.
  const sepeteCariFiyatUygula = async () => {
    const cariId = seciliMusteri?.value?.id
    if (!cariId || !sepet.value.length) return
    await Promise.all(
      sepet.value.map(async (item) => {
        try {
          const r = await cariUrunFiyatGecmisi(cariId, item.id)
          const data = r.data
          if (data && data.sonFiyat != null) {
            item.sonAldigiFiyat = data.sonFiyat
            const enSon = (data.gecmis || [])[0]
            item.sonAldigiTarih = enSon?.tarih || null
            item.fiyat = data.sonFiyat
            if (!item.fiyatlar.some((f) => f.ad === t('hizliSatis.fiyatSonAldigi'))) {
              item.fiyatlar.unshift({ ad: t('hizliSatis.fiyatSonAldigi'), fiyat: data.sonFiyat })
            }
            item.fiyatTipi = t('hizliSatis.fiyatSonAldigi')
          }
        } catch {
          /* cari fiyat gecmisi alinamadi */
        }
      })
    )
  }

  // --- adet (TEK GIRIS NOKTASI) ---

  /**
   * Buton, girdi ve klavye (Alt+ok) HEPSI buraya gelir; boylece stok tavani,
   * geri al penceresi ve satir vurgusu her yolda ayni calisir.
   *
   * @param {number} idx Sepet indeksi
   * @param {number|string} istenen Yeni miktar (girdiden metin olabilir)
   * @param {'tam'|'adim'} [mod] `adim`: yalnizca bir adim artir/azalt
   */
  const miktarDegistir = (idx, istenen, mod = 'tam') => {
    const satir = sepet.value[idx]
    if (!satir) return { uygulandi: false, miktar: 0, sorun: null }

    const birim = satir.birim || 'adet'
    const stokMiktari = satir.stokMiktari ?? null

    const sonuc =
      mod === 'adim'
        ? adimla({ mevcut: satir.miktar, yon: istenen > 0 ? 1 : -1, birim, stokMiktari })
        : miktarDogrula({ istenen, stokMiktari, birim })

    if (!sonuc.degisti) return { uygulandi: false, miktar: satir.miktar, sorun: sonuc.sorun }

    // Adet degisikligi de (silme kadar) geri alinabilir olmali.
    geriAlSatirKaydet(idx)
    satir.miktar = sonuc.miktar
    satiriVurgula(satir.id)

    if (sonuc.asim) {
      bildir.uyari(t('hizliSatis.adetStokAsimiSepet', { n: sonuc.enFazla, birim }))
    } else if (sonuc.sorun === ADET_SORUN.GECERSIZ) {
      bildir.uyari(t('hizliSatis.adetGecersiz'))
    } else if (sonuc.sorun === ADET_SORUN.ADIM_UYUSMUYOR) {
      bildir.uyari(t('hizliSatis.adetAdimUyusmadi', { adim: adimBirimIcin(birim), sonuc: sonuc.miktar }))
    }

    return { uygulandi: true, miktar: sonuc.miktar, sorun: sonuc.sorun }
  }

  const miktarAzalt = (idx) => {
    // Once miktar 1'de dusunce satir SILINIYORDU — kasada farkinda olmadan
    // veri kaybi. Artik yalniz en kucuk degere iner.
    return miktarDegistir(idx, -1, 'adim')
  }

  /** Satir eylem menusunden "adedi sifirla" (en kucuk adede indirir). */
  const adediSifirla = (idx) => {
    const satir = sepet.value[idx]
    if (!satir) return undefined
    const adim = adimBirimIcin(satir.birim)
    return miktarDegistir(idx, satir.miktar <= adim ? satir.miktar : adim)
  }

  // --- silme / cogaltma ---

  // Satir silme: aktifSatir bir INDEKS; splice sonrasi geride kalir ve
  // Delete/Enter/Alt+ok kisayollari yanlis satira yonlenirdi.
  const sepetSil = (idx) => {
    if (idx < 0 || idx >= sepet.value.length) return
    geriAlSatirKaydet(idx)
    sepet.value.splice(idx, 1)
    if (sepet.value.length === 0) aktifSatir.value = -1
    else if (aktifSatir.value > idx) aktifSatir.value -= 1
    else if (aktifSatir.value === idx) aktifSatir.value = Math.min(idx, sepet.value.length - 1)
  }

  // "Cogalt" ayni fiyat tipini tekrarlamaz. Amaci ayni urunu FARKLI bir fiyat
  // tipiyle ayri satirda satmaktir. Once korukorune kopyaliyordu: ayni urun +
  // ayni fiyat tipi iki satir olusuyor, karttan ekleme yalnizca ilkini
  // buldugu icin "ekledim ama artmadi" durumu doguyordu.
  const satiriCogalt = (idx) => {
    const kaynak = sepet.value[idx]
    if (!kaynak) return false
    const alternatif = sonrakiFiyatTipi(kaynak.fiyatlar, urunFiyatTipleri(kaynak.id))
    if (!alternatif) {
      bildir.uyari(t('hizliSatis.cogaltFiyatTipiYok', { ad: kaynak.ad }))
      return false
    }
    sepet.value.splice(idx + 1, 0, { ...kaynak, fiyatTipi: alternatif.ad, fiyat: alternatif.fiyat })
    geriAlSatirCogaltKaydet(idx + 1)
    return true
  }

  const aktifSatiriCogalt = () => {
    const hedef = aktifSatir.value
    if (satiriCogalt(hedef)) aktifSatir.value = hedef + 1
  }

  // --- geri alma: satir ---

  const geriAlSatirKaydet = (idx) => {
    const kalem = sepet.value[idx]
    if (!kalem) return
    geriAlSatir.value = { mod: 'sil', kalem: { ...kalem }, idx }
    geriAlSatirZamanlayiciTazele()
  }

  const geriAlSatirCogaltKaydet = (yeniIdx) => {
    if (yeniIdx < 0 || yeniIdx >= sepet.value.length) return
    geriAlSatir.value = { mod: 'cogalt', idx: yeniIdx }
    geriAlSatirZamanlayiciTazele()
  }

  const geriAlSatirZamanlayiciTazele = () => {
    clearTimeout(geriAlSatirZamanlayici)
    geriAlSatirZamanlayici = setTimeout(() => {
      geriAlSatir.value = null
    }, GERI_AL_PENCERE_MS)
  }

  const geriAlSatirYap = () => {
    if (!geriAlSatir.value) return
    const { kalem, idx, mod } = geriAlSatir.value
    if (mod === 'cogalt') {
      // Kopyayi kaldir. Konum kaymasina uygun sekilde bulunmali: araya yeni
      // satir eklenmis olabilir.
      const hedef = Math.min(Math.max(idx, 0), sepet.value.length)
      sepet.value.splice(hedef, 1)
      if (sepet.value.length === 0) aktifSatir.value = -1
      else aktifSatir.value = Math.min(hedef, sepet.value.length - 1)
    } else {
      const hedef = Math.min(Math.max(idx, 0), sepet.value.length)
      sepet.value.splice(hedef, 0, kalem)
      aktifSatir.value = hedef
    }
    geriAlSatir.value = null
    clearTimeout(geriAlSatirZamanlayici)
  }

  // --- geri alma: sepetin tamami ---

  const sepetiGeriAlinabilirTemizle = () => {
    if (sepet.value.length) {
      geriAlSepet.value = sepet.value.map((i) => ({ ...i }))
      clearTimeout(geriAlZamanlayici)
      geriAlZamanlayici = setTimeout(() => {
        geriAlSepet.value = null
      }, GERI_AL_PENCERE_MS)
    }
    sepet.value = []
    aktifSatir.value = -1
    geriAlSatir.value = null
    clearTimeout(geriAlSatirZamanlayici)
  }

  const sepetGeriAl = () => {
    if (!geriAlSepet.value) return
    sepet.value = geriAlSepet.value.map((i) => ({ ...i }))
    geriAlSepet.value = null
    clearTimeout(geriAlZamanlayici)
  }

  /**
   * Geri al: once SATIR penceresi (daha yeni, daha kisa omurlu), sonra sepet.
   * @returns {boolean} Geri alinacak bir sey var miydi
   */
  const geriAlYap = () => {
    if (geriAlSatir.value) {
      geriAlSatirYap()
      return true
    }
    if (geriAlSepet.value) {
      sepetGeriAl()
      return true
    }
    return false
  }

  /** Sepeti geri alinamaz sekilde temizler (satis sonrasi). */
  const sepetiSifirla = () => {
    sepet.value = []
    geriAlSepet.value = null
    geriAlSatir.value = null
    clearTimeout(geriAlSatirZamanlayici)
    clearTimeout(geriAlZamanlayici)
    aktifSatir.value = -1
  }

  // --- surukle-birak siralama ---

  const suruklemeBasla = (idx) => {
    suruklenenIdx.value = idx
  }
  const suruklemeUzerine = (idx) => {
    suruklenenUzerinde.value = idx
  }
  const suruklemeBitir = () => {
    suruklenenIdx.value = null
    suruklenenUzerinde.value = null
  }
  const suruklemeBirak = (hedefIdx) => {
    const kaynak = suruklenenIdx.value
    suruklenenUzerinde.value = null
    if (kaynak === null || kaynak === hedefIdx) {
      suruklenenIdx.value = null
      return
    }
    const [tasinan] = sepet.value.splice(kaynak, 1)
    sepet.value.splice(hedefIdx, 0, tasinan)
    aktifSatir.value = hedefIdx
    suruklenenIdx.value = null
  }

  return {
    // durum
    sepet,
    aktifSatir,
    vurguluId,
    geriAlSatir,
    geriAlSepet,
    suruklenenIdx,
    suruklenenUzerinde,
    // toplamlar
    toplam,
    toplamFt3,
    toplamAgirlik,
    agirlikVarMi,
    // yardimcilar
    satiriVurgula,
    sepetteAdet,
    sepetSatiriBul,
    urunFiyatTipleri,
    satirAktifYap,
    stokAsimiUyar,
    // islemler
    sepeteEkle,
    fiyatTipiDegisti,
    sepeteCariFiyatUygula,
    miktarDegistir,
    miktarAzalt,
    adediSifirla,
    sepetSil,
    satiriCogalt,
    aktifSatiriCogalt,
    // geri alma
    geriAlSatirKaydet,
    geriAlSatirCogaltKaydet,
    geriAlSatirYap,
    sepetiGeriAlinabilirTemizle,
    sepetGeriAl,
    geriAlYap,
    sepetiSifirla,
    // siralama
    suruklemeBasla,
    suruklemeUzerine,
    suruklemeBirak,
    suruklemeBitir
  }
}
