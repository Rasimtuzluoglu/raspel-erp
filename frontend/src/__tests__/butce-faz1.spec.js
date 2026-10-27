import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Bütçe revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI + sunucu filtreleri + hafıza + iskelet + erişilebilirlik).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Butceler.vue')
const finansApi = oku('api/modules/finans.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/finans/ButceController.java')
const service = okuB('service/finans/ButceService.java')
const repo = okuB('repository/finans/ButceRepository.java')
const hareketRepo = okuB('repository/finans/HareketRepository.java')

describe('Bütçe — Faz 1', () => {
  it('sayfa iskeleti: PageHeader + 4 KPI + filtre çubuğu', () => {
    expect(view).toMatch(/<PageHeader/)
    expect(view).toMatch(/butceler\.altBaslik/)
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/butceler\.kpiButce/)
    expect(view).toMatch(/butceler\.kpiGerceklesen/)
    expect(view).toMatch(/butceler\.kpiSapma/)
    expect(view).toMatch(/butceler\.kpiKullanim/)
    expect(view).toMatch(/class="tur-seg"/)
  })

  it('sunucu taraflı liste + sıralama + sayfalama', () => {
    expect(view).toMatch(/butceAPI\.filtreli\(/)
    expect(view).toMatch(/const sayfaDegisti = \(e\) =>/)
    expect(view).toMatch(/const siralaDegisti = \(e\) =>/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(finansApi).toMatch(/filtreli\(params = \{\}\)\s*\{[\s\S]*?\/butceler\/filtreli/)
    expect(finansApi).toMatch(/ozet\(params = \{\}\)\s*\{[\s\S]*?\/butceler\/ozet/)
    expect(finansApi).toMatch(/kategoriler\(params = \{\}\)\s*\{[\s\S]*?\/butceler\/kategoriler/)
  })

  it('backend: filtreli/ozet/kategoriler ve tür ayrımlı gerçekleşen', () => {
    expect(controller).toMatch(/@GetMapping\("\/filtreli"\)/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(controller).toMatch(/@GetMapping\("\/kategoriler"\)/)
    expect(service).toMatch(/public Page<ButceDTO> filtreli\(/)
    expect(service).toMatch(/public ButceOzetDTO ozet\(Long sirketId, Integer yil, String tur\)/)
    expect(service).toMatch(/Hareket\.HareketTuru\.TAHSILAT/)
    expect(service).toMatch(/masrafRepository\.sumTutarBySirketIdAndTarihBetween/)
    expect(repo).toMatch(/Page<Butce> filtreliGetir\(/)
    expect(hareketRepo).toMatch(/sumTutarBySirketIdAndTurAndTarihBetween/)
  })

  it('hafıza + iskelet + erişilebilirlik', () => {
    expect(view).toMatch(/const FILTRE_ANAHTAR = 'raspel_butce_filtre'/)
    expect(view).toMatch(/localStorage\.setItem\(\s*FILTRE_ANAHTAR/)
    expect(view).toMatch(/const ozetIskelet = computed/)
    expect(view).toMatch(/<SkeletonLoader[\s\S]*?:count="6"/)
    expect(view).toMatch(/class="sr-only"\s+aria-live="polite"/)
    expect(view).toMatch(/:aria-pressed="tur === secenek\.value"/)
    expect(view).toMatch(/aria-label="t\('butceler\.tur'\)"/)
    expect(view).toMatch(/:breakpoints="\{ '575px': '92vw' \}"/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'altBaslik',
      'kpiButce',
      'kpiGerceklesen',
      'kpiSapma',
      'kpiKullanim',
      'tumu',
      'donem',
      'aramaPlaceholder',
      'filtreYok',
      'kayitDuyuru'
    ]) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
