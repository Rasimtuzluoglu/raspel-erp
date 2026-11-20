import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { GRAFIK, SERI, DURUM_RENK } from '../utils/grafikRenkleri.js'

/**
 * Grafik/KPI renk paleti sözleşmesi: renkler tek modülde ve app.css
 * token'larıyla birebir aynı. Ekran dosyalarında stil dışı hex kalmaz.
 */

const SRC = join(process.cwd(), 'src')
const oku = (...yol) => readFileSync(join(SRC, ...yol), 'utf8')
const appCss = oku('assets/app.css')

const tokenDegeri = (ad) => {
  const eslesme = appCss.match(new RegExp(`${ad}:\\s*(#[0-9a-fA-F]{6})`))
  return eslesme ? eslesme[1].toLowerCase() : null
}

const HEDEF_DOSYALAR = [
  'views/Tahsilat.vue',
  'views/Raporlar.vue',
  'views/CekSenet.vue',
  'views/Muhasebe.vue',
  'views/Ajanda.vue',
  'views/Anomaliler.vue',
  'views/KarlilikAnalizi.vue',
  'components/StokDetayDialog.vue'
]

const stilOncesi = (kaynak) => kaynak.split('<style')[0]

describe('Grafik paleti', () => {
  it('semantik renkler app.css token değerleriyle aynı', () => {
    expect(GRAFIK.yesil).toBe(tokenDegeri('--success'))
    expect(GRAFIK.kirmizi).toBe(tokenDegeri('--danger'))
    expect(GRAFIK.amber).toBe(tokenDegeri('--warning'))
    expect(GRAFIK.mavi).toBe(tokenDegeri('--info'))
    expect(GRAFIK.mor).toBe(tokenDegeri('--purple'))
  })

  it('çok serili grafik paleti 7 ayrı renk sunar', () => {
    expect(SERI).toHaveLength(7)
    expect(new Set(SERI).size).toBe(7)
  })

  it('durum eşlemesi tüm seviyeleri kapsar', () => {
    expect(Object.keys(DURUM_RENK).sort()).toEqual(['basarili', 'bilgi', 'hata', 'notr', 'uyari'])
  })
})

describe('Grafik renkleri ekranlara dağılmaz', () => {
  for (const dosya of HEDEF_DOSYALAR) {
    it(`${dosya} stil dışında hex renk taşımaz`, () => {
      const kalan = stilOncesi(oku(dosya)).match(/#[0-9a-fA-F]{6}\b/g) || []
      expect(kalan, `${dosya}: ${kalan.join(', ')}`).toEqual([])
    })
  }

  it('hedef dosyalar palet modülünü kullanır', () => {
    const kullanan = HEDEF_DOSYALAR.filter((d) => oku(d).includes('grafikRenkleri.js'))
    expect(kullanan.length).toBeGreaterThanOrEqual(6)
  })
})
