import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Taksit Takvimi revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (plan özetleri/detay/silme + kalem detayı + plan önizleme).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/TaksitTakvimi.vue')
const detayDrawer = oku('components/TaksitDetayDrawer.vue')
const planDrawer = oku('components/TaksitPlanDrawer.vue')
const finansApi = oku('api/modules/finans.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/finans/TaksitController.java')
const service = okuB('service/finans/TaksitService.java')
const repo = okuB('repository/finans/TaksitRepository.java')

describe('Taksit Takvimi — Faz 3', () => {
  it('backend: plan özet/detay/silme uçları', () => {
    expect(controller).toMatch(/@GetMapping\("\/planlar"\)/)
    expect(controller).toMatch(/@GetMapping\("\/plan\/\{planNo\}"\)/)
    expect(controller).toMatch(/@DeleteMapping\("\/plan\/\{planNo\}"\)/)
    expect(service).toMatch(/TaksitPlanOzetDTO> planlar\(Long sirketId, String q\)/)
    expect(service).toMatch(/List<TaksitDTO> planDetay\(Long sirketId, String planNo\)/)
    expect(service).toMatch(/Taksit planı bulunamadı/)
    expect(repo).toMatch(/planOzetleri/)
    expect(repo).toMatch(/findBySirketIdAndPlanNoOrderByTaksitNoAsc/)
    expect(repo).toMatch(/DELETE|deleteByPlanNoAndSirketId/)
  })

  it('frontend: plan sekmesi + plan çekmecesi', () => {
    expect(view).toMatch(/taksitAPI\.planlar/)
    expect(view).toMatch(/const planAksiyonlari = \(p\)/)
    expect(view).toMatch(/const planSilOnay = \(p\)/)
    expect(view).toMatch(/taksitAPI\.planSil\(p\.planNo\)/)
    expect(view).toMatch(/<TaksitPlanDrawer/)
    expect(view).toMatch(/planYuzde/)
    expect(finansApi).toMatch(/planlar\(params\)\s*\{[\s\S]*?\/taksitler\/planlar/)
    expect(finansApi).toMatch(/planDetay\(planNo\)/)
    expect(finansApi).toMatch(/planSil\(planNo\)/)
  })

  it('frontend: kalem detay çekmecesi + plan önizleme', () => {
    expect(view).toMatch(/<TaksitDetayDrawer/)
    expect(view).toMatch(/const kalemDetay = \(k\)/)
    expect(view).toMatch(/detayDrawer\.value = true/)
    expect(view).toMatch(/taksitBasiTutar/)
    expect(view).toMatch(/sonTaksitTutar/)
    expect(detayDrawer).toMatch(/kalem\.gecikmeGunu/)
    expect(planDrawer).toMatch(/kalemler/)
    expect(planDrawer).toMatch(/ilerleme-bar/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'planNo', 'planYok', 'odenenTutar', 'kalan', 'ilkVade', 'sonVade', 'taksitler', 'planSil']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
