import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Üretim revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (sekme/filtre hafızası, iskelet yükleme, erişilebilirlik).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Uretim.vue')
const drawer = oku('components/UretimEmriDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Üretim — Faz 1', () => {
  it('son sekme ve emir filtreleri kalıcı', () => {
    expect(view).toMatch(/const SEKME_ANAHTAR = 'raspel_uretim_tab'/)
    expect(view).toMatch(/const FILTRE_ANAHTAR = 'raspel_uretim_emir_filtre'/)
    expect(view).toMatch(/ref\(Number\(localStorage\.getItem\(SEKME_ANAHTAR\)\) \|\| 0\)/)
    expect(view).toMatch(/localStorage\.setItem\(SEKME_ANAHTAR, String\(v\)\)/)
    expect(view).toMatch(/const kayitliFiltre = \(\(\) =>/)
    expect(view).toMatch(/localStorage\.setItem\(\s*FILTRE_ANAHTAR/)
    expect(view).toMatch(/const isoToDate = \(s\) =>/)
  })

  it('KPI, liste ve reçete için iskelet yükleme', () => {
    expect(view).toMatch(/const kpiIskelet = computed/)
    expect(view).toMatch(/<SkeletonLoader height="86px" \/>/)
    expect(view).toMatch(/<SkeletonLoader[\s\S]*?:count="6"[\s\S]*?height="44px"/)
    expect(view).toMatch(/<SkeletonLoader[\s\S]*?:count="5"/)
    expect(drawer).toMatch(/<SkeletonLoader[\s\S]*?:count="3"/)
  })

  it('erişilebilirlik: canlı duyuru, aria-pressed, aria-busy, etiketler', () => {
    expect(view).toMatch(/const emirDuyuru = computed/)
    expect(view).toMatch(/class="sr-only"\s+aria-live="polite"/)
    expect(view).toMatch(/:aria-pressed="aktifCip === c\.deger"/)
    expect(view).toMatch(/class="emir-cipler"\s+role="group"/)
    expect(view).toMatch(/:aria-busy="true"/)
    expect(view).toMatch(/:aria-label="t\('uretim\.aramaPlaceholder'\)"/)
    expect(view).toMatch(/:aria-label="t\('uretim\.baslangicTarihi'\)"/)
    expect(view).toMatch(/:aria-label="t\('uretim\.bitisTarihi'\)"/)
    expect(drawer).toMatch(/:breakpoints="\{ '575px': '92vw' \}"/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of ['hizliFiltre', 'emirDuyuru']) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
