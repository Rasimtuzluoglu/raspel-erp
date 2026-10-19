import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Dönemler revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (skeleton + mobil düzen + a11y + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const view = oku('views/Donemler.vue')

describe('Donemler — Faz 4', () => {
  it('skeleton yükleniyor durumu', () => {
    expect(view).toMatch(/import SkeletonLoader from '\.\.\/components\/SkeletonLoader\.vue'/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/role="status"/)
  })

  it('mobil düzen (dönem ızgarası + kapanış geçmişi)', () => {
    expect(view).toMatch(/@media \(max-width: 640px\)/)
    expect(view).toMatch(/\.kapanis-baslik-satir \{\s*display: none;/)
    expect(view).toMatch(/\.kapanis-satir \{\s*grid-template-columns: 1fr 1fr;/)
  })

  it('a11y: çip grubu + aria-pressed', () => {
    expect(view).toMatch(/role="group"/)
    expect(view).toMatch(/:aria-pressed="durumFiltresi === c\.deger"/)
  })

  it('reduced-motion desteği', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })
})
