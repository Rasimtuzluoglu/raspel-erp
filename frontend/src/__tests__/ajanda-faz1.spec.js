import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Ajanda revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (4 KPI + /ajanda/ozet + sekmeli iskelet).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Ajanda.vue')
const sistemApi = oku('api/modules/sistem.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/sistem/AjandaController.java')
const service = okuB('service/sistem/AjandaService.java')
const gorevRepo = okuB('repository/sistem/AjandaGorevRepository.java')

describe('Ajanda — Faz 1', () => {
  it('PageHeader subtitle + yenile + aksiyonlar', () => {
    expect(view).toMatch(/:subtitle="t\('ajanda\.altBaslik'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
    expect(view).toMatch(/ajanda\.gorevEkle/)
    expect(view).toMatch(/ajanda\.hatirlatici/)
  })

  it('backend: ozet DTO + agregasyonlar', () => {
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/AjandaOzetDTO ozet\(Long sirketId, Long kullaniciId\)/)
    expect(service).toMatch(/toplamKalanVadeAraliginda/)
    expect(service).toMatch(/countByKullaniciIdAndDurumAndBitisTarihiBefore/)
    expect(gorevRepo).toMatch(/countByKullaniciIdAndDurum\(/)
  })

  it('frontend: 4 KPI + 4 sekme + hafıza', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/ajanda\.kpiBugun/)
    expect(view).toMatch(/ajanda\.kpiBuAyVade/)
    expect(view).toMatch(/ajanda\.kpiBekleyen/)
    expect(view).toMatch(/ajanda\.kpiGecikmis/)
    expect(view).toMatch(/<TabView v-model:active-index="aktifTab">/)
    expect(view).toMatch(/ajanda\.tabTakvim/)
    expect(view).toMatch(/ajanda\.tabYaklasan/)
    expect(view).toMatch(/ajanda\.tabGorevler/)
    expect(view).toMatch(/ajanda\.tabHatirlaticilar/)
    expect(view).toMatch(/raspel_ajanda_tab/)
    expect(sistemApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/ajanda\/ozet/)
    expect(view).toMatch(/ajandaAPI\.ozet\(\)/)
  })

  it('ozet sirket yoksa sıfır döner', () => {
    expect(service).toMatch(/if \(sirketId == null\) \{[\s\S]*?AjandaOzetDTO\.builder\(\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'altBaslik',
      'kpiBugun',
      'kpiBuAyVade',
      'kpiBekleyen',
      'kpiGecikmis',
      'tabTakvim',
      'tabYaklasan',
      'tabGorevler',
      'tabHatirlaticilar'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
