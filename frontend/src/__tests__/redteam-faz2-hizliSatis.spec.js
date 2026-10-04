import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

/**
 * REDTEAM/Faz2 regresyon testleri — kaynak-kodu tarayan denetimler.
 *
 * Bu testler bilesen MOUNT ETMEZ; dosyalari okuyup kural ihlali tespit eder.
 * Amac: "tuzak" hatalarin tekrar tekrar girmesini engellemek. Iki hata turu
 * Turkce'de "hata" gibi görünmüyor ama calisma zamaninda sessizce bozukluk
 * yaratıyordu:
 *
 *  1) Teleport + CSS prefix: PrimeVue overlay'leri `<body>`'ye teleport eder.
 *     Bir slot icerigine `.container X` yazmak, teleport sonrasi X'i hedeflemez
 *     ve kural HICBIR UYGULANMAZ (sessiz).
 *  2) Katman z-index: `--z-*` jetonlari tanimliydi ama 0 kez kullaniliyordu;
 *     sabit sayilar PrimeVue varsayilanlariyla kesisiyordu.
 */

const KOK = join(process.cwd(), 'src')
const CSS_DOSYALARI = [
  join(KOK, 'assets', 'app.css'),
  join(KOK, 'assets', 'pos-panels.css')
]

function vueDosyalari() {
  const sonuc = []
  const yigin = [KOK]
  while (yigin.length) {
    const yol = yigin.pop()
    let girdiler
    try {
      girdiler = readdirSync(yol)
    } catch {
      continue
    }
    for (const g of girdiler) {
      if (g === 'node_modules' || g === '__snapshots__') continue
      const tam = join(yol, g)
      let st
      try {
        st = statSync(tam)
      } catch {
        continue
      }
      if (st.isDirectory()) yigin.push(tam)
      else if (g.endsWith('.vue')) sonuc.push(tam)
    }
  }
  return sonuc
}

/**
 * Yorum satirlarini temizler.
 * REDTEAM notu: Tarayici KODU denetlemelidir, metni degil. Duzeltmelerin
 * kendi yorumlarinda kurali ANLATAN ifadeler (orn.
 * "previously `:panel-style="{ minWidth: '380px' }"`") tarayiciyi yaniltiyordu.
 */
function yorumsuz(icerik) {
  return icerik
    .split(/\r?\n/)
    .map((satir) =>
      satir
        // HTML yorumu
        .replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, ' '))
        // satir sonu veya blok basi // yorumu
        .replace(/(^|\s)\/\/.*$/, (m) => m.replace(/[^\n]/g, ' '))
        // blok yorumu
        .replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' '))
    )
    .join('\n')
}

