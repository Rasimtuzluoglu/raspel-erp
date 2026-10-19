import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Anomaliler revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (seviye çipleri + tür filtresi + arama + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Anomaliler.vue')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const excelController = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sistem/ExcelExportController.java'),
  'utf8'
)

describe('Anomaliler — Faz 2', () => {
  it('seviye çipleri + tür filtresi + arama', () => {
    expect(view).toMatch(/const seviyeCipleri = computed/)
    expect(view).toMatch(/const filtreliAnomaliler = computed/)
    expect(view).toMatch(/class="anomali-cipler"/)
    expect(view).toMatch(/v-model="filtreTur"/)
    expect(view).toMatch(/v-model="filtreQ"/)
    expect(view).toMatch(/v-for="item in filtreliAnomaliler"/)
  })

  it('son tarama zamanı + filtre boş durumu', () => {
    expect(view).toMatch(/const sonTarama = ref/)
    expect(view).toMatch(/anomaliler\.sonTarama/)
    expect(view).toMatch(/anomaliler\.filtreBos/)
    expect(view).toMatch(/const filtreTemizle = /)
  })

  it('Excel export (anomaliler + IP)', () => {
    expect(dosyaApi).toMatch(/anomaliler\(params\)\s*\{[\s\S]*?\/exports\/anomaliler/)
    expect(dosyaApi).toMatch(/ipWhitelist\(\)\s*\{[\s\S]*?\/exports\/ip-whitelist/)
    expect(view).toMatch(/excelAPI\.anomaliler\(/)
    expect(view).toMatch(/excelAPI\.ipWhitelist\(\)/)
    expect(excelController).toMatch(/@GetMapping\("\/anomaliler"\)/)
    expect(excelController).toMatch(/@GetMapping\("\/ip-whitelist"\)/)
    expect(excelController).toMatch(/hasRole\('ADMIN'\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'cipTumu',
      'aramaPlaceholder',
      'turFiltre',
      'sonTarama',
      'excel',
      'excelIndirildi',
      'excelHata',
      'filtreBos',
      'temizle'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
