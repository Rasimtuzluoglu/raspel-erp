import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Stok Sayımı revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (sunucu taraflı filtre/sayfalama + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSayim.vue')
const stokApi = oku('api/modules/stok.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/envanter/StokSayimController.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSayimService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokSayimRepository.java'),
  'utf8'
)
const excelController = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sistem/ExcelExportController.java'),
  'utf8'
)

describe('StokSayim — Faz 2', () => {
  it('sunucu taraflı filtre + sayfalama', () => {
    expect(view).toMatch(/stokSayimAPI\.filtreli\(filtreParams\(\)\)/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/@sort="siralaDegisti"/)
    expect(stokApi).toMatch(/filtreli\(params = \{\}\)\s*\{[\s\S]*?\/stok-sayim\/filtreli/)
  })

  it('durum çipleri + tarih aralığı + temizle', () => {
    expect(view).toMatch(/const durumCipleri = computed/)
    expect(view).toMatch(/class="sayim-cipler"/)
    expect(view).toMatch(/v-model="filtreBaslangic"/)
    expect(view).toMatch(/v-model="filtreBitis"/)
    expect(view).toMatch(/const filtreTemizle = /)
  })

  it('Excel export (uç + indirme)', () => {
    expect(dosyaApi).toMatch(/stokSayim\(params\)\s*\{[\s\S]*?\/exports\/stok-sayim/)
    expect(view).toMatch(/excelAPI\.stokSayim\(params\)/)
    expect(view).toMatch(/stokSayim\.excelIndirildi/)
    expect(excelController).toMatch(/@GetMapping\("\/stok-sayim"\)/)
  })

  it('backend filtreli sorgu', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(service).toMatch(/public Page<StokSayimDTO> filtreliGetir/)
    expect(repo).toMatch(/Page<StokSayim> filtreliGetir/)
    expect(repo).toMatch(/LOWER\(st\.ad\) LIKE :q ESCAPE/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['baslangic', 'bitis', 'temizle', 'excel', 'excelIndirildi', 'excelHata', 'filtreBos', 'cipTumu']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
