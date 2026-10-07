import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

/**
 * REDTEAM/Faz4 — POS klavyeye gore yeniden tasarim: regresyon denetimleri.
 *
 * DENETLENEN BULGULAR (hicbir derleyici/ESLint uyarisi uretmıyordu):
 *
 *  1) Izgara klavyeye GIRILEMIYORDU. `↑/↓` oklari sepette geziyordu ve urun
 *     kartlari hicbir zaman odaklanmiyordu. Kasada urun kartini secmek icin
 *     fare zorunluydu.
 *  2) HIZLI ADET YOKTU. `PosUrunKarti` yalnizca `emit('sec')` ile 1 adet
 *     ekliyordu; 20 adet almak icin 20 kez tiklamak gerekiyordu.
 *  3) GERI AL klavyeye bagli DEGILDI (G / Ctrl+Z eklendi).
 *  4) `?` POS'ta global KisayolRehberi'nin yerine yerel ipucunu acmiyordu.
 *  5) `g h` POS'tan cikis kisayolu OLMUYORDU (`n/k/h` odeme tuslari
 *     `useKisayollar`'in `g`-gezinme harflerini yutuyordu).
 *  6) "Yazdir (F9)" etiketi YANLISDI: F9 satisi tamamliyor, yazdirma yalnizca
 *     Ctrl+P ile mumkundu; satis sonrasi fiy yeniden basilamiyordu.
 *  7) Satiri cogaltma YALNIZCA klavyeden mumkundu ve hicbir menude gorunmuyordu.
 *
 * ---------------------------------------------------------------------------
 * SINIFLANDIRMA (POS revizyonu, Asama 0)
 *
 * Bu dosya iki tur denetim karistiriyordu. Revizyon plani geregi ayrildi:
 *
 *  A) ARTUK DAVRANIS TESTIYLE KAPSALI OLAN KURALLAR SILINDI.
 *     Bunlar bilesenlerin props/emits/erisilebilirlik sozlesmesidir ve
 *     mount testiyle dogrulanir; kaynak metnine bakan bir denetim ayni
 *     kurali daha zayif bir yolla tekrar ediyordu:
 *       - kart roving tabindex / aria-selected / sag tik adet penceresi
 *         -> src/components/__tests__/PosUrunKarti.spec.js
 *       - satir eylem menusu (duzenle/cogalt/adedi sifirla) baglantisi
 *         -> src/components/__tests__/PosSepetPaneli.spec.js
 *
 *  B) VIEW-IC KLAVYE/SEPET KURALLARI KORUNDU, ancak SABIT METIN yerine
 *     BLOK KAPSAMLI denetim haline getirildi. Gerekce: bu kurallar
 *     `HizliSatis.vue` icinde duruyor ve Faz1'de `usePosKisayollar` /
 *     `usePosSepet` composable'larina tasiyacak. Tasi, TASIYANA kadar
 *     kural kaybolmasin diya burada tutulur; her biri tasima hedefini
 *     yorumda belirtir. Blok kapsamli yazildigi icin yorum eklemek,
 *     siralamayi degistirmek veya fonksiyonu yeniden adlandirmak
 *     (kosul metni degistirmedikce) testi kirmaz.
 *
 *  C) KALAN KURALLAR BILESEN/ALTYAPI DUZEYINDE ve yerinde kalir:
 *     `useKisayollar` sayfa basina kayit/nerede-calistir sözlesmesi ve
 *     teleport/defineEmpts gibi capraz dosya mimari kurallari.
 *
 * YENI DOSYA EKLEMEK: bu bir "kural" dosyasi; ekran davranisi degil, kodun
 * belli ozelliklerini korur. Yeniden yazim yuzunden kirilirsa once
 * kuralin degerini sor: degerini koruyorsan testi guncelle.
 */

