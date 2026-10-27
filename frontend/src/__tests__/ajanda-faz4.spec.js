import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Ajanda revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (mobil kartlar, kompakt takvim, erişilebilirlik, iskelet, hareket azaltma).
 */

const SRC = join(process.cwd(), 'src')
const view = readFileSync(join(SRC, 'views/Ajanda.vue'), 'utf8')
const tr = readFileSync(join(SRC, 'locales/tr.json'), 'utf8')
const en = readFileSync(join(SRC, 'locales/en.json'), 'utf8')

describe('Ajanda — Faz 4', () => {
  it('takvim ARIA grid yapısı ve klavye seçimi sunar', () => {
    expect(view).toMatch(/role="grid"/)
    expect(view).toMatch(/role="rowgroup"/)
    expect(view).toMatch(/role="row"/)
    expect(view).toMatch(/role="columnheader"/)
    expect(view).toMatch(/:role="gun \? 'gridcell' : 'presentation'"/)
    expect(view).toMatch(/:aria-selected="gun \? ayniGun\(gun, seciliGun\) : undefined"/)
    expect(view).toMatch(/@keydown\.enter\.prevent="gun && gunuSec\(gun\)"/)
    expect(view).toMatch(/@keydown\.space\.prevent="gun && gunuSec\(gun\)"/)
    expect(view).toMatch(/const haftalar = computed/)
    expect(view).toMatch(/const gunAriaEtiket = \(gun\) =>/)
  })

  it('canlı bölge ve aria-busy ile durum duyurulur', () => {
    expect(view).toMatch(/class="sr-only"\s+aria-live="polite"/)
    expect(view).toMatch(/const seciliGunOzeti = computed/)
    expect(view).toMatch(/:aria-busy="yukleniyor"/)
  })

  it('yüklenirken iskelet, hareket azaltma desteği var', () => {
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/v-if="yukleniyor && !olaylar\.length"/)
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })

  it('mobil kart düzeni ve kompakt takvim eşikleri tanımlı', () => {
    expect(view).toMatch(/@media \(max-width: 640px\)/)
    expect(view).toMatch(/grid-template-areas:\s*\n?\s*'sec icerik eylem'/)
    expect(view).toMatch(/\.gorev-sec \{\s*grid-area: sec;/)
    expect(view).toMatch(/\.gun-hucre \{\s*min-height: 44px;/)
    expect(view).toMatch(/\.tip-cip \{\s*flex-shrink: 0;\s*min-height: 32px;/)
  })

  it('i18n gün etiketi tr ve en için tam', () => {
    expect(tr).toMatch(/"gunAria": "\{tarih\}: \{sayi\} olay"/)
    expect(en).toMatch(/"gunAria": "\{tarih\}: \{sayi\} events"/)
  })
})
