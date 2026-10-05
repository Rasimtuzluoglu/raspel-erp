import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'

/**
 * REDTEAM/Faz3 regresyon testleri — kaynak-kodu tarayan denetimler.
 *
 * Bu testler bilesen MOUNT ETMEZ; .vue dosyalarini okuyup kural ihlali tespit
 * eder. Uc hata turu Turkce'de "hata" gibi gorunmez, ekranda da cok supheli
 * durmaz ama sonucu bozuk olur:
 *
 *  1) Etiket disi metin dugumu: `v-permission` bir DIEREKSIYON olmadan
 *     duz metin olarak yazilirsa Vue derleyicisi onu literal metin dugumu
 *     olarak derler ve EKRANA BASAR. Derleyici HICBIR uyari vermez. Test
 *     hicbir sey gormez, ekranda metin gorunur.
 *  2) Yanlis yetki kodu: frontend bir yetki kodu arar, backend baska bir
 *     kodu (veya `hasRole`) kontrol eder. Buton yanlis rolde gorunur ve
 *     403 alir; dogru rolde gizli kalir.
 *  3) PrimeVue 3 kalinti sinifi: PrimeVue 4'te `p-input-icon-left` /
 *     `p-input-icon-right` KALDIRILDI. Sinif hala yaziliyorsa yerlestirme
 *     kurali hic uretilmiyor -> ikon input'un uzerine biniyor.
 */

