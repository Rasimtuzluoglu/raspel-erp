import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz4 — Satir duzenleme/silme kurallari.
 *
 * SIKAYET ("satir duzenlemek/silmek karisik"): uc somut kusur vardi:
 *
 *  1) DUPLICATE SATIR: "Cogalt" ayni urunun ayni fiyat tipiyle ikinci
 *     satirini aciyordu. Karttan yeniden ekleme `find` ile yalnizca ILK
 *     eslesmeyi buldugu icin kopyadaki miktar artmiyordu → "ekledim ama
 *     artmadi". Kural: satir benzersizligi `(stokId + fiyatTipi)`.
 *  2) AKTIF SATIR SENKRONU: satir icindeki dugmeler `@click.stop` ile
 *     kabarmayi engelledigi icin klavye odagi BASKA satirda kaliyordu;
 *     bir satiri tiklayip `Del`e basmak YANLIS satiri siliyordu.
 *  3) ADET PENCERESI: `position: fixed` ama `top/left` yoktu → tiklanan
 *     karttan bagimsiz, ekranin ortasinda aciliyordu.
 *
 * Bu dosya view + bilesen kaynagindaki SOZLESMEYI denetler; davranislar
 * mount/birim testleriyle ayrica kapsanir (PosSepetPaneli.spec.js,
 * PosAdetGirisi.spec.js).
 */

const KOK = join(process.cwd(), 'src')
const posKod = readFileSync(join(KOK, 'views/HizliSatis.vue'), 'utf8')
// REDTEAM/Faz4 sonrasi sepet cekirdedi `usePosSepet` composable'ina tasindi.
// Kurallar artik orada; bu testler doğru dosyayi taramali.
const sepetKod = readFileSync(join(KOK, 'composables/usePosSepet.js'), 'utf8')
const kartKod = readFileSync(join(KOK, 'components/PosUrunKarti.vue'), 'utf8')
const adetKod = readFileSync(join(KOK, 'components/PosAdetGirisi.vue'), 'utf8')

const yorumsuz = (s) =>
  s
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .split(/\r?\n/)
    .map((satir) => satir.replace(/(^|\s)\/\/.*$/, ''))
    .join('\n')

/** Yalniz <template> bloklarini dondurur. */
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

/** Anchor'dan baslayip suslu parantez dengesiyle biten blok. */
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

describe('Faz4.1 - satir benzersizligi (stokId + fiyatTipi)', () => {
  it('sepete ekleme satiri id + FIYAT TIPI ile bulur (yalniz id degil)', () => {
    const ekle = blokBul(yorumsuz(sepetKod), 'const sepeteEkle = async')
    expect(ekle).toContain('sepetSatiriBul')
    // Yalniz-id aramasina geri donus bu hatayi geri getirir.
    expect(ekle).not.toMatch(/sepet\.value\.find\(\(i\) => i\.id === [a-z]+\.id\)/)
  })

  it('sepetSatiriBul kurali saf util fonksiyonuna devreder', () => {
    // Kuralin KENDISI (id + fiyatTipi eslesmesi) `utils/posSepet.js` icinde
    // ve davranis testi vardir: src/utils/__tests__/posSepet.spec.js
    const sarmal = blokBul(yorumsuz(sepetKod), 'const sepetSatiriBul =')
    expect(sarmal).toContain('posSatiriBul(sepet.value, id, fiyatTipi)')
  })

  it('zenginlestirme de (id + fiyatTipi) ile bulur, yalniz id ile DEGIL', () => {
    // Ayni urun iki fiyat tipiyle sepetteyken yanlis satirin fiyat listesi
    // ve cari gecmisiyle guncellenmesi bu kurala bagli.
    const ekle = blokBul(yorumsuz(sepetKod), 'const sepeteEkle = async')
    expect(ekle).toMatch(/sepetSatiriBul\(u\.id,\s*hedefFiyatTipi\)/)
  })

  it('cogaltma ayni fiyat tipini TEKRARLAMAZ; alternatif yoksa cogaltmaz', () => {
    const cogalt = blokBul(yorumsuz(sepetKod), 'const satiriCogalt =')
    // Alternatif fiyat tipi saf util'den secilir (birim testli).
    expect(cogalt).toContain('sonrakiFiyatTipi')
    // Alternatif yoksa kullaniciyi bilgilendirir ve ERKEN DONER (cogaltmaz)
    expect(cogalt).toContain('cogaltFiyatTipiYok')
    expect(cogalt).toContain('return false')
  })

  it('cogaltilan satira ALTERNATIF fiyat tipi ve fiyati atanir', () => {
    const cogalt = blokBul(yorumsuz(sepetKod), 'const satiriCogalt =')
    expect(cogalt).toMatch(/fiyatTipi:\s*alternatif\.ad/)
    expect(cogalt).toMatch(/fiyat:\s*alternatif\.fiyat/)
  })

  it('kart rozeti ayni urunun TUM satirlarini toplar (util devri)', () => {
    const adet = blokBul(yorumsuz(sepetKod), 'const sepetteAdet =')
    expect(adet).toContain('sepetteToplamAdet(sepet.value, id)')
  })
})

