import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Faz3E — Saha Portalı revizyonu (kaynak-tabanlı doğrulama).
 */

const SRC = join(process.cwd(), 'src')
const saha = readFileSync(join(SRC, 'views/SahaPortali.vue'), 'utf8')
const panel = readFileSync(join(SRC, 'components/SahaSiparislerPanel.vue'), 'utf8')

describe('Faz3E — Saha Portalı', () => {
  it('mobil için responsive kurallar ekli', () => {
    expect(saha).toMatch(/@media \(max-width: 640px\)/)
    expect(saha).toMatch(/\.form-row-2\s*\{\s*grid-template-columns: 1fr/)
    expect(saha).toMatch(/\.p-tabview-nav/)
  })

  it('çevrimdışı kuyruk bekleyen sayısı görünür + elle senkron', () => {
    expect(saha).toMatch(/kuyrukBekliyor/)
    expect(saha).toMatch(/const kuyrukSenkronizeEdiliyor/)
  })

  it('sipariş panelinde i18n boşlukları giderildi (ham metin yok)', () => {
    expect(panel).not.toMatch(/<small>Tutar<\/small>/)
    expect(panel).not.toMatch(/label="Durum"/)
    expect(panel).toMatch(/\$t\('sahaPortali\.tutar'\)/)
    expect(panel).toMatch(/const durumEtiket/)
    expect(panel).toMatch(/siparisTakip\.durum\./)
  })
})