// ---------------------------------------------------------------------------
describe('REDTEAM Faz2.1 - Teleport edilen slot iceriklerinde CSS prefix', () => {
  // Bu sinif siniflari PrimeVue Dropdown/AutoComplete/OverlayPanel'in #option
  // ya da #item slotunda kullanilir -> govdeye teleport edilir -> icinde
  // bulundugu bir kapsayici (container/pos-container) ATA degildir.
  const TELEPORTLU_SINIFLAR = [
    'personel-opsiyon',
    'musteri-option',
    'urun-oneri'
  ]

  it('teleport edilen slot siniflari icin kurallar ONESIZ tanimlanmis olmali', () => {
    const ihlaller = []
    for (const cssYol of CSS_DOSYALARI) {
      const icerik = yorumsuz(readFileSync(cssYol, 'utf8'))
      const satirlar = icerik.split(/\r?\n/)
      satirlar.forEach((satir, i) => {
        for (const sinif of TELEPORTLU_SINIFLAR) {
          // `.container .sinif {` veya `.sinif .alt-sinif {` desenlerini ara
          const onEkli = new RegExp(
            `^\\s*\\.[a-z0-9-]+\\s+\\${sinif}\\b`,
            'i'
          ).test(satir)
          // `.sinif > span` gibi alt-secici KENDI sinifi oneksizdir; sorun degil
          if (onEkli) {
            ihlaller.push(
              `${relative(process.cwd(), cssYol)}:${i + 1}  ` +
              `"${satir.trim()}"  -> ".${sinif}" teleport sonrasi bu oneksiz kural OGUSMEZ`
            )
          }
        }
      })
    }
    expect(
      ihlaller,
      'Teleport edilen slot icerikleri icin onekli CSS kurali bulundu:\n' +
      ihlaller.join('\n')
    ).toEqual([])
  })

  it('slot siniflari oneksiz tanimlanmis olmali (`.personel-opsiyon` gibi)', () => {
    const posPanels = yorumsuz(readFileSync(join(KOK, 'assets', 'pos-panels.css'), 'utf8'))
    expect(
      posPanels,
      '`.personel-opsiyon` kuralı oneksiz tanımlanmalı (dropdown option slotu teleport edilir)'
    ).toMatch(/^\.personel-opsiyon\s*\{/m)
  })
})

// ---------------------------------------------------------------------------
describe('REDTEAM Faz2.2 - AutoComplete `dropdown` oku + minLength cakismasi', () => {
  // `dropdown` + `min-length="2"` birlikte kullanildiginda ok ISLEVSIZDIR:
  // ok'a tiklaninca `@complete` query:'' ile gelir, liste bosalir ve
  // PrimeVue `hide()` cagirir -> panel acilip aninda kapanir.
  it('AutoComplete `dropdown` ve `min-length` ayni elemanda bulunmamali', () => {
    const ihlaller = []
    for (const dosya of vueDosyalari()) {
      const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
      // <AutoComplete ... > bloklarini yakala
      const bloklar = icerik.match(/<AutoComplete[\s\S]*?>/g) || []
      bloklar.forEach((blok) => {
        const dropdownVar = /(^|\s)dropdown(\s|\n|\/|>|=)/.test(blok)
        const minLengthVar = /:min-length\s*=\s*"?\d/.test(blok)
        if (dropdownVar && minLengthVar) {
          const satirNo = icerik.slice(0, icerik.indexOf(blok)).split(/\r?\n/).length
          ihlaller.push(
            `${relative(process.cwd(), dosya)}:${satirNo}  ` +
            '`dropdown` + `:min-length` birlikte: ok panelyi acir ve hemen kapatir'
          )
        }
      })
    }
    expect(
      ihlaller,
      'AutoComplete dropdown/minLength çakışması bulundu:\n' + ihlaller.join('\n')
    ).toEqual([])
  })

  it('panel-style sabit referans kullanmali (her renderda yeni obje ziplama yapar)', () => {
    const ihlaller = []
    for (const dosya of vueDosyalari()) {
      const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
      // :panel-style="{ ... }" -> template literal (her render'da yeni obje)
      const desen = /:panel-style\s*=\s*"\s*\{/g
      let m
      while ((m = desen.exec(icerik)) !== null) {
        const satirNo = icerik.slice(0, m.index).split(/\r?\n/).length
        ihlaller.push(
          `${relative(process.cwd(), dosya)}:${satirNo}  ` +
          ':panel-style="{ ... }" -> her render\'da yeni obje; sabit referans kullanin'
        )
      }
    }
    expect(
      ihlaller,
      'Template literal panel-style bulundu:\n' + ihlaller.join('\n')
    ).toEqual([])
  })
})

// ---------------------------------------------------------------------------
describe('REDTEAM Faz2.3 - Cift gecikme (debounce) kalmamali', () => {
  it('AutoComplete :delay ve koddaki setTimeout birlikte kullanilmamali', () => {
    const dosya = join(KOK, 'views', 'HizliSatis.vue')
    const icerik = yorumsuz(readFileSync(dosya, 'utf8'))

    // :delay="0" -> PrimeVue debounce yok; gecikme tek yerden yonetilir
    expect(
      icerik,
      'Ürün arama AutoComplete :delay değeri 0 olmalı (kod zaten 250ms debounce yapıyor)'
    ).toMatch(/<AutoComplete[\s\S]*?:min-length="2"\s*\n\s*:delay="0"/)

    // Kodda tek bir setTimeout olmali (urunOneriZamanlayici)
    const setTimeoutSayisi = (icerik.match(/urunOneriZamanlayici\s*=\s*setTimeout/g) || []).length
    expect(setTimeoutSayisi, 'Ürün öneri gecikmesi tek noktada yönetilmeli').toBe(1)
  })
})

// ---------------------------------------------------------------------------
describe('REDTEAM Faz2.4 - POS stok katalogu 50 urunle sinirli degil', () => {
  it('stokStore.getAll parametresiz cagrilmamali (backend varsayilani 50 doner)', () => {
    const dosya = join(KOK, 'views', 'HizliSatis.vue')
    const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
    const parametresiz = icerik.match(/stokStore\.getAll\(\s*\)/g) || []
    expect(
      parametresiz,
      'stokStore.getAll() parametresiz çağrıldı: backend @PageableDefault(size=50) ' +
      'devreye girer ve "Daha fazla göster" butonu hiç görünmez ' +
      `(bulunan: ${parametresiz.length})`
    ).toEqual([])
  })

  it('stok yuklemesi global max-page-size (200) ile yapilmali', () => {
    const dosya = join(KOK, 'views', 'HizliSatis.vue')
    const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
    expect(icerik).toMatch(/stokStore\.getAll\(\{\s*size:\s*200\s*\}\)/)
  })
})

// ---------------------------------------------------------------------------
describe('REDTEAM Faz2.5 - Katman z-index merdiveni', () => {
  it('--z-* jetonlari tanimli ve SIRALI olmali', () => {
    const appCss = yorumsuz(readFileSync(join(KOK, 'assets', 'app.css'), 'utf8'))
    const jetonlar = {}
    const desen = /--z-([a-z]+):\s*(\d+);/g
    let m
    while ((m = desen.exec(appCss)) !== null) jetonlar[m[1]] = Number(m[2])

    for (const gerekli of ['sticky', 'sidebar', 'overlay', 'menu', 'modal', 'tooltip']) {
      expect(jetonlar[gerekli], `--z-${gerekli} tanımlı olmalı`).toBeTypeOf('number')
    }
    // Sıralama: sticky < sidebar < overlay < menu < modal < tooltip
    expect(jetonlar.sticky, 'sticky < sidebar').toBeLessThan(jetonlar.sidebar)
    expect(jetonlar.sidebar, 'sidebar < overlay').toBeLessThan(jetonlar.overlay)
    expect(jetonlar.overlay, 'overlay < menu').toBeLessThan(jetonlar.menu)
    expect(jetonlar.menu, 'menu < modal').toBeLessThan(jetonlar.modal)
    expect(jetonlar.modal, 'modal < tooltip').toBeLessThan(jetonlar.tooltip)
  })

  it('PrimeVue zIndex tabanlari app.css jetonlariyla ayni olmali', () => {
    const mainJs = yorumsuz(readFileSync(join(KOK, 'main.js'), 'utf8'))
    const appCss = yorumsuz(readFileSync(join(KOK, 'assets', 'app.css'), 'utf8'))
    const jeton = {}
    const desen = /--z-([a-z]+):\s*(\d+);/g
    let m
    while ((m = desen.exec(appCss)) !== null) jeton[m[1]] = Number(m[2])

    const eslesmeler = [
      ['overlay', /overlay:\s*(\d+)/],
      ['menu', /menu:\s*(\d+)/],
      ['modal', /modal:\s*(\d+)/],
      ['tooltip', /tooltip:\s*(\d+)/]
    ]
    for (const [ad, rx] of eslesmeler) {
      const eslesme = mainJs.match(rx)
      expect(eslesme, `main.js içinde ${ad} tanımlı olmalı`).not.toBeNull()
      expect(
        Number(eslesme[1]),
        `main.js ${ad} (${eslesme[1]}) ile app.css --z-${ad} (${jeton[ad]}) eşit olmalı`
      ).toBe(jeton[ad])
    }
  })

  it('mobil offline banner overlay tabaninin ustunde olmamali', () => {
    const appVue = yorumsuz(readFileSync(join(KOK, 'App.vue'), 'utf8'))
    expect(
      appVue,
      'Mobil .offline-banner z-index: 1001 idi ve PrimeVue overlay (varsayılan 1000) ' +
      'üstündeydi → POS ürün arama sonuçları görünmüyordu. Jeton kullanılmalı.'
    ).not.toMatch(/z-index:\s*1001/)
    // CSS kuralini `{` ile sabitle: `class="offline-banner"` (sablon) da
    // `.offline-banner` ile eslesiyor ve z-index'ten cok uzakta.
    expect(appVue).toMatch(
      /\.offline-banner\s*\{[^}]*z-index:\s*var\(--z-sticky\)/
    )
  })

  it('z-index 1200 gibi PrimeVue overlay tabanina cakisan sabit deger kalmasin', () => {
    // 1200 = overlay tabani. Bir CSS kurali tam 1200 kullanirsa
    // overlay ile esit olur ve DOM sirasina bagli kalir.
    const ihlaller = []
    for (const dosya of [...vueDosyalari(), ...CSS_DOSYALARI]) {
      const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
      icerik.split(/\r?\n/).forEach((satir, i) => {
        const m = satir.match(/z-index:\s*(1100|1200|1300)\s*;/)
        if (m) {
          ihlaller.push(
            `${relative(process.cwd(), dosya)}:${i + 1}  z-index: ${m[1]}  ` +
            '→ PrimeVue katman tabanıyla çakışır; var(--z-*) kullanın'
          )
        }
      })
    }
    expect(ihlaller, 'PrimeVue tabanıyla çakışan z-index bulundu:\n' + ihlaller.join('\n')).toEqual([])
  })
})
