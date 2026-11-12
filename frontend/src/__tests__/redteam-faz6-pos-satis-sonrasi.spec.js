import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz6 — Satis SONRASI akis (Hizli Satis).
 *
 * DORT KUSUR:
 *  1) Barkod okutulduktan sonra arama kutusunun metni/onerileri ve izgara
 *     filtresi (`katalogArama`) DOLU kaliyordu. Bir sonraki urunu okutana kadar
 *     izgara eksik listeleniyor, oneri listesi kapanmis kutuya ragmen acik
 *     saniyordu.
 *  2) Fis yalnizca sepette urun varken yazdirilabiliyordu. Satis tamamlanip
 *     sepet temizlenince "Yazdir" butonu ve fis onizlemesi KAYBOLUYORDU; musteri
 *     "fisi tekrar ver" dediginde kasiyerin hicbir yolu yoktu.
 *  3) Ad aramasinda ("vida") sonuc bulunamazsa "yeni urun" formu aciliyordu —
 *     oysa kullanici yalnizca ARAMA yapmisti.
 *  4) Ozet dialogu kapandiktan sonra odak dialog icinde kaliyordu; bir sonraki
 *     okutma icin fareyle barkod kutusuna tiklamak gerekiyordu.
 */

const KOK = join(process.cwd(), 'src')
const posKod = readFileSync(join(KOK, 'views/HizliSatis.vue'), 'utf8')

const yorumsuz = (s) =>
  s
    .replace(/<!--[\s\S]*?-->/g, '')
    .replace(/\/\*[\s\S]*?\*\//g, '')
    .split(/\r?\n/)
    .map((satir) => satir.replace(/(^|\s)\/\/.*$/, ''))
    .join('\n')

const scriptKodu = (s) => {
  const bas = s.indexOf('<script setup>')
  const son = s.lastIndexOf('</script>')
  return bas === -1 || son <= bas ? '' : yorumsuz(s.slice(bas, son))
}

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

describe('Faz6.1 - basarili eklemeden sonra arama durumu sifirlanir', () => {
  it('komutTemizle katalog aramasini ve onerileri temizler', () => {
    const s = scriptKodu(posKod)
    const blok = blokBul(s, 'const komutTemizle =')
    expect(blok).toContain("katalogArama.value = ''")
    expect(blok).toContain('urunOnerileri.value = []')
    expect(blok).toContain('komutOnerileriAcik.value = false')
  })

  it('barkodTarandi basarili her yolda komutTemizle cagirir', () => {
    const blok = blokBul(scriptKodu(posKod), 'const barkodTarandi =')
    // Katalogda bulundu, sunucuda bulundu, ad aramasinda ilk sonuc.
    expect(blok.match(/komutTemizle\(\)/g)?.length || 0).toBeGreaterThanOrEqual(3)
  })

  it('oneri listesinden secimde de arama temizlenir', () => {
    const blok = blokBul(scriptKodu(posKod), 'const komutSec =')
    expect(blok).toContain('komutTemizle()')
  })
})

describe('Faz6.2 - fis satistan sonra da yazdirilabilir (snapshot)', () => {
  it('sepet temizlense de fis snapshot uzerinden basilir', () => {
    const s = scriptKodu(posKod)
    expect(s).toContain('const sonSepet = ref([])')
    expect(s).toContain('const fisSepeti = computed')
    expect(s).toContain('sonSepet.value')
    expect(s).toContain('const sonFisOzet = ref(null)')
    expect(s).toContain('return sonFisOzet.value')
  })

  it('basarili satista snapshot kaydedilir', () => {
    const blok = blokBul(scriptKodu(posKod), 'const satisBasarili =')
    expect(blok).toContain('sonSepet.value = sepet.value.map')
    expect(blok).toContain('sonFisOzet.value = {')
  })

  it('yazdirma sepet yerine snapshot kullanir', () => {
    const s = scriptKodu(posKod)
    const yazdir = blokBul(s, 'const fisiYazdir =')
    expect(yazdir).toContain('fisSepeti.value')
    expect(yazdir).not.toContain('sepet: sepet.value')
    const termal = blokBul(s, 'const termalYazdir =')
    expect(termal).toContain('fisSepeti.value')
  })

  it('fis bolumu snapshot varken de gorunur kalir', () => {
    const sablon = yorumsuz(posKod)
    expect(sablon).toContain('v-if="sepet.length > 0 || sonSepet.length > 0"')
    expect(sablon).toContain(':sepet="fisSepeti"')
  })
})

describe('Faz6.3 - ad aramasi yeni urun formunu acmaz', () => {
  it('ad aramasi barkod benzeri koddan ayrilir', () => {
    const blok = blokBul(scriptKodu(posKod), 'const barkodTarandi =')
    expect(blok).toContain('const adAramasi = !kodBenzeri')
    expect(blok).toContain('if (adAramasi)')
  })

  it('ad aramasinda ilk sonuc sepete eklenir', () => {
    const blok = blokBul(scriptKodu(posKod), 'const barkodTarandi =')
    expect(blok).toContain('liste[0] || urunOnerileri.value[0]')
    expect(blok).toContain('else sepeteEkle(ilk, adet)')
  })

  it('yeni urun formu yalniz barkod benzeri aramada acilir', () => {
    const blok = blokBul(scriptKodu(posKod), 'const barkodTarandi =')
    const adDal = blok.indexOf('if (adAramasi) {')
    const formAc = blok.indexOf('hizliUrunDialog.value = true')
    expect(adDal).toBeGreaterThan(-1)
    // Ad aramasi dalindan `return` ile cikilir; form satiri ondan SONRA gelir.
    expect(blok.indexOf('return', adDal)).toBeLessThan(formAc)
  })
})

describe('Faz6.4 - odak ve fis saati', () => {
  it('ozet dialogu kapaninca barkod alanina odak doner', () => {
    const s = scriptKodu(posKod)
    expect(s).toMatch(/watch\(satisOzetDialog,[\s\S]{0,160}odakla\(barkodInputRef\)/)
  })

  it('fis tarihi her yazdirmada tazelenir', () => {
    const s = scriptKodu(posKod)
    expect(s).toContain('const simdikiTarih = ref(formatDateTime(new Date()))')
    const yazdir = blokBul(s, 'const fisiYazdir =')
    expect(yazdir).toContain('simdikiTarih.value = formatDateTime(new Date())')
    const termal = blokBul(s, 'const termalYazdir =')
    expect(termal).toContain('simdikiTarih.value = formatDateTime(new Date())')
  })
})