describe('Faz4.2 - aktif satir senkronu (fare <-> klavye)', () => {
  it('satir eylemleri aktif satiri hizalayan sarmalayicidan gecer', () => {
    const s = yorumsuz(sepetKod)
    expect(s).toContain('const satirAktifYap')
    const sarmal = blokBul(s, 'const satirAktifYap')
    expect(sarmal).toContain('aktifSatir.value = idx')
  })

  it('tum satir olaylari sarmalayiciya baglidir (stoplu tiklama dahil)', () => {
    const sablon = yorumsuz(sablonBlogu(posKod))
    for (const olay of ['sil', 'cogalt', 'miktar-azalt', 'miktar-artir', 'urun-degistir-ac', 'adedi-sifirla']) {
      expect(sablon, `@${olay} sarmalayici kullanmali`).toContain(`@${olay}="satirAktifYap(`)
    }
    // Obje yayan olay da aktif satiri hizalamali
    expect(sablon).toContain('@miktar-degistir="({ idx, miktar }) => { aktifSatir = idx;')
  })

  it('sevket edilen satir olaylari hala tanimli (kopuk baglanti yok)', () => {
    // Sepet islemleri composable'da, urun degistirme akisi view'da.
    const sepetTarafi = yorumsuz(sepetKod)
    for (const fn of ['sepetSil', 'satiriCogalt', 'adediSifirla', 'miktarAzalt']) {
      expect(sepetTarafi, `${fn} composable'da tanimli olmali`).toContain(`const ${fn} =`)
    }
    const viewTarafi = scriptKodu(posKod)
    for (const fn of ['urunDegistirAc']) {
      expect(viewTarafi, `${fn} view'da tanimli olmali`).toContain(`const ${fn} =`)
    }
  })
})

describe('Faz4.3 - adet girdisi odakta metni secer', () => {
  it('odaklaninca mevcut deger secilir (yazinca uzerine yazar)', () => {
    // 1 -> 20 yazmak isteyen kasiyer once "1"i silmek zorunda kalmamali.
    expect(adetKod).toContain('@focus="$event.target.select()"')
  })
})

describe('Faz4.4 - adet penceresi tiklanan kartin yanina konumlanir', () => {
  it('konum durumu ve stil hesaplamasi tanimli', () => {
    const s = scriptKodu(posKod)
    expect(s).toContain('adetPopoverPoz')
    expect(s).toContain('adetPopoverStil')
  })

  it('konum ekran disina tasmayacak sekilde sinirlanir', () => {
    const stil = blokBul(scriptKodu(posKod), 'const adetPopoverStil =')
    expect(stil).toMatch(/innerWidth/)
    expect(stil).toMatch(/innerHeight/)
    // Kirpma: Math.min/Math.max ile tavan ve taban
    expect(stil).toContain('Math.min')
    expect(stil).toContain('Math.max')
  })

  it('sag tik olayindan nokta alinir; yoksa odakli kartin kutusu kullanilir', () => {
    const konum = blokBul(scriptKodu(posKod), 'const adetPopoverKonumlandir =')
    expect(konum).toMatch(/clientX/)
    expect(konum).toMatch(/clientY/)
    expect(konum).toContain('getBoundingClientRect')
  })

  it('adetPopoverAc olayi opsiyonel 3. parametre olarak alir', () => {
    const ac = blokBul(scriptKodu(posKod), 'const adetPopoverAc =')
    expect(ac).toMatch(/\(u,\s*i,\s*olay\s*=\s*null\)/)
    expect(ac).toContain('adetPopoverKonumlandir')
  })

  it('template stili baglar ve kart olayi gecirir', () => {
    const sablon = yorumsuz(sablonBlogu(posKod))
    expect(sablon).toContain(':style="adetPopoverStil"')
    expect(sablon).toContain('@adet-ist="(olay) => adetPopoverAc(u, i, olay)"')
    expect(kartKod).toContain("emit('adet-ist', $event)")
  })
})

describe('Faz4.5 - adet penceresi stok yoksa ACILMAZ', () => {
  it('stoksuz urunde pencere yerine uyari verilir', () => {
    const ac = blokBul(scriptKodu(posKod), 'const adetPopoverAc =')
    expect(ac).toContain('stokYokMu(u)')
    expect(ac).toContain('stokYokUyari')
    // Erken donus: pencere acilmaz
    expect(ac).toMatch(/return\s*\n?\s*\}/)
  })
})
