import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz7 — Mobil/dokunma ve olü secici temizligi (Hizli Satis).
 *
 * KUSURLAR:
 *  1) Cekmece yalnizca perdeye dokunarak kapaniyordu; Esc calismiyordu ve
 *     kapali cekmece DOM'da kalip klavye/ekran okuyucu tarafindan geziliyordu.
 *  2) Parmakla kullanimda 30-36px dugmeler (satir sil, adet +/-) iskalaniyordu.
 *  3) `odaklaAktifAdet` OLU bir sinifa odaklaniyordu: `.sepet-adet-input`
 *     hicbir zaman render edilmiyor (gercek sinif `.adet-girdi`) — F4 kisayolu
 *     sessizce hicbir sey yapmiyordu.
 *  4) Fiyat/stok siralamasi tek yonluydu; en ucuz urunu gormek imkansizdi.
 *  5) Kamera modalinde Turkce literal placeholder/buton vardi (i18n disi).
 */

const KOK = join(process.cwd(), 'src')
const posKod = readFileSync(join(KOK, 'views/HizliSatis.vue'), 'utf8')
const posCss = readFileSync(join(KOK, 'assets', 'pos-hizli-satis.css'), 'utf8')
const panelCss = readFileSync(join(KOK, 'assets', 'pos-panels.css'), 'utf8')
const tarayiciKod = readFileSync(join(KOK, 'components', 'BarcodeScannerModal.vue'), 'utf8')
const tr = JSON.parse(readFileSync(join(KOK, 'locales', 'tr.json'), 'utf8'))
const en = JSON.parse(readFileSync(join(KOK, 'locales', 'en.json'), 'utf8'))

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

describe('Faz7.1 - mobil cekmece kapatma ve erisilebilirlik', () => {
  it('cekmece kapaliyken inert + aria-hidden ile gizlenir', () => {
    const sablon = yorumsuz(posKod)
    expect(sablon).toContain(':inert="darEkran && !sepetDrawerAcik"')
    expect(sablon).toContain(':aria-hidden="darEkran && !sepetDrawerAcik ? \'true\' : undefined"')
  })

  it('cekmecede gorunur kapatma dugmesi var', () => {
    const sablon = yorumsuz(posKod)
    expect(sablon).toContain('class="sepet-drawer-kapat"')
    expect(sablon).toContain('@click="sepetDrawerAcik = false"')
  })

  it('kapatma dugmesi stilalri ve FAB safe-area boslugu tanimli', () => {
    expect(posCss).toMatch(/\.sepet-drawer-kapat \{[^}]*width: 44px/)
    expect(posCss).toContain('env(safe-area-inset-bottom, 0px)')
    // Kapatma dugmesi kart basligini ortmesin diye cekmeceye ust bosluk verilir.
    expect(posCss).toMatch(/\.pos-right\.acik \{[^}]*padding-top: 52px/)
  })

  it('Esc cekmeceyi kapatir (dialog yoksa)', () => {
    const s = scriptKodu(posKod)
    const iptal = s.slice(s.indexOf('iptal: () =>'), s.indexOf('yazdir:'))
    expect(iptal).toContain('sepetDrawerAcik.value = false')
  })

  it('ekran esigi matchMedia ile izlenir ve temizlenir', () => {
    const s = scriptKodu(posKod)
    expect(s).toContain("window.matchMedia('(max-width: 1280px)')")
    expect(s).toContain("addEventListener?.('change', darEkranDegisti)")
    expect(s).toContain("removeEventListener?.('change', darEkranDegisti)")
  })
})

describe('Faz7.2 - dokunma hedefleri', () => {
  it('kaba isaretcide 44px hedefler uygulanir', () => {
    expect(panelCss).toContain('@media (pointer: coarse)')
    const blok = panelCss.slice(panelCss.indexOf('@media (pointer: coarse)'))
    expect(blok).toContain('.pos-container .sepet-sil')
    expect(blok).toContain('width: 44px')
    expect(blok).toContain('min-height: 44px')
    expect(blok).toMatch(/\.pos-container \.adet-btn \{[^}]*min-height: 44px/)
  })
})

describe('Faz7.3 - olu odak sinifi temizligi', () => {
  it('odak dogru sinifa gider', () => {
    const s = scriptKodu(posKod)
    expect(s).toContain(".sepet-item.aktif-satir .adet-girdi")
    expect(s).not.toContain('sepet-adet-input')
  })

  it('CSS ve kaynakta olu sinif kalmadi', () => {
    expect(panelCss).not.toContain('sepet-adet-input')
    expect(panelCss).not.toContain('sepet-adet-grup')
    // Buyuk yazi modu artik gercek sinifi buyutur.
    expect(panelCss).toMatch(/pos-buyuk \.adet-girdi/)
  })
})

describe('Faz7.4 - siralama iki yonlu', () => {
  it('fiyat ve stok icin artan/azalan secenekleri var', () => {
    const s = scriptKodu(posKod)
    for (const deger of ['fiyat-artan', 'fiyat-azalan', 'stok-cok', 'stok-az']) {
      expect(s).toContain(`'${deger}'`)
    }
  })

  it('computed her yonu uygular', () => {
    const s = scriptKodu(posKod)
    const blok = s.slice(s.indexOf('const gorunenUrunler = computed'), s.indexOf('// Görünen ürünler'))
    expect(blok).toContain('satisFiyati(a) - satisFiyati(b)')
    expect(blok).toContain('satisFiyati(b) - satisFiyati(a)')
    expect(blok).toContain('(a.miktar || 0) - (b.miktar || 0)')
    expect(blok).toContain('(b.miktar || 0) - (a.miktar || 0)')
  })

  it('yeni siralama anahtarlari iki dilde de var', () => {
    for (const anahtar of ['siralaFiyatArtan', 'siralaFiyatAzalan', 'siralaStokCok', 'siralaStokAz']) {
      expect(tr.hizliSatis[anahtar]).toBeTruthy()
      expect(en.hizliSatis[anahtar]).toBeTruthy()
    }
  })
})

describe('Faz7.5 - kamera modali i18n', () => {
  it('Turkce literal placeholder/buton kalmadi', () => {
    expect(tarayiciKod).not.toContain('placeholder="Barkod No..."')
    expect(tarayiciKod).not.toContain('label="Ekle"')
    expect(tarayiciKod).toContain("$t('cmp.barkodNoPlaceholder')")
    expect(tarayiciKod).toContain("$t('cmp.ekle')")
  })

  it('kamera metinleri iki dilde tanimli', () => {
    expect(tr.cmp.barkodNoPlaceholder).toBeTruthy()
    expect(tr.cmp.ekle).toBeTruthy()
    expect(en.cmp.barkodNoPlaceholder).toBeTruthy()
    expect(en.cmp.ekle).toBeTruthy()
  })
})
