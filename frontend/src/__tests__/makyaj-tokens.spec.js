import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * “Mevcut kimliği incelt” makyaj sözleşmesi:
 * token katmanı, ortak görsel sınıflar, boş durum illüstrasyonları ve
 * hedef ekranlarda token dışı renk bırakılmaması.
 *
 * Kapsam dışı (bilinçli yerel paletler): giriş ekranı, durum sayfası,
 * tema seçici renk seçenekleri ve yazdırma/kağıt önizleme blokları.
 */

const SRC = join(process.cwd(), 'src')
const oku = (...yol) => readFileSync(join(SRC, ...yol), 'utf8')

const appCss = oku('assets/app.css')
const uiKit = oku('assets/ui-kit.css')
const main = oku('main.js')
const emptyState = oku('components/EmptyState.vue')

const TOKENLAR = [
  '--space-1',
  '--space-2',
  '--space-3',
  '--space-4',
  '--space-5',
  '--space-6',
  '--shadow-kart',
  '--shadow-yukari',
  '--font-2xl',
  '--font-3xl',
  '--focus-ring'
]

const ORTAK_SINIFLAR = [
  '.rp-kart',
  '.rp-rozet',
  '.rp-rozet--ok',
  '.rp-rozet--uyari',
  '.rp-rozet--hata',
  '.rp-rozet--notr',
  '.rp-sayi',
  '.rp-baslik-2',
  '.rp-baslik-3',
  '.rp-gecis'
]

const HEDEF_DOSYALAR = [
  'views/Tahsilat.vue',
  'views/Raporlar.vue',
  'views/CekSenet.vue',
  'views/Teklifler.vue',
  'views/Muhasebe.vue',
  'views/Ajanda.vue',
  'views/Anomaliler.vue',
  'views/KarlilikAnalizi.vue',
  'components/QuickSearch.vue',
  'components/StokDetayDialog.vue'
]

const stilBlogu = (kaynak) => (kaynak.match(/<style[\s\S]*?<\/style>/g) || []).join('\n')

/** `var(--token, #hex)` fallback'leri kural dışıdır; kağıt/print blokları hariç. */
const fallbackleriTemizle = (css) =>
  css
    .replace(/var\(--[^)]*#[0-9a-fA-F]{3,8}[^)]*\)/g, '')
    .replace(/\.teklif-mektup-kagit\s*\{[^}]*\}/g, '')

const YASAK_RENK =
  /#(?:ef4444|f87171|dc2626|991b1b|b91c1c|10b981|22c55e|34d399|4ade80|5eead4|f59e0b|fbbf24|b45309|e65100|3b82f6|38bdf8|8b5cf6|f97316|06b6d4|a3e635|64748b|475569|94a3b8|cbd5e1|e2e8f0|1e293b|f1f5f9|f8fafc|0f172a|1f2937|fff3e0|fce4ec|880e4f)\b/gi

describe('Makyaj — token katmanı', () => {
  it('boşluk, gölge, başlık ve odak tokenları tanımlı', () => {
    for (const token of TOKENLAR) {
      expect(appCss, token).toContain(`${token}:`)
    }
  })

  it('açık temada da gölge/odak tokenları yeniden tanımlanır', () => {
    const acikTema = appCss.slice(appCss.search(/\[data-theme=["']light["']\]/))
    expect(acikTema).toContain('--shadow-kart:')
    expect(acikTema).toContain('--focus-ring:')
  })

  it('hareket azaltma tercihi global olarak desteklenir', () => {
    expect(appCss).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*transition:\s*none/)
    expect(appCss).toMatch(/@media \(prefers-reduced-motion: reduce\)[\s\S]*animation:\s*none/)
  })
})

describe('Makyaj — ortak görsel sınıflar', () => {
  it('ui-kit.css beklenen sınıfları tanımlar', () => {
    for (const sinif of ORTAK_SINIFLAR) {
      expect(uiKit, sinif).toContain(`${sinif} `)
    }
  })

  it('ui-kit.css main.js içinde yüklenir', () => {
    expect(main).toMatch(/assets\/ui-kit\.css/)
  })

  it('ortak sınıflar yalnız token kullanır', () => {
    expect(uiKit).not.toMatch(YASAK_RENK)
  })
})

describe('Makyaj — boş durum illüstrasyonları', () => {
  it('EmptyState opsiyonel illüstrasyon sunar, mevcut ikon davranışı korunur', () => {
    expect(emptyState).toMatch(/illustrasyon:/)
    for (const tur of ['liste', 'depo', 'arama', 'rapor']) {
      expect(emptyState, tur).toContain(`'${tur}'`)
    }
    expect(emptyState).toContain('empty-state-icon')
  })
})

describe('Makyaj — hedef ekranlarda token dışı renk kalmadı', () => {
  for (const dosya of HEDEF_DOSYALAR) {
    it(`${dosya} stil bloğunda eski renk literalleri yok`, () => {
      const temiz = fallbackleriTemizle(stilBlogu(oku(dosya)))
      const kalan = temiz.match(YASAK_RENK) || []
      expect(kalan, `${dosya}: ${kalan.join(', ')}`).toEqual([])
    })
  }
})
