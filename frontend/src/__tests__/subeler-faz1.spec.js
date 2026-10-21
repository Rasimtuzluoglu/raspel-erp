import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Şubeler revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI/ozet + AppDataTable + tek EmptyState).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Subeler.vue')
const sistemApi = oku('api/modules/sistem.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/sube/SubeController.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sube/SubeService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/sube/SubeRepository.java'),
  'utf8'
)

describe('Subeler — Faz 1', () => {
  it('PageHeader subtitle + yenile', () => {
    expect(view).toMatch(/:subtitle="t\('subeler\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/subeler\.kpiToplam/)
    expect(view).toMatch(/subeAPI\.ozet\(\)/)
    expect(sistemApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/subeler\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/SubeOzetDTO ozet/)
    expect(repo).toMatch(/countBySirketIdAndAktifFalse/)
  })

  it('AppDataTable + arama + boş durum', () => {
    expect(view).toMatch(/<AppDataTable/)
    expect(view).toMatch(/arama-aktif/)
    expect(view).toMatch(/gorunum-anahtari="subeler"/)
    expect((view.match(/<EmptyState/g) || []).length).toBe(2)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiToplam', 'kpiAktif', 'kpiPasif', 'kpiDepo', 'aramaPlaceholder']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
