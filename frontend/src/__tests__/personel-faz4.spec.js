import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Personel revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kartlar + skeleton + erişilebilirlik + tab hafızası + reduced-motion).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Personel.vue')
const drawer = oku('components/PersonelDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Personel — Faz 4', () => {
  it('mobil kart görünümü + masaüstü gizleme', () => {
    expect(view).toMatch(/class="personel-mobil"/)
    expect(view).toMatch(/class="personel-mobil-kart"/)
    expect(view).toMatch(/class="personel-masa"/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
    expect(view).toMatch(/\.personel-masa\s*\{\s*display: none;/)
    expect(view).toMatch(/\.personel-mobil\s*\{\s*display: none;/)
  })

  it('skeleton yükleme durumu', () => {
    expect(view).toMatch(/import SkeletonLoader/)
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/v-if="yukleniyor && !personeller\.length"/)
  })

  it('erişilebilirlik: aria-busy, canlı bölge, aria-required', () => {
    expect(view).toMatch(/:aria-busy="yukleniyor"/)
    expect(view).toMatch(/role="status"/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/aria-required="true"/)
    expect(view).toMatch(/class="sr-only"/)
    expect(drawer).toMatch(/role="status"/)
    expect(drawer).toMatch(/:aria-label="t\('personelDetay\.sonIzinler'\)"/)
  })

  it('tab hafızası + reduced-motion', () => {
    expect(view).toMatch(/v-model:active-index="aktifTab"/)
    expect(view).toMatch(/raspel_personel_tab/)
    expect(view).toMatch(/watch\(aktifTab/)
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })

  it('sonuç sayısı i18n anahtarı tr ve en için tam', () => {
    expect(tr).toMatch(/"sonucSayisi"/)
    expect(en).toMatch(/"sonucSayisi"/)
  })
})
