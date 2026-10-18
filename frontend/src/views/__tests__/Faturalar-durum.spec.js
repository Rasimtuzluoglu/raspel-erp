import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Durum sutunu: rozetler hizali (ust uste binme yok).
 */

const src = readFileSync(join(process.cwd(), 'src/views/Faturalar.vue'), 'utf8')
const detay = readFileSync(join(process.cwd(), 'src/views/FaturaDetay.vue'), 'utf8')

describe('Fatura durum rozetleri', () => {
  it('durum hucresi flex konteyner kullanir', () => {
    expect(src).toContain('durum-hucre')
    expect(src).toMatch(/\.durum-hucre\s*\{[^}]*display:\s*flex/)
  })

  it('durum/odeme/tur rozetleri nowrap ve inline-flex', () => {
    expect(src).toMatch(/\.durum-badge\s*\{[^}]*white-space:\s*nowrap/)
    expect(src).toMatch(/\.odeme-badge\s*\{[^}]*white-space:\s*nowrap/)
    expect(src).toMatch(/\.badge\s*\{[^}]*white-space:\s*nowrap/)
  })

  it('fatura detay rozetleri de hizali', () => {
    expect(detay).toMatch(/\.durum-badge\s*\{[^}]*display:\s*inline-flex/)
  })
})
