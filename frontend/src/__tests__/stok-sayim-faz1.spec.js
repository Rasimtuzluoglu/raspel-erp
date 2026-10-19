import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Stok Sayımı revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI + AppDataTable + durum yerelleştirme).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSayim.vue')
const stokApi = oku('api/modules/stok.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/envanter/StokSayimController.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSayimService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokSayimRepository.java'),
  'utf8'
)

describe('StokSayim — Faz 1', () => {
  it('PageHeader subtitle + yenile', () => {
    expect(view).toMatch(/:subtitle="t\('stokSayim\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/stokSayim\.kpiToplam/)
    expect(view).toMatch(/stokSayimAPI\.ozet\(\)/)
    expect(stokApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/stok-sayim\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/public com\.raspel\.erp\.dto\.envanter\.StokSayimOzetDTO ozet/)
    expect(repo).toMatch(/countFarkliTamamlanan/)
  })

  it('AppDataTable kullanımı', () => {
    expect(view).toMatch(/<AppDataTable/)
    expect(view).toMatch(/aramaPlaceholder/)
  })

  it('durum yerelleştirme', () => {
    expect(view).toMatch(/durumLabel\(data\.durum, t\)/)
    expect(view).toMatch(/const durumSeverity =/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiToplam', 'kpiTaslak', 'kpiTamamlanan', 'kpiFarkli', 'kpiBugun', 'aramaPlaceholder', 'veriYok']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
