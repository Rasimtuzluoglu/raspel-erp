import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Taksit Takvimi revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (sunucu taraflı liste + Excel + ödeme tarihi düzeltmesi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/TaksitTakvimi.vue')
const finansApi = oku('api/modules/finans.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/finans/TaksitController.java')
const service = okuB('service/finans/TaksitService.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('Taksit Takvimi — Faz 2', () => {
  it('backend: cari adı araması + Excel ucu', () => {
    expect(controller).toMatch(/@RequestParam\(required = false\) String q/)
    expect(service).toMatch(/String q, Pageable pageable/)
    expect(service).toMatch(/LOWER\(\).*cariHesap|cb\.like\(cb\.lower\(root\.get\("cariHesap"\)\.get\("ad"\)\)/)
    expect(service).toMatch(/Locale\.ROOT/)
    expect(excelController).toMatch(/@GetMapping\("\/taksitler"\)/)
    expect(excelController).toMatch(/taksitService\.listele\(sirketId, cariId, durum, baslangic, bitis, q, MAX_EXPORT_PAGE\)/)
  })

  it('frontend: lazy liste + filtre parametreleri', () => {
    expect(view).toMatch(/<AppDataTable/)
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/taksitAPI\.listele\(filtreParams\(\)\)/)
    expect(view).toMatch(/raspel_taksit_rows/)
    expect(view).toMatch(/selection-mode="range"/)
    expect(view).toMatch(/@input="aramaGirdi"/)
    expect(view).toMatch(/const kalemAksiyonlari = \(k\)/)
    expect(finansApi).toMatch(/\blistele\(params\)\s*\{[\s\S]*?\/taksitler/)
  })

  it('frontend: Excel filtreye uyar', () => {
    expect(dosyaApi).toMatch(/taksitler\(params\)\s*\{[\s\S]*?responseType: 'blob'/)
    expect(view).toMatch(/excelAPI\.taksitler\(params\)/)
    expect(view).toMatch(/delete params\.page/)
  })

  it('frontend: ödeme tarihi timezone düzeltmesi', () => {
    expect(view).toMatch(/hareketTarihi: getLocalDateString\(odemeForm\.value\.hareketTarihi\)/)
    expect(view).not.toMatch(/hareketTarihi\.toISOString/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aramaPlaceholder', 'filtreBos', 'durumTumu', 'excel', 'excelIndirildi', 'excelHata']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
