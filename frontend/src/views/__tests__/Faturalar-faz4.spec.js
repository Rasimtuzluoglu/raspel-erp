import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 4 — FaturaDetay + Tasarım/Yazdırma + Geçmiş dialogu: tutarlı başlıklar
 * ve sabit aksiyon barı.
 */

const oku = (p) => readFileSync(join(process.cwd(), 'src', p), 'utf8')

describe('Fatura detay/tasarım/geçmiş — Faz4', () => {
  it('geçmiş dialogu ikonlu başlık + responsive genişlik', () => {
    const s = oku('components/FaturaGecmisDialog.vue')
    expect(s).toContain('fg-dialog-baslik')
    expect(s).toContain('maxWidth')
  })

  it('tasarım modalı ikonlu başlık kullanır', () => {
    const s = oku('components/FaturaTasarimModal.vue')
    expect(s).toContain('tasarim-baslik')
    expect(s).toContain('pi pi-palette')
  })

  it('fatura detay aksiyon barı sabit kart', () => {
    const s = oku('views/FaturaDetay.vue')
    expect(s).toMatch(/\.detay-header\s*\{[^}]*position:\s*sticky/)
    expect(s).toMatch(/\.detay-header\s*\{[^}]*border-radius/)
  })
})