const KOK = join(process.cwd(), 'src')
const posKod = readFileSync(join(KOK, 'views/HizliSatis.vue'), 'utf8')
const kartKod = readFileSync(join(KOK, 'components/PosUrunKarti.vue'), 'utf8')
const kisayolKod = readFileSync(join(KOK, 'composables/useKisayollar.js'), 'utf8')
// Sepet cekirdedi (sepeteEkle/sepetSil/geriAlSatir*) `usePosSepet`'e tasindi.
const sepetKod = readFileSync(join(KOK, 'composables/usePosSepet.js'), 'utf8')
// ASAMA 1: POS klavye kisayollari `HizliSatis.vue` icindeki TEK bir
// `handlePosKeys` fonksiyonundan `composables/usePosKisayollar.js` icindeki
// gruplara tasindi. Artik kisayol kurallari IKI dosyaya dagiliyor:
//   - `usePosKisayollar.js` : tus -> eylem eslemesi, oncelik sirasi, yazarken koruma
//   - `HizliSatis.vue`      : o eylemlerin KARSILIGI olan view fonksiyonlari
//                          (sepeteEkle, geriAlYap, urunIzgaraHareket ...)
// Bu dosya artik yalniz VIEW tarafindaki sözlesmeyi denetler; tus->eylem
// eslemesinin davranis testleri `src/composables/__tests__/usePosKisayollar.spec.js`
// dosyasindadir (orada mount'suz, saf fonksiyon uzerinden dogrulanir).
const posKisayolKod = readFileSync(join(KOK, 'composables/usePosKisayollar.js'), 'utf8')
// ASAMA 1: HizliSatis'in <style scoped> blogu `assets/pos-hizli-satis.css`
// dosyasina tasindi (SFC 4000+ satirdan ~2800'e indi). Dosya <style scoped>
// UZERINDEN @import edildigi icai kurallar yine SADECE bu view'in sablonuna
// uygulanir — dosyayi global CSS sanmak yanlis olur.
// `sepetPanelKod` artik okunmuyor: panelin satir eylem menusu baglantisi artik
// mount testiyle kapsali (PosSepetPaneli.spec.js). Cift tanim kurali ise tum
// .vue dosyalarini tarayan capraz-dosya denetimine donusturuldu.

/** Yorum satirlarini temizler; denetim KOD okumalidir, metni degil.
 *  NOT: dosyalar CRLF kaydedilmis olabilir; JS regexte `.` \r'yi TUTMAZ. */
const yorumsuz = (s) =>
  s
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .split(/\r?\n/)
    .map((satir) => satir.replace(/(^|\s)\/\/.*$/, ''))
    .join('\n')

/** Yalniz <template> bloklarini dondurur (script'teki dizeleri tarama). */
const sablonBlogu = (kod) => {
  const bas = kod.indexOf('<template>')
  const son = kod.lastIndexOf('</template>')
  return bas === -1 || son <= bas ? '' : kod.slice(bas, son)
}

/** Yalniz <script setup> bloklarini dondurur. */
const scriptKodu = (s) => {
  const bas = s.indexOf('<script setup>')
  const son = s.lastIndexOf('</script>')
  return bas === -1 || son <= bas ? '' : yorumsuz(s.slice(bas, son))
}

/** `anchor` metninden baslayip suslu parantez DENGESINI takip ederek biten
 *  blogu dondurur.
 *
 *  Neden sabit `slice(0, 400)` degil? Cunku 400 karakterlik pencere bir
 *  ACIKLAMA satiri eklendiginde koda degil yoruma gore kirilir. Buradaki
 *  denetimler kodun YAPISINI (hangi cagri hangi fonksiyonun icinde) kontrol
 *  ediyor; yorum eklendikce de dogru kalmali. */
const blokBul = (kod, anchor) => {
  const bas = kod.indexOf(anchor)
  if (bas === -1) return ''
  const suAc = kod.indexOf('{', bas)
  if (suAc === -1) return ''
  let derinlik = 0
  for (let i = suAc; i < kod.length; i++) {
    if (kod[i] === '{') derinlik++
    else if (kod[i] === '}') {
      derinlik--
      if (derinlik === 0) return kod.slice(bas, i + 1)
    }
  }
  return kod.slice(bas)
}

/** Projedeki tum .vue dosyalari (capraz dosya kurallari icin). */
const vueDosyalari = () => {
  const bul = (klasor, cikti = []) => {
    for (const ad of readdirSync(klasor)) {
      const yol = join(klasor, ad)
      if (statSync(yol).isDirectory()) bul(yol, cikti)
      else if (ad.endsWith('.vue')) cikti.push(yol)
    }
    return cikti
  }
  return bul(KOK)
}

/** POS sayfa kabugu stilleri (bkz. ASAMA 1 notu): @import edilen CSS dosyasi. */
const posStilKod = yorumsuz(readFileSync(join(KOK, 'assets', 'pos-hizli-satis.css'), 'utf8'))

// ===========================================================================
// (A) Once davranis testiyle kapsali kurallar — SILINDI, yerine:
//   PosUrunKarti.spec.js : roving tabindex, aria-selected, role=option,
//                          sag tik -> `adet-ist`, odakli kart isareti,
//                          stok rozeti renk esikleri
//   PosSepetPaneli.spec.js: satir eylem menusu -> duzenle/cogalt/adedi sifirla
// ===========================================================================

