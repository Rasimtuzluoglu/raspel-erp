import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Seri/Lot Takibi revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (sunucu taraflı filtre/sayfalama + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSeriler.vue')
const stokApi = oku('api/modules/stok.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/envanter/StokSeriController.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSeriService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokSeriRepository.java'),
  'utf8'
)
const excelController = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sistem/ExcelExportController.java'),
  'utf8'
)

describe('StokSeriler — Faz 2', () => {
  it('sunucu taraflı filtre + sayfalama', () => {
    expect(view).toMatch(/stokSeriAPI\.filtreli\(filtreParams\(\)\)/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/@sort="siralaDegisti"/)
    expect(stokApi).toMatch(/filtreli\(params = \{\}\)\s*\{[\s\S]*?\/stok-seri\/filtreli/)
  })

  it('durum çipleri + depo/SKT filtreleri', () => {
    expect(view).toMatch(/const durumCipleri = computed/)
    expect(view).toMatch(/class="seri-cipler"/)
    expect(view).toMatch(/v-model="filtreDepo"/)
    expect(view).toMatch(/v-model="filtreSktBaslangic"/)
    expect(view).toMatch(/v-model="filtreSktBitis"/)
    expect(view).toMatch(/const filtreTemizle = /)
  })

  it('Excel export (uç + indirme)', () => {
    expect(dosyaApi).toMatch(/stokSeri\(params\)\s*\{[\s\S]*?\/exports\/stok-seri/)
    expect(view).toMatch(/excelAPI\.stokSeri\(params\)/)
    expect(view).toMatch(/stokSeriler\.excelIndirildi/)
    expect(excelController).toMatch(/@GetMapping\("\/stok-seri"\)/)
  })

  it('backend filtreli sorgu', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(service).toMatch(/public Page<StokSeriDTO> filtreliGetir/)
    expect(repo).toMatch(/Page<StokSeri> filtreliGetir/)
    expect(repo).toMatch(/LOWER\(s\.stok\.ad\) LIKE :q ESCAPE/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['sktBaslangic', 'sktBitis', 'temizle', 'excel', 'excelIndirildi', 'excelHata', 'filtreBos', 'cipTumu']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
