import { nextTick, onMounted, onUnmounted } from 'vue'

/**
 * Hizli Satis klavye kisayollari.
 *
 * SORUN: `handlePosKeys` 146 satirdi, 25+ kisayol iceriyordu ve hepsi tek bir
 * ic ice `if` zincirindeydi. Bu birkac seyi kotu yapiyordu:
 *   - hangi kisayolun nerede tanimlandigi bulunamiyordu,
 *   - bir kisayol eklemek/cikarmak zincirin ortasini kaydirmak demekti,
 *   - "F tuslari once, izgara modu sonra" gibi ONCEKI SIRALAMA KURALI kodda
 *     gorunmezdi; bir `if` yukarida tasinsa davranans sessizce degisirdi.
 *
 * COZUM: Kisayollar bagimsiz "gruplar"a bolunur. Her grup tek bir soruyu
 * cevaplar ("bu tus bu kapsamda mi?"), `true` donerse olay islenmistir.
 * Sıralama TEK bir dizide ve acik sekilde yazilidir; `?` gibi belirsiz
 * durumlar yorumla desteklenir.
 *
 * KAPSAM KURALLARI:
 *   - Fonksiyon tuslari (F1..F11) HER ZAMAN once bakilir; izgara odaktayken de
 *     gecerlidir. Aksi halde izgara modunda Escape/F tuslari calismazdi.
 *   - Izgara modu acikken oklar/Enter/Backspace/rakam SEPETI degil IZGARAYI
 *     yonetir ve diger tum kisayollar devre disi kalir (oda modu).
 *   - Harf kisayollari (n/k/h/t/p/g/d) YAZARKEN tetiklenmez.
 *   - `?` BURAYA YAZILMAZ. `?` tusu `useKisayollar` tarafindan, capture
 *     fazinda ve girdi alaninda degilken yakalanir; orada `calistir('ipucu')`
 *     cagrilir ve POS bu eylemi `ipucuToggle` ile kaydeder. Iki handler da
 *     capture fazinda `window` uzerinde oldugu icin, `?` burada da ele
 *     alinsaydi ipucu iki kez acilip kapanirdi (net sonuc: hicbir sey olmazdu).
 *     Bu yuzden `harfKisayollari` icinde `?` YOKTUR ve olmamalidir.
 *
 * @typedef {object} PosKisayolBaglami Tüm bagimliliklar (ref'ler + eylemler)
 */

/** Odak bir metin alaninda mi? (harf/ok kisayollari burada devre disi)
 *  `Boolean(...)` ile sarilir: `el.isContentEditable` tanimsiz oldugunda
 *  zincir `undefined` uretip geri donuyordu (falsy oldugu icin calisiyordu,
 *  ama "bu bir boolean" sozlesmesi tutmuyordu). */
const girdideMi = (e) => {
  const el = e.target
  return Boolean(
    el && (el.tagName === 'INPUT' || el.tagName === 'SELECT' || el.tagName === 'TEXTAREA' || el.isContentEditable)
  )
}

/** Bilesen ref'ini odakla ($el uzerinden, PrimeVue uyumlu). */
const odakla = (r) => {
  const el = r?.value
  el?.$el?.focus?.() || el?.focus?.()
}

// ---------------------------------------------------------------------------
// GRUP 1 — Fonksiyon tuslari (F1..F11)
// ---------------------------------------------------------------------------

/**
 * @returns {boolean} Olay bu grupta isleniyorsa true
 */
