import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Personel revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (sunucu taraflı arama/filtre + sayfalama + Excel filtreleri).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Personel.vue')
const ikApi = oku('api/modules/ik.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelController.java')
const service = okuB('service/ik/PersonelService.java')
const repo = okuB('repository/ik/PersonelRepository.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('Personel — Faz 2', () => {
  it('backend: filtreli + departmanlar uçları ve specification', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(controller).toMatch(/@RequestParam\(required = false\) String q/)
    expect(controller).toMatch(/@RequestParam\(required = false\) String durum/)
    expect(controller).toMatch(/@GetMapping\("\/departmanlar"\)/)
    expect(service).toMatch(/Page<PersonelDTO> filtreli\(Long sirketId, String q, String departman, String rol, String durum, Pageable pageable\)/)
    expect(service).toMatch(/filtreSpec/)
    expect(service).toMatch(/cb\.isTrue\(root\.get\("aktif"\)\)/)
    expect(service).toMatch(/List<String> departmanlar\(Long sirketId\)/)
    expect(repo).toMatch(/JpaSpecificationExecutor<Personel>/)
    expect(repo).toMatch(/findDistinctDepartmanBySirketId/)
  })

  it('backend: Excel filtre parametreleri', () => {
    expect(excelController).toMatch(/public ResponseEntity<byte\[\]> personel\([\s\S]*?String q[\s\S]*?String durum\)/)
    expect(excelController).toMatch(/personelService\.filtreli\(sirketId, q, departman, rol, durum, MAX_EXPORT_PAGE\)/)
  })

  it('frontend: lazy tablo + filtre parametreleri', () => {
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/@page="sayfaDegisti"/)
    expect(view).toMatch(/@sort="siralaDegisti"/)
    expect(view).toMatch(/const filtreParams = \(\)/)
    expect(view).toMatch(/personelAPI\.filtreli\(filtreParams\(\)\)/)
    expect(view).toMatch(/raspel_personel_rows/)
    expect(view).toMatch(/const filtreVar = computed/)
    expect(ikApi).toMatch(/filtreli\(params\)\s*\{[\s\S]*?\/personel\/filtreli/)
    expect(ikApi).toMatch(/departmanlar\(\)\s*\{[\s\S]*?\/personel\/departmanlar/)
  })

  it('frontend: Excel filtreye uyar', () => {
    expect(dosyaApi).toMatch(/personel\(params\)\s*\{[\s\S]*?responseType: 'blob'/)
    expect(view).toMatch(/excelAPI\.personel\(params\)/)
    expect(view).toMatch(/delete params\.page/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aramaPlaceholder', 'filtreBos', 'filtreDurumTumu', 'excelIndirildi', 'excelHata']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
