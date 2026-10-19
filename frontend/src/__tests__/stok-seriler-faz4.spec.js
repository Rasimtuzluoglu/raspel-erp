import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Seri/Lot Takibi revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kart + skeleton + a11y + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const view = oku('views/StokSeriler.vue')

describe('StokSeriler — Faz 4', () => {
  it('skeleton + a11y', () => {
    expect(view).toMatch(/import SkeletonLoader from '\.\.\/components\/SkeletonLoader\.vue'/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/role="status"/)
    expect(view).toMatch(/aria-live="polite"/)
  })

  it('mobil kart görünümü + masa/mobil geçişi', () => {
    expect(view).toMatch(/class="seri-mobil"/)
    expect(view).toMatch(/seri-mobil-kart/)
    expect(view).toMatch(/seri-masa/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })
})
