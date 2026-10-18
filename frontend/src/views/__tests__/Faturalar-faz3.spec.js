import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 3 — Fatura oluştur/düzenle dialogu: bölüm başlıkları + kart düzeni.
 */

const src = readFileSync(join(process.cwd(), 'src/views/Faturalar.vue'), 'utf8')

describe('Fatura olustur/duzenle dialogu — Faz3', () => {
  it('bolum basliklari kullanir (Fatura Bilgileri / Urun Ekle / Kalemler)', () => {
    expect(src).toContain('fatura-form-baslik')
    expect(src).toContain('bolumFaturaBilgileri')
    expect(src).toContain('bolumUrunEkle')
    expect(src).toContain("t('faturalar.faturaKalemleri')")
  })

  it('form-grid kart gorunumu (padding + radius)', () => {
    expect(src).toMatch(/\.form-grid\s*\{[^}]*border-radius/)
  })

  it('responsive: form-grid tek kolona iner', () => {
    expect(src).toMatch(/@media \(max-width: 900px\)\s*\{[^}]*\.form-grid/)
  })
})
