import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Dönemler revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (yıl sonu kapanış önizlemesi + kapanış geçmişi/kullanıcı + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Donemler.vue')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sistem/DonemService.java'),
  'utf8'
)
const excelController = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sistem/ExcelExportController.java'),
  'utf8'
)

describe('Donemler — Faz 3', () => {
  it('kapanış önizlemesi + yapılabilirlik', () => {
    expect(view).toMatch(/const kapanisOnizleme = computed/)
    expect(view).toMatch(/const kapanisYapilabilir = computed/)
    expect(view).toMatch(/donemler\.etkilenecekDonemler/)
    expect(view).toMatch(/donemler\.yilZatenKapali/)
    expect(view).toMatch(/:disabled="!kapanisYapilabilir"/)
  })

  it('kapanış geçmişi kullanıcı + kilitlenen adet gösterir', () => {
    expect(view).toMatch(/donemler\.kilitlenenAdet/)
    expect(view).toMatch(/donemler\.kapanisTarihi/)
    expect(view).toMatch(/k\.kullaniciId/)
    expect(view).toMatch(/k\.kilitlenenDonemSayisi/)
  })

  it('kapanış Excel export (uç + indirme)', () => {
    expect(dosyaApi).toMatch(/donemKapanislari\(\)\s*\{[\s\S]*?\/exports\/donem-kapanislar/)
    expect(view).toMatch(/excelAPI\.donemKapanislari\(\)/)
    expect(view).toMatch(/const kapanisExcelIndir = async/)
  })

  it('backend kapanış sayısını hesaplar ve dışa aktarır', () => {
    expect(service).toMatch(/kilitlenenDonemSayisi\(adet\)/)
    expect(excelController).toMatch(/@GetMapping\("\/donem-kapanislar"\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'etkilenecekDonemler',
      'etkilenecekDonemYok',
      'yilZatenKapali',
      'kapanisYapilamaz',
      'kapanisTarihi',
      'kullanici',
      'kilitlenenAdet'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