const KOK = join(process.cwd(), 'src')

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
 * Yorum satirlarini temizler. Tarayici KODU denetlemelidir, metni degil:
 * duzeltmelerin kendi yorumlarinda kurali ANLATAN ifadeler tarayiciyi
 * yaniltirdi (Faz2'de bu yuzden eklendi).
 *
 * REDTEAM/Faz3 notu: once BILDIRIYE GORE, sonra satir satir temizlik vardi.
 * Bu cok satirli HTML yorumlarini (`<!-- ... -->`) TEMIZLEMEDI; Faz3'te
 * eklenen aciklama yorumlari icindeki `v-if=` satirlari ihlal sanildi.
 * Yorumlar icerik genelinde temizlenir, satir sayisi korunur.
 */
function yorumsuz(icerik) {
  const bosluk = (m) => m.replace(/[^\n]/g, ' ')
  return icerik
    .replace(/<!--[\s\S]*?-->/g, bosluk)
    .replace(/\/\*[\s\S]*?\*\//g, bosluk)
    .split(/\r?\n/)
    .map((satir) => satir.replace(/(^|\s)\/\/.*$/, bosluk))
    .join('\n')
}

/** Yalniz <template> bloklarini dondurur (JS string'lerini tarama). */
function sablonBlogu(kod) {
  const bas = kod.indexOf('<template>')
  const son = kod.lastIndexOf('</template>')
  if (bas === -1 || son === -1 || son <= bas) return ''
  return kod.slice(bas, son)
}

/**
 * Etiket disinda kalmis yonerge METIN DUGUMLERI bulur.
 *
 * REDTEAM notu: Ilk denemede denetim cok genisti ve 1655 ihlal bildirdi.
 * Sebep: su kalsip DOGRU bir oz nitelik, yanlis metin dugumu sanildi:
 *
 *     <Button
 *       v-if="x"          <-- etiket ACIK, bu bir OZNITELIK
 *       icon="pi"
 *     />
 *
 * Ayirici etiket ici/ disi durumu (`etikette` bayragi), oznitelik degerleri
 * icindeki `>` karakterleri ve yorumlar hesaba katilir.
 *
 * @returns {{satir:number, metin:string}[]}
 */
function metinDugumleri(templateKodu) {
  const bulunan = []
  const satirlar = templateKodu.split(/\r?\n/)
  let etikette = false
  let tirnakta = null

  for (let i = 0; i < satirlar.length; i++) {
    const satir = satirlar[i]
    let metin = satir

    for (let c = 0; c < metin.length; c++) {
      const ch = metin[c]

      if (tirnakta) {
        if (ch === tirnakta) tirnakta = null
        continue
      }
      if (ch === '"' || ch === "'" || ch === '`') {
        tirnakta = ch
        continue
      }
      if (ch === '<') {
        etikette = true
        continue
      }
      if (ch === '>') {
        // Etiket kapandi. Bu noktadan sonra gelen duz metin bir METIN DUGUMU.
        etikette = false
        continue
      }
    }

    const kalan = satir.trim()
    // Etiket KAPALIYKEN (veya bu satirda hic etiket yokken) kalan duz metin
    // `v-...` ile basliyorsa, DIEREKSIYONUN isaretleri atlanmis demektir ->
    // Vue bunu literal metin dugumu derleyip EKRANA BASAR.
    // Etiket hala ACIKSA (`etikette === true`) bu bir OZNITELIK, ihlal degil.
    if (!etikette && kalan && /^v-[a-z][a-zA-Z-]*\s*[=]/.test(kalan)) {
      bulunan.push({ satir: i + 1, metin: kalan })
    }
  }
  return bulunan
}

/** Etiketlerin tam listesi: acilan etiket adlari ve oz nitelik bloklari. */
function etiketOzellikleri(templateKodu) {
  const sonuc = []
  const satirlar = templateKodu.split(/\r?\n/)
  let etikette = false
  let tirnakta = null
  let mevcut = null

  for (let i = 0; i < satirlar.length; i++) {
    const satir = satirlar[i]
    for (let c = 0; c < satir.length; c++) {
      const ch = satir[c]
      if (tirnakta) {
        if (ch === tirnakta) tirnakta = null
        continue
      }
      if (ch === '"' || ch === "'" || ch === '`') {
        tirnakta = ch
        continue
      }
      if (ch === '<') {
        const kapanisMi = satir[c + 1] === '/'
        etikette = true
        const ad = (/^<\/?([A-Za-z][\w.-]*)/.exec(satir.slice(c)) || [])[1] || ''
        // Kapanis etiketi oz nitelik TASIYAMAZ; yalniz acilis etiketleri
        // kaydedilir (aksi halde "</template>" de bir kayit olurdu).
        mevcut = kapanisMi ? null : { ad, satir: i + 1, govde: satir.slice(c + 1) }
        continue
      }
      if (ch === '>') {
        if (mevcut) {
          mevcut.govde += satir.slice(0, c)
          sonuc.push(mevcut)
          mevcut = null
        }
        etikette = false
        continue
      }
    }
    // Etiket bu satirda kapanmadi -> cok satirli etiket, govdeyi biriktir
    if (etikette && mevcut) mevcut.govde += satir
  }
  return sonuc
}

// ---------------------------------------------------------------------------
describe('REDTEAM Faz3.1 - Etiket disinda kalmis yönerge metin dugumu', () => {
  /**
   * Asil denetim: template icinde, bir etiketin disinda duran metin satiri
   * `v-...` ile basliyorsa bu bir DIEREKSIYON olmadan yazilmis yonergedir.
   * Denetim her etiket satirindan sonra gelen "metin" satirini tarar.
   */
  const IHLALLER = []

  for (const dosya of vueDosyalari()) {
    const template = yorumsuz(sablonBlogu(readFileSync(dosya, 'utf8')))
    for (const { satir, metin } of metinDugumleri(template)) {
      IHLALLER.push(`${relative(process.cwd(), dosya)}:${satir}  ${metin}`)
    }
  }

  it('hicbir .vue dosyasinda etiket disinda v-* yonergesi olmamali', () => {
    expect(IHLALLER).toEqual([])
  })

  it('ayristirici cok satirlı etiketi metin dugumu SAYMAMALI', () => {
    // Bu kalsip Faz3'te gercekten var ve DOGRU: etiket acik, `v-if` bir
    // oz nitelik. Tarayici bunu ihlal bildirmemeli.
    const ornek = [
      '<template>',
      '  <Button',
      '    v-if="kosul"',
      '    icon="pi pi-trash"',
      '  />',
      '</template>'
    ].join('\n')
    expect(metinDugumleri(ornek)).toEqual([])
  })

  it('ayristirici gercek metin dugumunu YAKALAMALI (pozitif kontrol)', () => {
    // Kategoriler.vue'daki hatanin birebir minimal hali.
    const ornek = [
      '<template>',
      '  <template #body="s">',
      '    v-permission="\'STOK_DELETE\'"',
      '    <Button icon="pi pi-trash" />',
      '  </template>',
      '</template>'
    ].join('\n')
    const bulunan = metinDugumleri(ornek)
    expect(bulunan).toHaveLength(1)
    expect(bulunan[0].metin).toContain('v-permission')
    expect(bulunan[0].satir).toBe(3)
  })

  it('oz nitelik degeri icindeki > karakteri ayristiriciyi bozmamali', () => {
    const ornek = [
      '<template>',
      '  <Button :title="a > b ? x : y" />',
      '  <Column v-if="z" />',
      '</template>'
    ].join('\n')
    expect(metinDugumleri(ornek)).toEqual([])
  })
})

// ---------------------------------------------------------------------------
describe('REDTEAM Faz3.2 - v-permission kodu backend ile uyumlu olmali', () => {
  /**
   * Backend'de silme islemi ADMIN-only (hasRole('ADMIN')) ise, frontend'de
   * o islem icin STOK_DELETE / CARI_DELETE gibi bir yetki KODU kullanilmamali.
   * Aksi halde kod menzil disi rolde butonu gosterir ve 403 alir.
   */
  const ADMIN_ONLY_ILISKI = [
    { dosya: 'Depolar.vue', kod: 'STOK_DELETE', gerekce: 'DepoController:63 hasRole(ADMIN)' },
    { dosya: 'CariHesaplar.vue', kod: 'CARI_DELETE', gerekce: 'CariHesapController:160 toplu-sil hasRole(ADMIN)' },
    { dosya: 'Kategoriler.vue', kod: 'STOK_DELETE', gerekce: 'KategoriController:57 hasRole(ADMIN)' },
    { dosya: 'Projeler.vue', kod: 'SISTEM_DELETE', gerekce: 'ProjeController:76 hasRole(ADMIN)' },
    { dosya: 'Personel.vue', kod: 'IK_DELETE', gerekce: 'PersonelController:67 hasRole(ADMIN)' },
    { dosya: 'Izinler.vue', kod: 'IK_DELETE', gerekce: 'PersonelIzinController:72 hasRole(ADMIN)' }
  ]

  it('ADMIN-only uçlarda yanlis yetki kodu kullanilmamali', () => {
    const ihlaller = []
    for (const kural of ADMIN_ONLY_ILISKI) {
      const dosya = vueDosyalari().find((d) => d.endsWith(kural.dosya))
      if (!dosya) {
        ihlaller.push(`dosya bulunamadi: ${kural.dosya}`)
        continue
      }
      const icerik = yorumsuz(sablonBlogu(readFileSync(dosya, 'utf8')))
      const desen = kural.kod
      icerik.split(/\r?\n/).forEach((satir, i) => {
        if (satir.includes(`v-permission="'${desen}'"`)) {
          ihlaller.push(
            `${relative(process.cwd(), dosya)}:${i + 1}  ${kural.kod} -> ${kural.gerekce}`
          )
        }
      })
    }
    expect(ihlaller).toEqual([])
  })

  it('bir etikette hem v-if hem v-permission OLMAMALI (tek kontrol)', () => {
    // Iki kontrol yan yana birakildiginda biri sessizce etkisiz kalir.
    // CariHesaplar'da `v-if="isAdmin"` + `v-permission="'CARI_DELETE'"`
    // vardi; dogru olan `v-if` idi, yanlisi silindi.
    const ihlaller = []
    for (const dosya of vueDosyalari()) {
      const template = yorumsuz(sablonBlogu(readFileSync(dosya, 'utf8')))
      for (const et of etiketOzellikleri(template)) {
        if (!/v-permission=/.test(et.govde)) continue
        if (/\bv-if=/.test(et.govde)) {
          ihlaller.push(
            `${relative(process.cwd(), dosya)}:${et.satir}  <${et.ad}> ${et.govde.trim()}`
          )
        }
      }
    }
    expect(ihlaller).toEqual([])
  })

  it('ayristirici cok satirlı etikette v-permission + v-if yakalamali', () => {
    const ornek = [
      '<template>',
      '  <Button',
      '    v-if="isAdmin"',
      '    v-permission="\'CARI_DELETE\'"',
      '    label="Sil"',
      '  />',
      '</template>'
    ].join('\n')
    const etiketler = etiketOzellikleri(ornek)
    // Kapanis etiketleri (`</template>`) oz nitelik tasimaz ve kaydedilmez.
    expect(etiketler.map((e) => e.ad)).toEqual(['template', 'Button'])
    const button = etiketler.find((e) => e.ad === 'Button')
    expect(button.govde).toContain('v-if')
    expect(button.govde).toContain('v-permission')
    expect(button.govde).toContain('label="Sil"')
  })

  it('ayristirici kapanis etiketlerini kaydetmemeli', () => {
    const ornek = ['<template>', '  <Column v-if="a" />', '</template>'].join('\n')
    expect(etiketOzellikleri(ornek).map((e) => e.ad)).toEqual(['template', 'Column'])
  })
})

// ---------------------------------------------------------------------------
describe('REDTEAM Faz3.3 - PrimeVue 4: p-input-icon-left kaldirildi', () => {
  const KALINTI_SINIFLAR = ['p-input-icon-left', 'p-input-icon-right']

  it('kaldirilmis PrimeVue 3 ikon siniflari hicbir yerde kullanilmamali', () => {
    const ihlaller = []
    for (const dosya of vueDosyalari()) {
      const icerik = yorumsuz(readFileSync(dosya, 'utf8'))
      icerik.split(/\r?\n/).forEach((satir, i) => {
        for (const sinif of KALINTI_SINIFLAR) {
          if (satir.includes(sinif)) {
            ihlaller.push(
              `${relative(process.cwd(), dosya)}:${i + 1}  ${sinif}  ${satir.trim()}`
            )
          }
        }
      })
    }
    expect(ihlaller).toEqual([])
  })

  it('dönüştürülen dosyalar IconField + InputIcon kullaniyor olmali', () => {
    const donusen = [
      'components/TeklifListesi.vue',
      'views/AdresDefteri.vue',
      'views/CariHesaplar.vue',
      'views/FaturaGecmisRaporu.vue',
      'views/Faturalar.vue',
      'views/GizlilikPolitikasi.vue',
      'views/HizliSatis.vue',
      'views/KullanimSartlari.vue',
      'views/Satis.vue',
      'views/Stoklar.vue'
    ]
    const eksikler = []
    for (const hedef of donusen) {
      const dosya = vueDosyalari().find((d) => d.replace(/\\/g, '/').endsWith(hedef))
      if (!dosya) {
        eksikler.push(`dosya bulunamadi: ${hedef}`)
        continue
      }
      const kod = yorumsuz(sablonBlogu(readFileSync(dosya, 'utf8')))
      const acilan = (kod.match(/<IconField/g) || []).length
      const kapanan = (kod.match(/<\/IconField>/g) || []).length
      if (acilan === 0) {
        eksikler.push(`${hedef}: IconField yok`)
      } else if (acilan !== kapanan) {
        eksikler.push(`${hedef}: IconField acilan ${acilan} != kapanan ${kapanan}`)
      }
      if (!/<InputIcon/.test(kod)) {
        eksikler.push(`${hedef}: InputIcon yok`)
      }
    }
    expect(eksikler).toEqual([])
  })

  it('Stoklar dogrulanmis .p-iconfield seçicisini kullaniyor', () => {
    const dosya = vueDosyalari().find((d) => d.endsWith('Stoklar.vue'))
    const kod = yorumsuz(readFileSync(dosya, 'utf8'))
    expect(kod).toMatch(/\.filter-bar > \.p-iconfield\b/)
  })
})