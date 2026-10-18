import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 2 — Fatura satir ⋮ menuesu: gruplama + sabit konum + kayit baslığı.
 */

const src = readFileSync(join(process.cwd(), 'src/views/Faturalar.vue'), 'utf8')

describe('Faturalar satir menusu — Faz2', () => {
  it('menu gruplandirilmis ve ayracli', () => {
    expect(src).toContain('grupYazdirma')
    expect(src).toContain('grupGonderim')
    expect(src).toContain('grupKayit')
    expect(src).toContain('grupLojistik')
    expect(src).toContain('grupFinans')
    expect(src).toContain('{ ayrac: true }')
  })

  it('renk kodlari uygulanir (kes yesil, iptal kirmizi, tahsilat yesil)', () => {
    expect(src).toContain("sinif: 'eylem-pozitif'")
    expect(src).toContain("sinif: 'eylem-sil'")
  })

  it('satir menusu sabit konum + kayit basligi kullanir', () => {
    expect(src).toContain('sabit-konum')
    expect(src).toContain(':baslik=')
  })
})
