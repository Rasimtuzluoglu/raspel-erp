import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Liste ekranlarinda ortak filtre karti ve yogunluk kaliciligi.
 * Kaynak duzeyinde sozlesme; agir mount edilmez.
 */

const oku = (p) => readFileSync(join(process.cwd(), 'src', p), 'utf8')

const EKRANLAR = [
  ['views/CariHesaplar.vue', 'cari-filtreler'],
  ['views/Stoklar.vue', 'filter-bar'],
  ['views/Irsaliyeler.vue', 'irsaliye-arac'],
  ['views/Tahsilat.vue', 'tahsilat-arac'],
  ['views/SiparisTakip.vue', 'takip-filtre'],
  ['views/Masraflar.vue', 'masraf-arac']
]

describe('Ortak filtre karti — liste ekranlari', () => {
  it.each(EKRANLAR)('%s filtre cubugu filtre-kart sinifini kullanir', (dosya, sinif) => {
    const src = oku(dosya)
    expect(src).toMatch(new RegExp(`class="${sinif}[^"]*filtre-kart|class="[^"]*filtre-kart[^"]*${sinif}`))
  })

  it('global filtre-kart stili tanimlidir', () => {
    const css = oku('assets/app.css')
    expect(css).toMatch(/\.filtre-kart\s*\{[^}]*border-radius:\s*12px/)
    expect(css).toMatch(/\.filtre-kart\s*\{[^}]*background:\s*var\(--bg-card\)/)
  })

  it('TabloAyarlari yogunluk degisimini de kalici saklar', () => {
    const src = oku('components/TabloAyarlari.vue')
    expect(src).toContain('watch(yogunluk, kaydet)')
    expect(src).toContain('const kaydet = () =>')
  })

  it('Masraflar filtresinde temizle aksiyonu vardir', () => {
    const src = oku('views/Masraflar.vue')
    expect(src).toContain('data-test="masraf-filtre-temizle"')
    expect(src).toContain('const filtreTemizle = () =>')
    expect(src).toContain("tarihAraligi.value = null")
  })
})
