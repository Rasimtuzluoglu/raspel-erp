import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Şubeler revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (durum çipleri + filtrelenmiş liste + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Subeler.vue')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const excelController = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sistem/ExcelExportController.java'),
  'utf8'
)

describe('Subeler — Faz 2', () => {
  it('durum çipleri + filtrelenmiş liste', () => {
    expect(view).toMatch(/const durumCipleri = computed/)
    expect(view).toMatch(/const filtreliListe = computed/)
    expect(view).toMatch(/class="sube-cipler"/)
    expect(view).toMatch(/:value="filtreliListe"/)
    expect(view).toMatch(/:aria-pressed="filtreDurum === c\.deger"/)
  })

  it('Excel export (uç + indirme)', () => {
    expect(dosyaApi).toMatch(/subeler\(params\)\s*\{[\s\S]*?\/exports\/subeler/)
    expect(view).toMatch(/excelAPI\.subeler\(params\)/)
    expect(view).toMatch(/subeler\.excelIndirildi/)
    expect(excelController).toMatch(/@GetMapping\("\/subeler"\)/)
  })

  it('filtre boş durumu', () => {
    expect(view).toMatch(/subeler\.filtreBos/)
    expect(view).toMatch(/filtreDurum !== 'TUMU'/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['cipTumu', 'filtreBos', 'excel', 'excelIndirildi', 'excelHata']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
