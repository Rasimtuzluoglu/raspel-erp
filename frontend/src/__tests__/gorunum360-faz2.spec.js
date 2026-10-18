import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * 360° Görünümler revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (Stok Kâr: tarih aralığı + kategori/negatif filtre + arama/sıralama + Excel).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Gorunumler360.vue')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Gorunumler360 — Faz 2 (Stok Kâr)', () => {
  it('tarih aralığı sunucu filtresi', () => {
    expect(view).toMatch(/v-model="stokBaslangic"/)
    expect(view).toMatch(/v-model="stokBitis"/)
    expect(view).toMatch(/const stokKarYukle = async/)
    expect(view).toMatch(/raporAPI\.stokKar360\(stokParams\(\)\)/)
    expect(view).toMatch(/const stokParams = \(\)/)
  })

  it('kategori + negatif marj filtreleri + KPI tıklama', () => {
    expect(view).toMatch(/const filtreliUrunler = computed/)
    expect(view).toMatch(/kategoriFiltresi/)
    expect(view).toMatch(/const kategoriler = computed/)
    expect(view).toMatch(/sadeceNegatif = true/)
    expect(view).toMatch(/class="stok-cipler"/)
    expect(view).toMatch(/@click="sadeceNegatif = !sadeceNegatif"/)
  })

  it('tablo arama/sıralama/görünüm anahtarı', () => {
    expect(view).toMatch(/arama-aktif/)
    expect(view).toMatch(/gorunum-anahtari="gorunum360-stok-kar"/)
    expect(view).toMatch(/field="brutKar"[\s\S]*?sortable/)
    expect(view).toMatch(/const stokFiltreTemizle = \(\)/)
  })

  it('Excel export (uç + bildirim)', () => {
    expect(dosyaApi).toMatch(/gorunum360StokKar\(params\)\s*\{[\s\S]*?\/exports\/gorunum360-stok-kar/)
    expect(view).toMatch(/excelAPI\.gorunum360StokKar\(stokParams\(\)\)/)
    expect(view).toMatch(/gorunum360\.excelIndirildi/)
    expect(view).toMatch(/gorunum360\.excelHata/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of ['baslangic', 'bitis', 'kategori', 'temizle', 'excel', 'cipTumu', 'cipNegatif', 'filtreBos']) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
