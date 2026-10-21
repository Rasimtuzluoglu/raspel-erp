import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Puantaj revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI/ozet + sayfa iskeleti).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Puantaj.vue')
const ikApi = oku('api/modules/ik.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelPuantajController.java')
const service = okuB('service/ik/PersonelPuantajService.java')
const repo = okuB('repository/ik/PersonelPuantajRepository.java')

describe('Puantaj — Faz 1', () => {
  it('PageHeader subtitle + yenile + yeni kayıt', () => {
    expect(view).toMatch(/:subtitle="t\('puantaj\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
    expect(view).toMatch(/puantaj\.yeniKayit/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/puantaj\.kpiGeldi/)
    expect(view).toMatch(/puantaj\.kpiMazeretli/)
    expect(view).toMatch(/puantajAPI\.ozet\(\)/)
    expect(ikApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/personel-puantaj\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/PuantajOzetDTO ozet\(Long sirketId\)/)
    expect(repo).toMatch(/countBySirketIdAndDurum/)
  })

  it('ozet sirket yoksa sıfır döner', () => {
    expect(service).toMatch(/if \(sirketId == null\) \{\s*return com\.raspel\.erp\.dto\.ik\.PuantajOzetDTO\.builder\(\)\.build\(\);/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiGeldi', 'kpiGelmedi', 'kpiIzinli', 'kpiMazeretli']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