// ---------------------------------------------------------------------------
// (B) Izgara klavye modu.
//     TUS -> EYLEM eslesmesi artik `usePosKisayollar.js` icinde; burada yalniz
//     view'in o eylemlere KARSILIK veren fonksiyonlari ve izgara odagi
//     baglantisi denetlenir. Esleme kurallarinin kendisi:
//     src/composables/__tests__/usePosKisayollar.spec.js
// ---------------------------------------------------------------------------
describe('Faz4.1 - urun izgarasi klavyeye acilmali', () => {
  it('izgara odak durumu ve hareket fonksiyonu view icinde tanimli', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toContain('urunIzgaraOdak')
    // Yon tuslarinin cagirdigi hedef fonksiyonlar view'da olmali
    expect(kod).toContain('urunIzgaraHareket')
    expect(kod).toContain('urunIzgarayaGir')
    expect(kod).toContain('urunIzgaradanCik')
  })

  it('izgara hareketi yatayda da sarma yapabilmeli (tek boyutlu degil)', () => {
    // Yatay hareket ayri ele alinir; aksi halde son sutundan ilk sutuna
    // gecilemez. Kapsam: hareket fonksiyonunun TAMAMI.
    const kod = scriptKodu(posKod)
    const hareket = blokBul(kod, 'const urunIzgaraHareket')
    expect(hareket).toContain('dx')
    expect(hareket).toContain('dy')
  })

  it('izgara modunda Enter urunu sepete ekler', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toContain('urunIzgaraSec')
    // Enter tusu composable'da bu fonksiyona baglanir
    expect(posKisayolKod).toContain('urunIzgaraSec()')
  })

  it('bir metin alanindayken ↓ ile izgaraya girilir', () => {
    // Once izgaraya gecmenin HICBIR yolu yoktu.
    // Esleme composable'da; view'de hedef fonksiyon tanimli olmali.
    const kod = scriptKodu(posKod)
    expect(kod).toContain('urunIzgarayaGir')
    expect(posKisayolKod).toContain("e.key !== 'ArrowDown'")
  })

  it('kartlar roving tabindex + liste semantigi ile duyuruluyor', () => {
    const sablon = yorumsuz(sablonBlogu(posKod))
    // Izgara bir "secim listesi" oldugu icin listbox/option semantigi sart.
    expect(sablon).toContain('role="listbox"')
    expect(kartKod).toContain('role="option"')
    // Roving tabindex: odakli kart 0, digerleri -1
    expect(sablon).toContain(':tabindex="urunIzgaraOdak ?')
    expect(sablon).toContain(':odakli="urunIzgaraOdak && i === urunIzgaraIndeks"')
  })

  it('odakli kart gorunur bicimde isaretlenir (fare takibi yokken konum bilinmeli)', () => {
    // Stil `assets/pos-hizli-satis.css` icinde (bkz. yukaridaki ASAMA 1 notu).
    expect(posStilKod).toMatch(/\.product-card\.izgara-odakli\s*\{/)
  })
})

