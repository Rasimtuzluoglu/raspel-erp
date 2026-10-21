import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * İzinler revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (sunucu taraflı arama/filtre + sayfalama + Excel filtreleri).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Izinler.vue')
const ikApi = oku('api/modules/ik.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelIzinController.java')
const service = okuB('service/ik/PersonelIzinService.java')
const repo = okuB('repository/ik/PersonelIzinRepository.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('İzinler — Faz 2', () => {
  it('backend: filtreli ucu + sorgu', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(controller).toMatch(/String durum/)
    expect(controller).toMatch(/Long personelId/)
    expect(controller).toMatch(/java\.time\.LocalDate baslangic/)
    expect(service).toMatch(/Page<PersonelIzinDTO> filtreli\(/)
    expect(service).toMatch(/TUMU.*equalsIgnoreCase/s)
    expect(repo).toMatch(/Page<PersonelIzin> filtrele\(/)
    expect(repo).toMatch(/:q IS NULL OR/)
  })

  it('backend: Excel filtre parametreleri', () => {
    expect(excelController).toMatch(/@GetMapping\("\/izinler"\)/)
    expect(excelController).toMatch(/personelIzinService\.filtreli\(sirketId, durum, personelId, baslangic, bitis, q, MAX_EXPORT_PAGE\)/)
  })

  it('frontend: lazy tablo + filtre parametreleri', () => {
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/personelIzinAPI\.filtreli\(filtreParams\(\)\)/)
    expect(view).toMatch(/raspel_izinler_rows/)
    expect(view).toMatch(/selection-mode="range"/)
    expect(view).toMatch(/@input="aramaGirdi"/)
    expect(ikApi).toMatch(/filtreli\(params\)\s*\{[\s\S]*?\/personel-izin\/filtreli/)
  })

  it('frontend: Excel filtreye uyar', () => {
    expect(dosyaApi).toMatch(/izinler\(params\)\s*\{[\s\S]*?responseType: 'blob'/)
    expect(view).toMatch(/excelAPI\.izinler\(params\)/)
    expect(view).toMatch(/delete params\.page/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aramaPlaceholder', 'filtreBos', 'excel', 'excelIndirildi', 'excelHata']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
