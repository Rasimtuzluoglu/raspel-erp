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

  it('satir menusu tetikleyiciye hizalanir ve kayit basligi kullanir', () => {
    expect(src).not.toContain('sabit-konum')
    expect(src).toContain(':baslik=')
  })

  it('birincil aksiyonlar: goruntule + acik satis faturasinda tahsilat', () => {
    expect(src).toContain('fatura-goruntule')
    expect(src).toContain('fatura-tahsilat')
    expect(src).toContain('tahsilatYapilabilir')
    expect(src).toContain("f?.tur === 'SATIS'")
    expect(src).toContain("f?.durum === 'KESILDI'")
    expect(src).toContain('tahsilataGit')
  })

  it('ikon-only butonlarda aria-label vardir', () => {
    expect(src).toContain(':aria-label="t(\'faturalar.goruntule\')"')
    expect(src).toContain(':aria-label="t(\'faturalar.tahsilatAl\')"')
    expect(src).toContain('data-test="fatura-kart-goruntule"')
    expect(src).toContain('data-test="fatura-kart-tahsilat"')
  })
})
