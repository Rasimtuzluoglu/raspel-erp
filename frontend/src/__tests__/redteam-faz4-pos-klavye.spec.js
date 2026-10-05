import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz4 — POS klavyeye gore yeniden tasarim: regresyon denetimleri.
 *
 * Bu dosya bilesen MOUNT ETMEZ; kaynak kodu okuyup kural ihlalini tespit eder.
 * Denetlenen bulgular hicbir derleyici/ESLint uyarisi URETMIYORDU ve hicbir
 * mevcut test yakalamiyordu:
 *
 *  1) Izgara klavyeyle GIRILEMIYORDU. `↑/↓` oklari sepette geziyordu ve urun
 *     kartlari hicbir zaman odaklanmiyordu. Kasada urun kartini secmek icin
 *     fare zorunluydu; barkod okutmayan satis akislari (kasa, menu, siparis)
 *     tamamen fareye bagliydi.
 *  2) HIZLI ADET YOKTU. `PosUrunKarti` yalnizca `emit('sec')` ile 1 adet
 *     ekliyordu; 20 adet almak icin 20 kez tiklamak gerekiyordu.
 *  3) GERI AL klavyeye bagli DEGILDI. Iki seviyeli geri alma vardi ama yalnizca
 *     fare ile basilabiliyordu.
 *  4) `?` POS'ta global KisayolRehberi'nin yerine yerel ipucunu acmiyordu.
 *  5) `g h` POS'tan cikis kisayolu OLMUYORDU: `n/k/h` odeme yontemi tuslari
 *     `useKisayollar`'in `g`-gezinme harflerini yutuyordu.
 *  6) "Yazdır (F9)" etiketi YANLISDI: F9 satisi tamamliyor, yazdirma yalnizca
 *     Ctrl+P ile mumkundu; ayrica satis sonrasi fiy yeniden basilamiyordu
 *     (`<details>` yalnizca `sepet.length > 0` iken gorunuyordu).
 *  7) Satiri cogaltma YALNIZCA klavyeden mumkundu ve hicbir menude gorunmuyordu
 *     (`SatirEylemleri` POS'a bagli degildi).
 */

const KOK = join(process.cwd(), 'src')
const posKod = readFileSync(join(KOK, 'views/HizliSatis.vue'), 'utf8')
const kartKod = readFileSync(join(KOK, 'components/PosUrunKarti.vue'), 'utf8')
const sepetPanelKod = readFileSync(join(KOK, 'components/PosSepetPaneli.vue'), 'utf8')
const kisayolKod = readFileSync(join(KOK, 'composables/useKisayollar.js'), 'utf8')

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

// ---------------------------------------------------------------------------
describe('Faz4.1 - urun izgarasi klavyeye acilmali', () => {
  it('izgara odak modu tanimli ve oklarla geziliyor', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toContain('urunIzgaraOdak')
    // Yatay ve dikey hareket ayri ele alinir (sarma icin sutun sayisi gerekir)
    expect(kod).toContain('urunIzgaraHareket')
    expect(kod).toMatch(/e\.key === 'ArrowLeft'/)
    expect(kod).toMatch(/e\.key === 'ArrowRight'/)
  })

  it('izgara modunda Enter urunu sepete ekler', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/if \(urunIzgaraOdak\.value\) \{/)
    expect(kod).toMatch(/e\.key === 'Enter'\) \{ e\.preventDefault\(\); urunIzgaraSec\(\)/)
  })

  it('bir metin alanindayken ↓ ile izgaraya girilir', () => {
    // Once izgaraya gecmenin HICBIR yolu yoktu.
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/girdideMi\(e\) && e\.key === 'ArrowDown'/)
    expect(kod).toContain('urunIzgarayaGir')
  })

  it('kartlar roving tabindex + aria ile duyuruluyor', () => {
    const sablon = yorumsuz(sablonBlogu(posKod))
    expect(sablon).toContain(':tabindex="urunIzgaraOdak ?')
    expect(sablon).toContain(':odakli="urunIzgaraOdak && i === urunIzgaraIndeks"')
    expect(sablon).toContain('role="listbox"')
    // Kart bileseni bu ozellikleri kabul etmeli
    expect(kartKod).toContain('odakli: { type: Boolean')
    expect(kartKod).toContain("role=\"option\"")
    expect(kartKod).toContain(':aria-selected="odakli"')
  })

  it('odakli kart gorunur bicimde isaretlenir (fare takibi yokken konum bilinmeli)', () => {
    expect(posKod).toMatch(/\.product-card\.izgara-odakli\s*\{/)
  })
})

