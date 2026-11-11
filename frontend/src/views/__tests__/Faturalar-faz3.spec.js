import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 3 — Fatura formu tek ekrana tasindi.
 * Liste ekrani yalniz liste; form kendi rotasinda 3 panelli.
 */

const liste = readFileSync(join(process.cwd(), 'src/views/Faturalar.vue'), 'utf8')
const ekran = readFileSync(join(process.cwd(), 'src/views/FaturaFormEkrani.vue'), 'utf8')

describe('Fatura formu — Faz3 (tek ekran)', () => {
  it('liste ekraninda form dialogu kalmadi', () => {
    expect(liste).not.toContain('<AppDialog')
    expect(liste).not.toContain('form-grid')
    expect(liste).not.toContain('fatura-form-baslik')
    expect(liste).toContain("name: 'FaturaYeni'")
    expect(liste).toContain("name: 'FaturaDuzenle'")
  })

  it('form ekraninda bolum basliklari ve kalem bileseni vardir', () => {
    expect(ekran).toContain('bolumFaturaBilgileri')
    expect(ekran).toContain('faturaKalemleri')
    expect(ekran).toContain('toplamlar')
    expect(ekran).toContain('FaturaKalemleri')
  })

  it('panel kartlari padding + radius ile ayrilir', () => {
    expect(ekran).toMatch(/\.form-panel\s*\{[^}]*border-radius/)
    expect(ekran).toMatch(/\.form-panel\s*\{[^}]*padding/)
  })

  it('responsive: 3 panel 2 kolona ve tek kolona iner', () => {
    expect(ekran).toMatch(/@media \(max-width: 1199px\)\s*\{[\s\S]*?fatura-form-grid/)
    expect(ekran).toMatch(/@media \(max-width: 767px\)\s*\{[\s\S]*?grid-template-columns: minmax\(0, 1fr\)/)
  })
})