// ---------------------------------------------------------------------------
// (B) Hizli adet — view ici.
//     TASIMA HEDEFI: Faz2 `PosAdetGirisi.vue` + `usePosSepet`
//     KART tarafi artik PosUrunKarti.spec.js'de mount ile kapsali.
// ---------------------------------------------------------------------------
describe('Faz4.2 - hizli adet', () => {
  it('kart sag tik ile adet penceresi acar', () => {
    // REDTEAM/Faz4: kart artik OLAYI da yayar (`$event`), boylece adet
    // penceresi tiklanan noktanin yanina konumlanabilir.
    expect(kartKod).toContain('@contextmenu.prevent="emit(\'adet-ist\', $event)"')
    expect(kartKod).toContain("defineEmits(['sec', 'adet-ist'])")
    expect(scriptKodu(posKod)).toContain('adetPopoverAc')
  })

  it('rakam + Enter miktarli ekleme', () => {
    // Girdi durumu view'da, tus->eylem eslemesi composable'da.
    const kod = scriptKodu(posKod)
    expect(kod).toContain('urunIzgaraRakam')
    // Enter rakam varsa miktarli ekler
    expect(kod).toMatch(/urunIzgaraRakam\.value \? parseInt\(urunIzgaraRakam\.value, 10\) \|\| 1 : 1/)
    // Rakam tanima ve Backspace composable'da
    expect(posKisayolKod).toMatch(/\/\^\[0-9\]\$\/\.test\(e\.key\)/)
    expect(posKisayolKod).toMatch(/case 'Backspace':/)
  })

  it('Shift+Enter adet penceresini acar', () => {
    // View'de adet penceresini acan fonksiyon; tus eslemesi composable'da.
    expect(scriptKodu(posKod)).toContain('adetPopoverAc')
    expect(posKisayolKod).toContain('e.shiftKey')
  })

  // GERCEK IS KURALI: ayni urun tekrar eklenince AYRI SATIR acilmaz,
  // mevcut satirin miktari ARTAR. "Cogalt" bunu bilerek atlayan tek yoldur.
  // REDTEAM/Faz2: artma artik dogrulama katmanindan gecer
  // (`miktarDogrula` -> stok tavani), bu yuzden `+=` ifadesi yerine
  // denetim "TEK push yolu + erken donus" kuralini korur.
  // Davranis testi: src/utils/__tests__/posAdet.spec.js
  it('sepete ekleme ayni urunde duplicate satir acmaz (tek push yolu)', () => {
    const ekleBlogu = blokBul(yorumsuz(sepetKod), 'const sepeteEkle = async')
    // Var olan satir bulundugunda islem biter (yeni satir acilmaz).
    expect(ekleBlogu).toContain('varOlan')
    expect(ekleBlogu).toMatch(/varOlan\.miktar\s*=/)
    // Sepete ekleme noktasi TEK olmali; ikinci bir `push` duplicate uretirdi.
    const pushSayisi = (ekleBlogu.match(/sepet\.value\.push/g) || []).length
    expect(pushSayisi, 'sepete ekleme birden fazla noktadan olmali (duplicate satir riski)').toBe(1)
  })
})

// ---------------------------------------------------------------------------
// (B) Geri al — view ici.
//     TASIMA HEDEFI: Faz1 `usePosSepet.geriAl`
// ---------------------------------------------------------------------------
describe('Faz4.3 - geri al klavyeye bagli', () => {
  it('G ve Ctrl+Z geri al calistirir', () => {
    const kod = scriptKodu(posKod)
    // G: harf kisa yolu composable'da, hedef fonksiyon view'da
    expect(kod).toContain('geriAlYap')
    expect(posKisayolKod).toMatch(/case 'g':/)
    // Ctrl+Z ayri dinleyici (metin alanindayken tarayici geri al'i calisir)
    expect(posKisayolKod).toContain('handlePosUndo')
  })

  it('her iki dinleyici de mount/unmount ediliyor', () => {
    // Dinleyici kaydi artik composable'in sorumlulugu: view mount olunca
    // `usePosKisayollar` cagrilir, o da window'a baglar.
    expect(posKisayolKod).toMatch(/addEventListener\('keydown', handlePosKeys, true\)/)
    expect(posKisayolKod).toMatch(/removeEventListener\('keydown', handlePosKeys, true\)/)
    expect(posKisayolKod).toMatch(/addEventListener\('keydown', handlePosUndo, true\)/)
    expect(posKisayolKod).toMatch(/removeEventListener\('keydown', handlePosUndo, true\)/)
    expect(scriptKodu(posKod)).toContain('usePosKisayollar(')
  })

  it('satir silme geri alinabilir', () => {
    // Once `Del` ile silinen satir SADECE fare ile geri alinabiliyordu.
    expect(blokBul(yorumsuz(sepetKod), 'const sepetSil')).toContain('geriAlSatirKaydet(idx)')
  })

  it('cogaltma geri alinabilir (kayit cogalt modunda)', () => {
    expect(yorumsuz(sepetKod)).toContain('geriAlSatirCogaltKaydet')
    // Cogaltma geri almasi KOPYAYI kaldirmali, yeniden eklememeli
    expect(yorumsuz(sepetKod)).toMatch(/if \(mod === 'cogalt'\) \{[\s\S]*?splice\(hedef, 1\)/)
  })
})

// ---------------------------------------------------------------------------
// (C) Klavye altyapisi — `useKisayollar` sozlesmesi (dosya tasinmayacak)
// ---------------------------------------------------------------------------
describe('Faz4.4 - POS yerel ipucu ve g-gezinme cakismasi', () => {
  it('useKisayollar ipucu eylemini destekliyor', () => {
    expect(kisayolKod).toContain("calistir('ipucu')")
    // Sayfa ipucu veriyorsa global rehber devreye girmemeli.
    // Kontrol `?` tusunun TUM `if` blogu uzerinde yapilir; boylece hem
    // blogun ici dogru, hem de blogun disinda ikinci bir `calistir('ipucu')`
    // cagrisi olmadigi gorulur.
    const qBlok = blokBul(yorumsuz(kisayolKod), "e.key === '?'")
    expect(qBlok).toMatch(/if \(!calistir\('ipucu'\)\)/)
  })

  it('gezinmeKapat secenegi sayfaya aciliyor', () => {
    expect(kisayolKod).toContain('gezinmeAskida')
    expect(kisayolKod).toMatch(/gezinmeAskida === 0/)
    // Saya asagi duserse gecici kisayollar sayfadan cikinca gezinme KAPANIR kalmasin
    expect(kisayolKod).toMatch(/Math\.max\(0, gezinmeAskida - 1\)/)
  })

  it('POS gezinmeyi kapatiyor ve yerel ipucunu veriyor', () => {
    // TASIMA HEDEFI: Faz1 `usePosKisayollar` birim testi
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/gezinmeKapat: true/)
    expect(kod).toMatch(/ipucu: \(\) => ipucuToggle\(\)/)
  })
})