// ---------------------------------------------------------------------------
describe('Faz4.2 - hizli adet', () => {
  it('kart sag tik ile adet penceresi acar', () => {
    expect(kartKod).toContain('@contextmenu.prevent="emit(\'adet-ist\')"')
    expect(kartKod).toContain("defineEmits(['sec', 'adet-ist'])")
    expect(scriptKodu(posKod)).toContain('adetPopoverAc')
  })

  it('rakam + Enter miktarli ekleme', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toContain('urunIzgaraRakam')
    expect(kod).toMatch(/\/\^\[0-9\]\$\/\.test\(e\.key\)/)
    // Enter rakam varsa miktarli ekler
    expect(kod).toMatch(/urunIzgaraRakam\.value \? parseInt\(urunIzgaraRakam\.value, 10\) \|\| 1 : 1/)
    // Backspace rakami siler
    expect(kod).toMatch(/e\.key === 'Backspace'/)
  })

  it('Shift+Enter adet penceresini acar', () => {
    expect(scriptKodu(posKod)).toMatch(/e\.key === 'Enter' && e\.shiftKey/)
  })

  it('sepeteEkle adet parametresi alir', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/const sepeteEkle = async \(u, adet = 1\)/)
    // Var olan satira EKLENIR, yeni satira miktar ile yazilir
    expect(kod).toMatch(/varOlan\.miktar \+= miktar/)
    expect(kod).toMatch(/miktar,\s*\n\s*fiyat: temelFiyatlar/)
  })
})

// ---------------------------------------------------------------------------
describe('Faz4.3 - geri al klavyeye bagli', () => {
  it('G ve Ctrl+Z geri al calistirir', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/k === 'g'\) \{/)
    expect(kod).toContain('geriAlYap')
    // Ctrl+Z ayri dinleyici: metin alanindayken tarayici geri al'i calisir
    expect(kod).toContain('handlePosUndo')
    expect(kod).toMatch(/if \(girdideMi\(e\)\) return/)
  })

  it('her iki dinleyici de mount/unmount ediliyor', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/addEventListener\('keydown', handlePosUndo, true\)/)
    expect(kod).toMatch(/removeEventListener\('keydown', handlePosUndo, true\)/)
  })

  it('satir silme geri alinabilir', () => {
    // Once `Del` ile silinen satir SADECE fare ile geri alinabiliyordu.
    const kod = scriptKodu(posKod)
    const silFn = kod.slice(kod.indexOf('const sepetSil'))
    expect(silFn.slice(0, 400)).toContain('geriAlSatirKaydet(idx)')
  })

  it('cogaltma geri alinabilir (kayit cogalt modunda)', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toContain('geriAlSatirCogaltKaydet')
    // Cogaltma geri almasi KOPYAYI kaldirmali, yeniden eklememeli
    expect(kod).toMatch(/if \(mod === 'cogalt'\) \{[\s\S]*?splice\(hedef, 1\)/)
  })
})

// ---------------------------------------------------------------------------
describe('Faz4.4 - POS yerel ipucu ve g-gezinme cakismasi', () => {
  it('useKisayollar ipucu eylemini destekliyor', () => {
    expect(kisayolKod).toContain("calistir('ipucu')")
    // Sayfa ipucu veriyorsa global rehber devreye girmemeli
    const q = kisayolKod.slice(kisayolKod.indexOf("e.key === '?'"))
    expect(q.slice(0, 400)).toMatch(/if \(!calistir\('ipucu'\)\)/)
  })

  it('gezinmeKapat secenegi sayfaya aciliyor', () => {
    expect(kisayolKod).toContain('gezinmeAskida')
    expect(kisayolKod).toMatch(/gezinmeAskida === 0/)
    // Saya asagi duserse gecici kisayollar sayfadan cikinca gezinme KAPANIR kalmasin
    expect(kisayolKod).toMatch(/Math\.max\(0, gezinmeAskida - 1\)/)
  })

  it('POS gezinmeyi kapatiyor ve yerel ipucunu veriyor', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/gezinmeKapat: true/)
    expect(kod).toMatch(/ipucu: \(\) => ipucuToggle\(\)/)
  })
})

// ---------------------------------------------------------------------------
describe('Faz4.5 - fis yazdirma kisayolu duzeltildi', () => {
  it('yanlis F9 etiketi giderildi, F8/F11 eklendi', () => {
    const kod = scriptKodu(posKod)
    expect(kod).toMatch(/if \(e\.key === 'F8'\)/)
    expect(kod).toMatch(/if \(e\.key === 'F11'\)/)
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
    expect(scriptKodu(posKod)).toMatch(/if \(e\.key === 'F7'\)/)
    expect(scriptKodu(posKod)).toContain('bugunkuSatislariAc')
  })
})

// ---------------------------------------------------------------------------
describe('Faz4.6 - satir eylem menusu POS sepetine bagli', () => {
  it('PosSepetPaneli SatirEylemleri kullaniyor', () => {
    expect(sepetPanelKod).toContain("import SatirEylemleri from './SatirEylemleri.vue'")
    expect(sepetPanelKod).toContain('<SatirEylemleri')
    // Duzenle (urun degistir) + Cogalt olaylari baglanmis olmali
    expect(sepetPanelKod).toContain("@duzenle=\"$emit('urun-degistir-ac', idx)\"")
    expect(sepetPanelKod).toContain("@cogalt=\"$emit('cogalt', idx)\"")
  })

  it('HizliSatis bu olaylari dinliyor', () => {
    const kod = yorumsuz(sablonBlogu(posKod))
    expect(kod).toContain('@cogalt="satiriCogalt"')
    expect(kod).toContain('@adedi-sifirla="adediSifirla"')
  })

  it('emit tanimi `const emit = defineEmits(...)` olmali (cift tanim yok)', () => {
    expect(sepetPanelKod).toMatch(/const emit = defineEmits/)
    expect(sepetPanelKod).not.toMatch(/^defineEmits\(/m)
  })
})