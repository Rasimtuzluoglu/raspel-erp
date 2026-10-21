import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Şubeler revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kartlar + skeleton + erişilebilirlik + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Subeler.vue')
const drawer = oku('components/SubeDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Subeler — Faz 4', () => {
  it('mobil kart görünümü + masaüstü gizleme', () => {
    expect(view).toMatch(/class="sube-mobil"/)
    expect(view).toMatch(/class="sube-mobil-kart"/)
    expect(view).toMatch(/class="sube-masa"/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
    expect(view).toMatch(/\.sube-masa\s*\{\s*display: none;/)
    expect(view).toMatch(/\.sube-mobil\s*\{\s*display: none;/)
  })

  it('skeleton yükleme durumu', () => {
    expect(view).toMatch(/import SkeletonLoader/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/v-if="yukleniyor && !list\.length"/)
  })

  it('erişilebilirlik: aria-busy, canlı bölge, aria-required', () => {
    expect(view).toMatch(/:aria-busy="yukleniyor"/)
    expect(view).toMatch(/role="status"/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/aria-required="true"/)
    expect(view).toMatch(/class="sr-only"/)
    expect(view).toMatch(/:aria-pressed="filtreDurum === c\.deger"/)
    expect(drawer).toMatch(/aria-live="polite"/)
    expect(drawer).toMatch(/:aria-label="t\('subeler\.bagliDepolar'\)"/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })

  it('sonuç sayısı i18n anahtarı tr ve en için tam', () => {
    expect(tr).toMatch(/"sonucSayisi"/)
    expect(en).toMatch(/"sonucSayisi"/)
  })
})
