import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 1 — Faturalar liste ekrani: PageHeader + KPI seridi + mobil kart.
 * (Kaynak duzeyinde sozlesme; agir view mount edilmez.)
 */

const src = readFileSync(join(process.cwd(), 'src/views/Faturalar.vue'), 'utf8')

describe('Faturalar liste ekrani — Faz1', () => {
  it('PageHeader kullanir (duz h1 yerine)', () => {
    expect(src).toContain('<PageHeader')
    expect(src).toContain("t('faturalar.title')")
    expect(src).not.toContain('<h1 class="page-title">')
  })

  it('KPI seridi fatura ozetinden beslenir', () => {
    expect(src).toContain('fatura-kpi')
    expect(src).toContain('faturaAPI.ozet')
    expect(src).toContain('faturaOzet')
    expect(src).toContain("t('faturalar.kpiCiro')")
  })

  it('kucuk ekran icin kart listesi icerir', () => {
    expect(src).toContain('fatura-kart-liste')
    expect(src).toContain('fatura-kart')
  })

  it('arama filtre cubuguna tasinmistir (Toolbar kaldirildi)', () => {
    expect(src).not.toContain('<Toolbar')
  })
})
