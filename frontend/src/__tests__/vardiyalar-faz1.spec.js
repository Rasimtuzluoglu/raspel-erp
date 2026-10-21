import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Vardiyalar revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI/ozet + sayfa iskeleti).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Vardiyalar.vue')
const ikApi = oku('api/modules/ik.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/VardiyaController.java')
const service = okuB('service/ik/VardiyaService.java')
const repo = okuB('repository/ik/VardiyaRepository.java')

describe('Vardiyalar — Faz 1', () => {
  it('PageHeader subtitle + yenile + yeni vardiya', () => {
    expect(view).toMatch(/:subtitle="t\('vardiyalar\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
    expect(view).toMatch(/vardiyalar\.yeniVardiya/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/vardiyalar\.kpiToplam/)
    expect(view).toMatch(/vardiyalar\.kpiPersonel/)
    expect(view).toMatch(/vardiyaAPI\.ozet\(\)/)
    expect(ikApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/vardiyalar\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/VardiyaOzetDTO ozet\(Long sirketId\)/)
    expect(repo).toMatch(/countBySirketIdAndTarihBetween/)
    expect(repo).toMatch(/countDistinctPersonelBySirketId/)
  })

  it('ozet sirket yoksa sıfır döner', () => {
    expect(service).toMatch(/if \(sirketId == null\) \{\s*return com\.raspel\.erp\.dto\.ik\.VardiyaOzetDTO\.builder\(\)\.build\(\);/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiToplam', 'kpiBugun', 'kpiBuHafta', 'kpiPersonel']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
