import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Puantaj revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kartlar + skeleton + erişilebilirlik + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Puantaj.vue')
const drawer = oku('components/PuantajDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Puantaj — Faz 4', () => {
  it('mobil kart görünümü + masaüstü gizleme', () => {
    expect(view).toMatch(/class="puantaj-mobil"/)
    expect(view).toMatch(/class="puantaj-mobil-kart"/)
    expect(view).toMatch(/class="puantaj-masa"/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
    expect(view).toMatch(/\.puantaj-masa\s*\{\s*display: none;/)
    expect(view).toMatch(/\.puantaj-mobil\s*\{\s*display: none;/)
  })

  it('skeleton yükleme durumu', () => {
    expect(view).toMatch(/import SkeletonLoader/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/v-if="yukleniyor && !list\.length"/)
  })

  it('erişilebilirlik: aria-busy, canlı bölge', () => {
    expect(view).toMatch(/:aria-busy="yukleniyor"/)
    expect(view).toMatch(/role="status"/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/class="sr-only"/)
    expect(drawer).toMatch(/durumEtiket/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })

  it('sonuç sayısı i18n anahtarı tr ve en için tam', () => {
    expect(tr).toMatch(/"sonucSayisi"/)
    expect(en).toMatch(/"sonucSayisi"/)
  })
})
