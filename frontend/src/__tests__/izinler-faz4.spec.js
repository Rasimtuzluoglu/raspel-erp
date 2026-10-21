import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * İzinler revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kartlar + skeleton + erişilebilirlik + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Izinler.vue')
const drawer = oku('components/IzinDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('İzinler — Faz 4', () => {
  it('mobil kart görünümü + masaüstü gizleme', () => {
    expect(view).toMatch(/class="izin-mobil"/)
    expect(view).toMatch(/class="izin-mobil-kart"/)
    expect(view).toMatch(/class="izin-masa"/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
    expect(view).toMatch(/\.izin-masa\s*\{\s*display: none;/)
    expect(view).toMatch(/\.izin-mobil\s*\{\s*display: none;/)
  })

  it('skeleton yükleme durumu', () => {
    expect(view).toMatch(/import SkeletonLoader/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/v-if="yukleniyor && !tumIzinler\.length"/)
  })

  it('erişilebilirlik: aria-busy, canlı bölge', () => {
    expect(view).toMatch(/:aria-busy="yukleniyor"/)
    expect(view).toMatch(/role="status"/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/class="sr-only"/)
    expect(drawer).toMatch(/role="status"/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })

  it('sonuç sayısı i18n anahtarı tr ve en için tam', () => {
    expect(tr).toMatch(/"sonucSayisi"/)
    expect(en).toMatch(/"sonucSayisi"/)
  })
})
