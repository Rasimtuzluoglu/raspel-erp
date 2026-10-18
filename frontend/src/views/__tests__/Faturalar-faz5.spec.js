import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 5 — İkincil fatura ekranları: PageHeader tutarlılığı.
 */

const oku = (p) => readFileSync(join(process.cwd(), 'src', 'views', p), 'utf8')

describe('EFatura / Fatura Geçmiş Raporu — Faz5', () => {
  it('EFatura PageHeader kullanır', () => {
    const s = oku('EFatura.vue')
    expect(s).toContain('<PageHeader')
    expect(s).not.toContain('<h1 class="page-title">')
    expect(s).toContain("t('efatura.title')")
  })

  it('Fatura Geçmiş Raporu PageHeader + filtre çubuğu kullanır', () => {
    const s = oku('FaturaGecmisRaporu.vue')
    expect(s).toContain('<PageHeader')
    expect(s).not.toContain('<h1 class="page-title">')
    expect(s).toContain('fgr-filtre-cubugu')
  })
})
