import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Anomaliler revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kart + skeleton + a11y + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const view = oku('views/Anomaliler.vue')

describe('Anomaliler — Faz 4', () => {
  it('mobil IP kartları + masa/mobil geçişi', () => {
    expect(view).toMatch(/class="ip-mobil"/)
    expect(view).toMatch(/ip-mobil-kart/)
    expect(view).toMatch(/ip-masa/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
  })

  it('skeleton + a11y', () => {
    expect(view).toMatch(/<SkeletonLoader/)
    expect((view.match(/role="status"/g) || []).length).toBeGreaterThanOrEqual(2)
    expect(view).toMatch(/:aria-label="t\('common\.loading'\)"/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })
})
