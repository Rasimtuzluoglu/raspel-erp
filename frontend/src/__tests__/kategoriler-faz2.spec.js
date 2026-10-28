import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Kategoriler revizyonu Faz 2 — i18n simetrisi, Kasa dropdown'u, yetki ve a11y.
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const tr = JSON.parse(readFileSync(join(SRC, 'locales/tr.json'), 'utf8'))
const en = JSON.parse(readFileSync(join(SRC, 'locales/en.json'), 'utf8'))

const kasa = oku('views/Kasa.vue')
const view = oku('views/Kategoriler.vue')
const drawer = oku('components/KategoriKullanimDrawer.vue')

describe('i18n simetrisi', () => {
  it('kategoriler blogu tr/en ayni anahtar kumesine sahip', () => {
    const trKeys = Object.keys(tr.kategoriler).sort()
    const enKeys = Object.keys(en.kategoriler).sort()
    expect(enKeys).toEqual(trKeys)
  })

  it('yeni revizyon anahtarlari iki dilde de var', () => {
    const beklenen = [
      'aciklama', 'kapsamKasa', 'kapsamStok', 'kapsamMasraf', 'kapsamButce',
      'kpiToplam', 'kpiAktif', 'kpiPasif', 'kpiAltKategori',
      'aramaPlaceholder', 'filtreTumu', 'filtreAktif', 'filtrePasif',
      'siralaSiraya', 'siralaAdArtan', 'siralaAdAzalan', 'siralaKullanim',
      'kullanimBaslik', 'kullanimToplam', 'kullanimDetay', 'kullanimYok',
      'modulStok', 'modulKasa', 'modulMasraf', 'modulButce', 'modulIskonto',
      'birlestirBaslik', 'birlestirAciklama', 'birlestirUyari', 'birlestirildi',
      'arsivle', 'aktiflestir', 'adminGerekli',
      'silKullanimEngel', 'silAltKategoriEngel',
      'sistemKategorileri', 'sistemSaltOkunur',
      'renk', 'ikon', 'sira', 'onizleme', 'ustKategori', 'adZorunlu'
    ]
    for (const anahtar of beklenen) {
      expect(tr.kategoriler[anahtar], `tr.${anahtar}`).toBeTruthy()
      expect(en.kategoriler[anahtar], `en.${anahtar}`).toBeTruthy()
    }
  })

  it('mevcut anahtarlar korunur', () => {
    for (const anahtar of ['title', 'yeniKategori', 'kategoriAdi', 'gelir', 'gider', 'empty', 'emptyHint', 'kategoriSilindi']) {
      expect(tr.kategoriler[anahtar]).toBeTruthy()
      expect(en.kategoriler[anahtar]).toBeTruthy()
    }
  })

  it('vue-i18n sozdizimi riski tasimaz', () => {
    for (const [dil, blok] of [['tr', tr.kategoriler], ['en', en.kategoriler]]) {
      for (const [anahtar, deger] of Object.entries(blok)) {
        if (typeof deger !== 'string') continue
        expect(deger, `${dil}.${anahtar} @`).not.toContain('@')
        expect(deger, `${dil}.${anahtar} |`).not.toContain('|')
      }
    }
  })
})

describe('Kasa kategori dropdown', () => {
  it('secenekler ucu kapsam ile cagrilir ve yalniz aktifler beslenir', () => {
    expect(kasa).toContain('kategoriStore.seceneklerGetir')
    expect(kasa).toContain("'KASA_GELIR'")
    expect(kasa).toContain("'KASA_GIDER'")
    expect(kasa).toMatch(/k\.aktif !== false/)
  })

  it('ust kategori basliklariyla gruplu dropdown kullanilir', () => {
    expect(kasa).toContain('option-group-label="label"')
    expect(kasa).toContain('option-group-children="items"')
    expect(kasa).toContain('ustKategoriler')
  })

  it('eski getAllKategoriler cagrisi kaldirildi (sayfali yanit dropdowna sizmaz)', () => {
    expect(kasa).not.toContain('kategoriStore.getAllKategoriler()')
  })
})

describe('yetki ve a11y', () => {
  it('silme/arsiv/birlestirme ADMIN kontrolune bagli ve neden gosterilir', () => {
    expect(view).toMatch(/authStore\.isAdmin/)
    expect(view).toContain('adminGerekli')
    expect(view).toMatch(/devreDisi:\s*!isAdmin\.value/)
  })

  it('kapsam sekmeleri ve filtreler erisilebilir isaretlenir', () => {
    expect(view).toContain('role="tablist"')
    expect(view).toContain('role="tab"')
    expect(view).toContain(':aria-selected=')
    expect(view).toContain(':aria-pressed=')
    expect(view).toContain('aria-live="polite"')
    expect(view).toContain(':aria-expanded=')
  })

  it('mobil ve hareket azaltma kurallari vardir', () => {
    expect(view).toContain('prefers-reduced-motion')
    expect(view).toContain('92vw')
    expect(view).toContain('kategori-mobil')
  })

  it('kullanim drawer ilgili ekranlara baglanir', () => {
    for (const yol of ['/stoklar', '/kasa', '/masraflar', '/butceler']) {
      expect(drawer).toContain(yol)
    }
  })

  it('Excel/export kapsam disidir', () => {
    expect(view).not.toMatch(/excelAPI|excelIndir/)
  })
})
