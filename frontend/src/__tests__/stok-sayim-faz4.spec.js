import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Stok Sayımı revizyonu — Faz 4 kaynak-tabanlı doğrulama
 * (aynı gün tarama + mobil kart + skeleton + a11y).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSayim.vue')

const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSayimService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokSayimRepository.java'),
  'utf8'
)

describe('StokSayim — Faz 4', () => {
  it('aynı gün TASLAK taraması', () => {
    expect(repo).toMatch(/findFirstBySirketIdAndStokIdAndDurumAndTarihOrderByOlusturmaTarihiDesc/)
    expect(service).toMatch(/findFirstBySirketIdAndStokIdAndDurumAndTarihOrderByOlusturmaTarihiDesc/)
    expect(service).toMatch(/java\.time\.LocalDate bugun = java\.time\.LocalDate\.now\(\)/)
  })

  it('mobil kart görünümü + masa/mobil geçişi', () => {
    expect(view).toMatch(/class="sayim-mobil"/)
    expect(view).toMatch(/sayim-mobil-kart/)
    expect(view).toMatch(/sayim-masa/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
  })

  it('skeleton + a11y + reduced-motion', () => {
    expect(view).toMatch(/<SkeletonLoader/)
    expect(view).toMatch(/role="status"/)
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/@media \(prefers-reduced-motion: reduce\)/)
  })
})