export function fonksiyonTuslari(e, c) {
  switch (e.key) {
    case 'F1':
      e.preventDefault(); odakla(c.barkodInputRef); return true

    case 'F2':
      // Sepeti geri alinabilir temizle (8 sn geri alma penceresi acilir).
      e.preventDefault()
      c.sepetiGeriAlinabilirTemizle()
      c.bildir('info', c.t('hizliSatis.sepetTemizlendi'))
      return true

    case 'F3':
      e.preventDefault()
      // F3 iki yonlu olmamali: izgaradaysak arama kutusuna degil izgaraya don.
      if (c.urunIzgaraOdak.value) c.urunIzgarayiOdakla()
      else c.odaklaUrunArama()
      return true

    case 'F4':
      e.preventDefault()
      c.musteriModu.value = 'musteri'
      nextTick(() => odakla(c.musteriPaneliRef.value?.musteriAutoRef))
      return true

    case 'F5':
      e.preventDefault(); c.yeniMusteriDialog.value = true; return true

    case 'F6':
      e.preventDefault(); c.scannerAcik.value = true; return true

    case 'F7':
      e.preventDefault(); c.bugunkuSatislariAc(); return true

    case 'F8':
      // Onceden F9'a bagliymis gibi etiketleniyordu (`hizliSatis.yazdirF9`)
      // ama F9 satisi tamamlar; yazdirma yalnizca Ctrl+P ile mumkundu.
      e.preventDefault(); c.fisOnizlemeToggle(); return true

    case 'F11':
      e.preventDefault(); c.termalYazdir(); return true

    // F9 = peşin tam, F10 = kısmi. YALNIZCA odeme durumunu ayarlar.
    //
    // ONEMLI (REDTEAM/Faz5): Once bu tuslar durumu ayarlayip ARDINDAN
    // `satisiTamamla()` cagiriyordu. Kasiyer odeme tipini secerken yanlislikla
    // F9/F10'a basmasi satisi ANINDA kaydediyor, fis/acilis penceresi aciliyor
    // ve geri donusu olmuyordu. Satisi bitirmek icin artik ACIK bir eylem
    // gerekir: `Ctrl/Cmd+S`, "Satisi Tamamla" butonu (veya Enter + onay).
    case 'F9':
    case 'F10':
      e.preventDefault()
      c.odemeDurumu.value = e.key === 'F9' ? 'tam' : 'yarim'
      return true

    default:
      return false
  }
}

// ---------------------------------------------------------------------------
// GRUP 2 — Metin alanindayken ↓ ile urun ızgarasina gir
// ---------------------------------------------------------------------------

/**
 * Once izgaraya gecmenin HICBIR yolu yoktu; kartlar yalniz fareyle secilebiliyordu.
 * Metin alaninda oklar zaten sepette gezmez (`girdideMi` korumasi), yani bu yon
 * bos kalirdi.
 *
 * @returns {boolean}
 */
export function girdidenIzgaraya(e, c) {
  if (e.key !== 'ArrowDown') return false
  if (!girdideMi(e)) return false
  if (!c.gorunenUrunler.value.length) return false
  e.preventDefault()
  c.urunIzgarayaGir()
  return true
}

// ---------------------------------------------------------------------------
// GRUP 3 — Alt + ok ile aktif satirin miktari
// ---------------------------------------------------------------------------

/** @returns {boolean} */
export function altOklar(e, c) {
  if (!e.altKey) return false
  if (e.key !== 'ArrowUp' && e.key !== 'ArrowDown') return false
  const satir = c.aktifSatir.value >= 0 ? c.sepet.value[c.aktifSatir.value] : null
  if (!satir) return false
  e.preventDefault()
  if (e.key === 'ArrowUp') satir.miktar++
  else c.miktarAzalt(c.aktifSatir.value)
  return true
}

// ---------------------------------------------------------------------------
// GRUP 4 — Izgara klavye modu (acikken bu bir "oda" modudur)
// ---------------------------------------------------------------------------

