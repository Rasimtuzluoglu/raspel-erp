import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Puantaj revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (şirket geneli sunucu filtre + sayfalama + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Puantaj.vue')
const ikApi = oku('api/modules/ik.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelPuantajController.java')
const service = okuB('service/ik/PersonelPuantajService.java')
const repo = okuB('repository/ik/PersonelPuantajRepository.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('Puantaj — Faz 2', () => {
  it('backend: filtreli ucu + sorgu', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(controller).toMatch(/Long personelId/)
    expect(controller).toMatch(/java\.time\.LocalDate baslangic/)
    expect(service).toMatch(/Page<PersonelPuantajDTO> filtreli\(/)
    expect(service).toMatch(/"TUMU"\.equalsIgnoreCase\(durum\)/)
    expect(repo).toMatch(/Page<PersonelPuantaj> filtrele\(/)
    expect(repo).toMatch(/:q IS NULL OR p\.personelId IN/)
  })

  it('backend: Excel filtre parametreleri', () => {
    expect(excelController).toMatch(/@GetMapping\("\/puantaj"\)/)
    expect(excelController).toMatch(/personelPuantajService\.filtreli\(sirketId, personelId, baslangic, bitis, durum, q, MAX_EXPORT_PAGE\)/)
  })

  it('frontend: şirket geneli lazy tablo + filtre parametreleri', () => {
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/puantajAPI\.filtreli\(filtreParams\(\)\)/)
    expect(view).toMatch(/raspel_puantaj_rows/)
    expect(view).toMatch(/selection-mode="range"/)
    expect(view).toMatch(/@input="aramaGirdi"/)
    expect(view).not.toMatch(/loadData/)
    expect(ikApi).toMatch(/filtreli\(params\)\s*\{[\s\S]*?\/personel-puantaj\/filtreli/)
  })

  it('frontend: Excel filtreye uyar', () => {
    expect(dosyaApi).toMatch(/puantaj\(params\)\s*\{[\s\S]*?responseType: 'blob'/)
    expect(view).toMatch(/excelAPI\.puantaj\(params\)/)
    expect(view).toMatch(/delete params\.page/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['durumTumu', 'aramaPlaceholder', 'filtreBos', 'excel', 'excelIndirildi', 'excelHata']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
