import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Üretim revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (reçete Excel filtreleri + drawer zenginleştirme).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Uretim.vue')
const drawer = oku('components/UretimEmriDetayDrawer.vue')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const service = okuB('service/envanter/UretimService.java')
const dto = okuB('dto/envanter/UretimEmriDTO.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('Üretim — Faz 2', () => {
  it('reçete Excel’i arama ve aktiflik filtresi alır', () => {
    expect(excelController).toMatch(/uretimService\.recetelerFiltreli\(sirketId, q, aktif\)/)
    expect(excelController).toMatch(/@RequestParam\(required = false\) Boolean aktif/)
    expect(service).toMatch(/public List<ReceteDTO> recetelerFiltreli\(Long sirketId, String q, Boolean aktif\)/)
    expect(dosyaApi).toMatch(/uretimReceteler\(params\)\s*\{[\s\S]*?\/exports\/uretim-receteler/)
    expect(view).toMatch(/excelAPI\.uretimReceteler\(receteParams\(\)\)/)
    expect(view).toMatch(/const receteParams = \(\)/)
    expect(view).toMatch(/const receteQ = ref\(''\)/)
  })

  it('emir DTO hesaplanan alanları taşır', () => {
    expect(dto).toMatch(/private BigDecimal fireOrani;/)
    expect(dto).toMatch(/private BigDecimal birimMaliyet;/)
    expect(dto).toMatch(/private BigDecimal sureSaat;/)
    expect(dto).toMatch(/private long gecikmeGun;/)
    expect(service).toMatch(/\.fireOrani\(fireOrani\)/)
    expect(service).toMatch(/\.birimMaliyet\(birimMaliyet\)/)
    expect(service).toMatch(/\.sureSaat\(sureSaat\)/)
    expect(service).toMatch(/\.gecikmeGun\(gecikmeGun\)/)
  })

  it('drawer yeni alanları ve durum renkli zaman çizelgesini gösterir', () => {
    expect(drawer).toMatch(/uretim\.birimMaliyet/)
    expect(drawer).toMatch(/uretim\.gerceklesenFireOrani/)
    expect(drawer).toMatch(/uretim\.gerceklesenSure/)
    expect(drawer).toMatch(/uretim\.gecikme/)
    expect(drawer).toMatch(/:class="durumSinif\(log\.yeniDurum\)"/)
    expect(drawer).toMatch(/const durumIkon = \(d\) =>/)
    expect(drawer).toMatch(/\.zc-adim\.iptal \.zc-nokta/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'receteAra',
      'birimMaliyet',
      'gerceklesenFireOrani',
      'gerceklesenSure',
      'gecikme',
      'gun',
      'saat'
    ]) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