/** Izgarada gosterilen urun sayisi 0 ise odak modunda anlamli hareket yok. */
export function izgaraModu(e, c) {
  if (!c.urunIzgaraOdak.value) return false

  switch (e.key) {
    case 'Escape':
      e.preventDefault(); c.urunIzgaradanCik(); return true
    case 'ArrowUp':
      e.preventDefault(); c.urunIzgaraHareket(0, -1); return true
    case 'ArrowDown':
      e.preventDefault(); c.urunIzgaraHareket(0, 1); return true
    case 'ArrowLeft':
      e.preventDefault(); c.urunIzgaraHareket(-1, 0); return true
    case 'ArrowRight':
      e.preventDefault(); c.urunIzgaraHareket(1, 0); return true
    case 'Backspace':
      // Miktarli ekleme icin yazilan rakamin son hanesini siler.
      e.preventDefault()
      c.urunIzgaraRakam.value = c.urunIzgaraRakam.value.slice(0, -1)
      return true
    default:
      break
  }

  if (e.key === 'Enter' && e.shiftKey) {
    e.preventDefault()
    c.adetPopoverAc(c.gorunenUrunler.value[c.urunIzgaraIndeks.value], c.urunIzgaraIndeks.value)
    return true
  }

  if (e.key === 'Enter') {
    e.preventDefault(); c.urunIzgaraSec(); return true
  }

  // Rakam yazildi: miktarli ekleme hazirlanir. Maksimum 4 hane (9999).
  if (/^[0-9]$/.test(e.key)) {
    e.preventDefault()
    if (c.urunIzgaraRakam.value.length < 4) c.urunIzgaraRakam.value += e.key
    return true
  }

  // Izgara modunda basilan HER tus yutulur: yazarken harf kisayollari
  // (n/k/h/t/p/g/d) tetiklenmemeli.
  return true
}

// ---------------------------------------------------------------------------
// GRUP 5 — Sepette satir arasi gezinme ve miktar alanina odak
// ---------------------------------------------------------------------------

/** @returns {boolean} */
export function sepetGezinme(e, c) {
  const yonluOk = e.key === 'ArrowUp' || e.key === 'ArrowDown'
  if (!girdideMi(e) && c.sepet.value.length && yonluOk) {
    e.preventDefault()
    const yon = e.key === 'ArrowDown' ? 1 : -1
    let idx = c.aktifSatir.value
    if (idx < 0) idx = yon > 0 ? 0 : c.sepet.value.length - 1
    else idx = Math.min(c.sepet.value.length - 1, Math.max(0, idx + yon))
    c.aktifSatir.value = idx
    return true
  }

  // Aktif satirin miktar alanina odaklan (Enter ile hizli adet duzenleme).
  if (!girdideMi(e) && e.key === 'Enter') {
    if (c.aktifSatir.value >= 0 && c.sepet.value[c.aktifSatir.value]) {
      e.preventDefault()
      c.odaklaAktifAdet()
    }
    return true
  }

  return false
}

// ---------------------------------------------------------------------------
// GRUP 6 — Delete ve harf kisayollari
// ---------------------------------------------------------------------------

/** Harf kisayollari: odeme yontemi, fis modu, geri al, satir cogalt. */
export function harfKisayollari(e, c) {
  switch (e.key.toLowerCase()) {
    case 'n':
      e.preventDefault(); c.odemeYontemi.value = 'NAKIT'; return true
    case 'k':
      e.preventDefault(); c.odemeYontemi.value = 'KART'; return true
    case 'h':
      e.preventDefault(); c.odemeYontemi.value = 'HAVALE'; return true
    case 't':
      // Taksit yontemine ONCEDEN kisiyol yoktu; fareye uzanmak gerekiyordu.
      e.preventDefault(); c.odemeYontemi.value = 'TAKSIT'; return true
    case 'p':
      // Fis modu (fiyatli/fiyatsiz) — yazdirma kisayolu degil, tek tusla degistirir.
      e.preventDefault(); c.fisDegiskeniniDegistir(); return true
    case 'g':
      e.preventDefault(); c.geriAlYap(); return true
    case 'd':
      // `D` = Duplicate: aktif satiri cogalt (ayni urunden ikinci satir acmak
      // icin sepetle oynamak gerekiyordu).
      if (c.aktifSatir.value >= 0 && c.sepet.value[c.aktifSatir.value]) {
        e.preventDefault(); c.aktifSatiriCogalt()
      }
      return true
    default:
      return false
  }
}

