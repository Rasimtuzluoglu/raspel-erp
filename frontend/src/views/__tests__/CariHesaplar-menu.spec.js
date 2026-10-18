import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Cari satir ⋮ menusu: gruplu + zenginlestirilmis + sabit konum.
 * (Dogrulama kaynak duzeyinde; davranis SatirEylemleri testlerinde.)
 */

const src = readFileSync(join(process.cwd(), 'src/views/CariHesaplar.vue'), 'utf8')

describe('CariHesaplar satir menusu', () => {
  it('menu gruplu bolumler icerir', () => {
    expect(src).toContain('grupFinans')
    expect(src).toContain('grupKayit')
    expect(src).toContain('grupIletisim')
    expect(src).toContain('grupDiger')
  })

  it('yeni aksiyonlar baglanmis', () => {
    expect(src).toContain('cariWhatsapp')
    expect(src).toContain('cariCogalt')
    expect(src).toContain('aktifPasifToggle')
    expect(src).toContain('cariAraTelefon')
    expect(src).toContain('cariEpostaGonder')
  })

  it('menu sabit konum ve kayit basligi kullanir', () => {
    expect(src).toContain('sabit-konum')
    expect(src).toContain(':baslik=')
  })
})
