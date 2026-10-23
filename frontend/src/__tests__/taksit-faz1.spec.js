import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Taksit Takvimi revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (4 KPI + SQL agregasyonları + sekmeli iskelet).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/TaksitTakvimi.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/finans/TaksitController.java')
const service = okuB('service/finans/TaksitService.java')
const repo = okuB('repository/finans/TaksitRepository.java')

describe('Taksit Takvimi — Faz 1', () => {
  it('backend: typed ozet + SQL agregasyonları', () => {
    expect(controller).toMatch(/TaksitOzetDTO> ozet\(/)
    expect(service).toMatch(/TaksitOzetDTO ozet\(Long sirketId\)/)
    expect(service).not.toMatch(/findBySirketIdAndOdemeDurumuNot\(sirketId/)
    expect(repo).toMatch(/sumBekleyenTutar/)
    expect(repo).toMatch(/sumGecikmisTutar/)
    expect(repo).toMatch(/sumBuAyTutar/)
    expect(repo).toMatch(/sumOdenenTutar/)
    expect(repo).toMatch(/countOdenen/)
  })

  it('frontend: 4 KPI + sekmeler + hafıza', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/taksitTakvimi\.kpiOdenen/)
    expect(view).toMatch(/<TabView v-model:active-index="aktifTab">/)
    expect(view).toMatch(/taksitTakvimi\.tabTakvim/)
    expect(view).toMatch(/taksitTakvimi\.tabTaksitler/)
    expect(view).toMatch(/taksitTakvimi\.tabPlanlar/)
    expect(view).toMatch(/raspel_taksit_tab/)
    expect(view).toMatch(/common\.refresh/)
  })

  it('ozet sirket yoksa sıfır döner', () => {
    expect(service).toMatch(/if \(sirketId == null\) \{[\s\S]*?TaksitOzetDTO\.builder\(\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['kpiOdenen', 'tabTakvim', 'tabTaksitler', 'tabPlanlar']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