/** Aktif satiri sil (Delete). */
export function satirSil(e, c) {
  if (e.key !== 'Delete') return false
  if (c.aktifSatir.value < 0) return false
  if (!c.sepet.value[c.aktifSatir.value]) return false
  e.preventDefault()
  c.sepetSil(c.aktifSatir.value)
  return true
}

/**
 * YAZARKEN KORUMA: odak bir metin alanindaysa asagidaki tum kisayollar
 * (Delete ve harfler) devre disi kalir.
 *
 * Bu ayri bir "grup" gibi yazildi cunku sira KURALDIR ve kodda gorunmez
 * olmamalidir: harf kisa yollari onceden bu korumanin SONRASINDA geliyordu.
 * Korumayi kaldirmak, urun adi yazarken `n/k/h/t/p/g/d` tuslarinin odeme
 * yontemini degistirmesine yol acar.
 */
export function yazarkenKoru(e) {
  return girdideMi(e)
}

/**
 * Ana dispatcher.
 *
 * SIRALAMA:
 *   1. Fonksiyon tuslari (F1..F11) her zaman gecerli; izgara modunda bile.
 *      `girdidenIzgaraya` onlarinla celisemedigi icin en basta.
 *   2. Izgara modu acikken oklar/Enter/Backspace/rakam SEPETI degil IZGARAYI
 *      yonetir ve o an basilan HER tusu yutar ("oda" modu).
 *   3. Sepette satir gezinme.
 *   4. YAZARKEN KORUMA — sonraki tum kisayollar bundan sonra devre disi.
 *   5. Delete ve harf kisayollari.
 *
 * @param {KeyboardEvent} e
 * @param {PosKisayolBaglami} c
 * @returns {boolean} Olay isleniyorsa true
 */
export function posKisayolCoz(e, c) {
  const gruplar = [
    girdidenIzgaraya,
    fonksiyonTuslari,
    altOklar,
    izgaraModu,
    sepetGezinme,
    yazarkenKoru,
    satirSil,
    harfKisayollari
  ]
  for (const grup of gruplar) {
    if (grup(e, c)) return true
  }
  return false
}

/**
 * POS kisayollarini window'a baglar.
 *
 * @param {PosKisayolBaglami} c Baglam (ref'ler + eylemler)
 * @param {object} [secenek]
 * @param {Function} [secenek.t] ceviri
 * @param {Function} [secenek.bildir] toast bildirimi
 */
export function usePosKisayollar(c, { t, bildir } = {}) {
  const baglam = { ...c, t, bildir }

  const handlePosKeys = (e) => {
    posKisayolCoz(e, baglam)
  }

  // Ctrl/Cmd+Z geri al. `handlePosKeys` capture fazinda calistigi icin tarayicinin
  // metin alanlarindaki yerlesik geri alma davranisini ezmemek icin ayri bir
  // dinleyicide yakalanir ve yalnizca odak metin alaninda DEGILKEN calisir.
  const handlePosUndo = (e) => {
    if (!(e.ctrlKey || e.metaKey) || e.key.toLowerCase() !== 'z' || e.shiftKey) return
    if (girdideMi(e)) return
    e.preventDefault()
    baglam.geriAlYap()
  }

  onMounted(() => {
    window.addEventListener('keydown', handlePosKeys, true)
    window.addEventListener('keydown', handlePosUndo, true)
  })

  onUnmounted(() => {
    window.removeEventListener('keydown', handlePosKeys, true)
    window.removeEventListener('keydown', handlePosUndo, true)
  })

  return { handlePosKeys, handlePosUndo, girdideMi }
}