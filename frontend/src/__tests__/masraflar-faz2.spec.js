import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Masraflar revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (sunucu taraflı arama/filtre + sayfalama + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Masraflar.vue')
const finansApi = oku('api/modules/finans.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/finans/MasrafController.java')
const service = okuB('service/finans/MasrafService.java')
const repo = okuB('repository/finans/MasrafRepository.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('Masraflar — Faz 2', () => {
  it('backend: filtreli ucu + sorgu', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(controller).toMatch(/String kategori/)
    expect(controller).toMatch(/java\.time\.LocalDate baslangic/)
    expect(service).toMatch(/Page<MasrafDTO> filtreli\(/)
    expect(service).toMatch(/"TUMU"\.equalsIgnoreCase\(kategori\)/)
    expect(repo).toMatch(/Page<Masraf> filtrele\(/)
    expect(repo).toMatch(/:q IS NULL OR/)
  })

  it('backend: Excel filtre parametreleri', () => {
    expect(excelController).toMatch(/@GetMapping\("\/masraflar"\)/)
    expect(excelController).toMatch(/masrafService\.filtreli\(sirketId, kategori, baslangic, bitis, q, MAX_EXPORT_PAGE\)/)
  })

  it('frontend: lazy tablo + filtre parametreleri', () => {
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/masrafAPI\.filtreli\(filtreParams\(\)\)/)
    expect(view).toMatch(/raspel_masraflar_rows/)
    expect(view).toMatch(/selection-mode="range"/)
    expect(view).toMatch(/@input="aramaGirdi"/)
    expect(view).toMatch(/kategoriEtiket/)
    expect(finansApi).toMatch(/filtreli\(params\)\s*\{[\s\S]*?\/masraflar\/filtreli/)
  })

  it('frontend: Excel filtreye uyar', () => {
    expect(dosyaApi).toMatch(/masraflar\(params\)\s*\{[\s\S]*?responseType: 'blob'/)
    expect(view).toMatch(/excelAPI\.masraflar\(params\)/)
    expect(view).toMatch(/delete params\.page/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['kategoriTumu', 'aramaPlaceholder', 'filtreBos', 'excel', 'excelIndirildi', 'excelHata']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