// ---------------------------------------------------------------------------
// (B) Fis yazdirma kisayollari — view ici.
//     TASIMA HEDEFI: Faz1 `utils/posFis.js`
// ---------------------------------------------------------------------------
describe('Faz4.5 - fis yazdirma kisayolu duzeltildi', () => {
  it('yanlis F9 etiketi giderildi, F8/F11 eklendi', () => {
    // F8/F11 tus eslemesi composable'da; ipucu seridi etiketleri view'da.
    expect(posKisayolKod).toMatch(/case 'F8':/)
    expect(posKisayolKod).toMatch(/case 'F11':/)
    // Artik `hizliSatis.yazdirF9` diye bir etiket OLMAMALI. KOD okunur:
    // dosyada "yazdirF9" gecen yerler yalniz duzeltme ACIKLAMALARINDA olabilir.
    const kodSade = yorumsuz(posKod)
    expect(kodSade).not.toContain('hizliSatis.yazdirF9')
    expect(kodSade).toContain('hizliSatis.yazdirF8')
    expect(kodSade).toContain('hizliSatis.termalF11')
  })

  it('fis blogu satis sonrasi da gorunur (yeniden yazdirma mumkun)', () => {
    // Once `v-if="sepet.length > 0"` idi: satis bitince bolum KAYBOLUYORDU.
    expect(yorumsuz(sablonBlogu(posKod))).toContain('v-if="sepet.length > 0 || sonSatis"')
  })

  it('F7 bugunku satislari acar', () => {
    expect(posKisayolKod).toMatch(/case 'F7':/)
    expect(scriptKodu(posKod)).toContain('bugunkuSatislariAc')
  })
})

// ---------------------------------------------------------------------------
// (A+C) Satir eylem menusu baglantisi — ARTUK mount ile kapsali.
//     `PosSepetPaneli.spec.js` "menudeki adedi sifirla adedi-sifirla yayar"
//     ve "urun adina tiklamak urun-degistir-ac yayar" testlerine bakin.
//     Asagidaki denetim genel MIMARI kurala genisletildi: hicbir .vue
//     dosyasinda `defineEmits` iki kez tanimlanmamali (cift cagri / lostuk).
// ---------------------------------------------------------------------------
describe('Faz4.6 - defineEmpts cift tanimi olmamali (capraz dosya kurali)', () => {
  it('hicbir .vue dosyasinda defineEmpts iki kez tanimlanmamali', () => {
    const ihlaller = []
    for (const dosya of vueDosyalari()) {
      const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
      const atamali = (icerik.match(/const\s+emit\s*=\s*defineEmpts\s*\(/g) || []).length
      const cift = (icerik.match(/^defineEmpts\s*\(/gm) || []).length
      if (atamali > 1) {
        ihlaller.push(
          `${relative(process.cwd(), dosya)}  'const emit = defineEmpts' ${atamali} kez tanimli`
        )
      }
      if (cift > 0) {
        ihlaller.push(
          `${relative(process.cwd(), dosya)}  'defineEmpts(' atamasi ${cift} kez; ` +
            'emit degiskeni kullanilmiyor ve cift tanim riski var'
        )
      }
    }
    expect(
      ihlaller,
      'defineEmpts cift tanimi bulundu:\n' + ihlaller.join('\n')
    ).toEqual([])
  })
})