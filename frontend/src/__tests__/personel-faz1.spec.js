import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Personel revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI/ozet + iskelet).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Personel.vue')
const ikApi = oku('api/modules/ik.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelController.java')
const service = okuB('service/ik/PersonelService.java')
const repo = okuB('repository/ik/PersonelRepository.java')

describe('Personel — Faz 1', () => {
  it('PageHeader subtitle + yenile + ADMIN yeni personel', () => {
    expect(view).toMatch(/:subtitle="t\('personel\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
    expect(view).toMatch(/v-if="authStore\.isAdmin"[\s\S]*?personel\.yeniPersonel/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/personel\.kpiToplam/)
    expect(view).toMatch(/personel\.kpiBekleyenIzin/)
    expect(view).toMatch(/personelAPI\.ozet\(\)/)
    expect(ikApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/personel\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/PersonelOzetDTO ozet\(Long sirketId\)/)
    expect(service).toMatch(/countByDurumAndSirketId\("BEKLEMEDE"/)
    expect(repo).toMatch(/countBySirketId\(Long sirketId\)/)
    expect(repo).toMatch(/countBySirketIdAndAktifFalse/)
  })

  it('ozet sirket yoksa sıfır döner', () => {
    expect(service).toMatch(/if \(sirketId == null\) \{\s*return com\.raspel\.erp\.dto\.ik\.PersonelOzetDTO\.builder\(\)\.build\(\);/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiToplam', 'kpiAktif', 'kpiPasif', 'kpiBekleyenIzin']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
