import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * 360° Görünümler revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kart görünümü + skeleton + a11y + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const view = oku('views/Gorunumler360.vue')

describe('Gorunumler360 — Faz 4', () => {
  it('skeleton loader tablolarda ve çalışan kartlarında', () => {
    expect(view).toMatch(/import SkeletonLoader from '\.\.\/components\/SkeletonLoader\.vue'/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect((view.match(/<SkeletonLoader/g) || []).length).toBeGreaterThanOrEqual(3)
  })

  it('mobil kart görünümleri + masa/mobil geçişi', () => {
    expect(view).toMatch(/class="stok-mobil"/)
    expect(view).toMatch(/class="segment-mobil"/)
    expect(view).toMatch(/mobil-kart/)
    expect(view).toMatch(/stok-masa/)
    expect(view).toMatch(/segment-masa/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
  })

  it('erişilebilirlik: aria-pressed + klavye + empty aksiyonu', () => {
    expect(view).toMatch(/:aria-pressed="sadeceNegatif"/)
    expect(view).toMatch(/@keydown\.enter\.prevent="urunGit/)
    expect(view).toMatch(/@keydown\.enter\.prevent="musteriGit/)
    expect(view).toMatch(/:action-label="stokFiltreVar \? t\('gorunum360\.temizle'\)/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })
})
