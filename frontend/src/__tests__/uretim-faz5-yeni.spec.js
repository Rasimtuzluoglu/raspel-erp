import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Üretim revizyonu — Faz 5 kaynak-tabanlı doğrulama (yeni)
 * (üretim takvimi / Gantt).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Uretim.vue')
const stokApi = oku('api/modules/stok.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/envanter/UretimController.java')
const service = okuB('service/envanter/UretimService.java')
const repo = okuB('repository/envanter/UretimEmriRepository.java')

describe('Üretim — Faz 5 (takvim)', () => {
  it('backend: takvim uç noktası ve sorgu', () => {
    expect(controller).toMatch(/@GetMapping\("\/takvim"\)/)
    expect(service).toMatch(/public List<UretimEmriDTO> takvim\(Long sirketId, LocalDate baslangic, LocalDate bitis\)/)
    expect(repo).toMatch(/takvimAralikta\(@Param\("sirketId"\) Long sirketId/)
    expect(repo).toMatch(/e\.planlananBaslangic <= :bitis AND e\.planlananBitis >= :baslangic/)
  })

  it('frontend: aylık Gantt ızgarası ve çubuk hesabı', () => {
    expect(view).toMatch(/const takvimAy = ref/)
    expect(view).toMatch(/const cubukHesapla = \(e\) =>/)
    expect(view).toMatch(/const takvimSatirlari = computed/)
    expect(view).toMatch(/class="gantt"/)
    expect(view).toMatch(/class="gantt-cubuk"/)
    expect(view).toMatch(/const takvimAyDegistir = \(delta\) =>/)
    expect(view).toMatch(/const takvimBugun = \(\) =>/)
    expect(stokApi).toMatch(/takvim\(params = \{\}\)\s*\{[\s\S]*?\/uretim\/takvim/)
  })

  it('erişilebilirlik: liste semantiği ve çubuk etiketi', () => {
    expect(view).toMatch(/role="list"/)
    expect(view).toMatch(/role="listitem"/)
    expect(view).toMatch(/:aria-label="ganttAria\(satir\)"/)
    expect(view).toMatch(/const ganttAria = \(satir\) =>/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'takvimTab',
      'bugun',
      'oncekiAy',
      'sonrakiAy',
      'takvimLejant',
      'takvimYok',
      'takvimYuklenemedi',
      'hatsiz',
      'ganttAria'
    ]) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
