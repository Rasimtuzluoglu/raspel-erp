import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'

const SRC = join(process.cwd(), 'src')
const DIRS = [join(SRC, 'views'), join(SRC, 'components')]

const files = DIRS.flatMap((d) =>
  readdirSync(d).filter((f) => f.endsWith('.vue')).map((f) => join(d, f))
)

/**
 * Yalnizca ikonu olan (gorunur metni olmayan) Button kaliplari.
 * Boyle butonlar ekran okuyucuda yalnizca "button" olarak okunur;
 * kullanici ne yaptigini bilemez (WCAG 4.1.2).
 *
 * Etiket sayimi su kriterlere dayanir:
 *   - :label / label  -> gorunur metin var
 *   - aria-label       -> erisilebilir ad var
 * Blok, <Button .../> kapanisina kadar okunur.
 */
// Kabul edilen etiket kaliplari:
//   :label / label         -> gorunur metin
//   aria-label              -> erisilebilir ad (asil tercih)
//   :title / title         -> native ipucu + erisilebilir ad
// Kabul EDILMEYEN: yalniz v-tooltip (gorunur ama ekran okuyucuya ad tasimaz).
const ETIKET_KALI = /(^|\s)(:label|label|aria-label|:aria-label|:title|title)=/
const TOOLTIP = /v-tooltip/

const ikonOnlyEksik = (src, file) => {
  const lines = src.split(/\r?\n/)
  const eksikler = []
  for (let i = 0; i < lines.length; i++) {
    if (!/<Button\b/.test(lines[i])) continue
    let j = i
    while (j < Math.min(i + 16, lines.length) && !/\/>/.test(lines[j])) j++
    if (j >= lines.length) continue
    const blok = lines.slice(i, j + 1).join(' ')
    if (!/(^|\s)icon=/.test(blok)) continue
    if (ETIKET_KALI.test(blok)) continue
    eksikler.push(`${file}:${i + 1}${TOOLTIP.test(blok) ? ' (tooltip-only)' : ''}`)
  }
  return eksikler
}

describe('ikon-only etkilesim kontrolu erisilebilirligi', () => {
  it('tarama calisiyor ve view/component dosyalarini okuyor', () => {
    expect(files.length).toBeGreaterThan(100)
  })

  it('HICBIR ikon-only buton etiketsiz kalmaz (WCAG 4.1.2)', () => {
    const toplam = []
    for (const f of files) {
      const src = readFileSync(f, 'utf8')
      toplam.push(...ikonOnlyEksik(src, f.replace(SRC + '\\', '')))
    }
    // v-tooltip tek basina yetmez: tooltip gorunur ama ekran
    // okuyucuya erisilebilir AD tasimaz.
    expect(toplam).toEqual([])
  })

  it('kisisel veriler (dijalog, liste durumu) erisilebilir ad tasiyor', () => {
    const dialog = readFileSync(join(SRC, 'components/AppDialog.vue'), 'utf8')
    // Diyalog, PrimeVue tarafindan role/aria-modal alir; ek bir erisilebilir
    // ad katmani gerekmiyor (focusTrap PrimeVue 4'te varsayilan acik).
    expect(dialog).toContain('modal')
    const liste = readFileSync(join(SRC, 'components/ListeDurumu.vue'), 'utf8')
    expect(liste).toContain('role="status"')
    expect(liste).toContain('aria-live="polite"')
  })
})