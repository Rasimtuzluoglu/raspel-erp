import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Masraflar revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI/ozet + iskelet).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Masraflar.vue')
const finansApi = oku('api/modules/finans.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/finans/MasrafController.java')
const service = okuB('service/finans/MasrafService.java')
const repo = okuB('repository/finans/MasrafRepository.java')

describe('Masraflar — Faz 1', () => {
  it('PageHeader subtitle + yenile + yetkili yeni masraf', () => {
    expect(view).toMatch(/:subtitle="t\('masraflar\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
    expect(view).toMatch(/v-if="canWrite"[\s\S]*?masraflar\.yeniMasraf/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/masraflar\.kpiBuAy/)
    expect(view).toMatch(/masraflar\.kpiKayitSayisi/)
    expect(view).toMatch(/masrafAPI\.ozet\(\)/)
    expect(finansApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/masraflar\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/MasrafOzetDTO ozet\(Long sirketId\)/)
    expect(repo).toMatch(/sumTutarBySirketIdAndTarihBetween/)
    expect(repo).toMatch(/sumKdvBySirketIdAndTarihBetween/)
    expect(repo).toMatch(/countBySirketId/)
  })

  it('ozet sirket yoksa sıfır döner', () => {
    expect(service).toMatch(/if \(sirketId == null\) \{[\s\S]*?MasrafOzetDTO\.builder\(\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiBuAy', 'kpiBuYil', 'kpiKdv', 'kpiKayitSayisi']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
