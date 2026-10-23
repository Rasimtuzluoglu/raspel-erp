import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Taksit Takvimi revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kartlar + kompakt takvim + skeleton + a11y + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/TaksitTakvimi.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Taksit Takvimi — Faz 4', () => {
  it('mobil kartlar + masaüstü gizleme', () => {
    expect(view).toMatch(/class="taksit-mobil"/)
    expect(view).toMatch(/class="taksit-mobil-kart"/)
    expect(view).toMatch(/class="taksit-masa"/)
    expect(view).toMatch(/class="plan-mobil"/)
    expect(view).toMatch(/class="plan-mobil-kart"/)
    expect(view).toMatch(/class="plan-masa"/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
    expect(view).toMatch(/\.taksit-masa,\s*\.plan-masa\s*\{\s*display: none;/)
  })

  it('kompakt takvim (mobilde nokta)', () => {
    expect(view).toMatch(/class="kalem-metin"/)
    expect(view).toMatch(/\.kalem-metin\s*\{\s*display: none;/)
    expect(view).toMatch(/role="grid"/)
  })

  it('skeleton yükleme durumu', () => {
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/v-if="listeYukleniyor && !liste\.length"/)
    expect(view).toMatch(/v-if="planYukleniyor && !planlar\.length"/)
  })

  it('erişilebilirlik: aria-busy, canlı bölge, ay duyurusu', () => {
    expect(view).toMatch(/:aria-busy="listeYukleniyor"/)
    expect(view).toMatch(/:aria-busy="planYukleniyor"/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/class="sr-only"/)
    expect(view).toMatch(/:aria-label="ayAdi"/)
  })

  it('reduced-motion + i18n', () => {
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
    expect(tr).toMatch(/"sonucSayisi"/)
    expect(en).toMatch(/"sonucSayisi"/)
  })
})
