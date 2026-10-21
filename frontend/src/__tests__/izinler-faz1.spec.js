import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * İzinler revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI/ozet + AppDataTable iskeleti).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Izinler.vue')
const ikApi = oku('api/modules/ik.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelIzinController.java')
const service = okuB('service/ik/PersonelIzinService.java')
const repo = okuB('repository/ik/PersonelIzinRepository.java')

describe('İzinler — Faz 1', () => {
  it('PageHeader subtitle + yenile', () => {
    expect(view).toMatch(/:subtitle="t\('izinler\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/izinler\.kpiBekleyen/)
    expect(view).toMatch(/izinler\.kpiBugunIzinli/)
    expect(view).toMatch(/personelIzinAPI\.ozet\(\)/)
    expect(ikApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/personel-izin\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/IzinOzetDTO ozet\(Long sirketId\)/)
    expect(service).toMatch(/countByDurumAndSirketId\("BEKLEMEDE"/)
    expect(service).toMatch(/countBugunIzinliAndSirketId/)
    expect(repo).toMatch(/countBugunIzinliAndSirketId/)
  })

  it('AppDataTable + kayıt görünümü hafızası', () => {
    expect(view).toMatch(/<AppDataTable/)
    expect(view).toMatch(/gorunum-anahtari="izinler"/)
    expect(view).not.toMatch(/<DataTable/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiBekleyen', 'kpiOnaylanan', 'kpiReddedilen', 'kpiBugunIzinli']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
