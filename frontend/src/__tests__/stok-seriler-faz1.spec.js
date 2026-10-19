import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Seri/Lot Takibi revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + KPI + AppDataTable + durum etiketleri).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSeriler.vue')
const stokApi = oku('api/modules/stok.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/controller/envanter/StokSeriController.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSeriService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokSeriRepository.java'),
  'utf8'
)

describe('StokSeriler — Faz 1', () => {
  it('PageHeader subtitle + yenile', () => {
    expect(view).toMatch(/:subtitle="t\('stokSeriler\.aciklama'\)"/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
  })

  it('KPI kartları + ozet uçları', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/stokSeriler\.kpiToplam/)
    expect(view).toMatch(/stokSeriAPI\.ozet\(30\)/)
    expect(stokApi).toMatch(/ozet\(gun = 30\)\s*\{[\s\S]*?\/stok-seri\/ozet/)
    expect(controller).toMatch(/@GetMapping\("\/ozet"\)/)
    expect(service).toMatch(/StokSeriOzetDTO ozet/)
    expect(repo).toMatch(/countSktYaklasan/)
    expect(repo).toMatch(/countSktGecmis/)
  })

  it('AppDataTable kullanımı', () => {
    expect(view).toMatch(/<AppDataTable/)
    expect(view).toMatch(/aramaPlaceholder/)
  })

  it('durum etiketi + sayı biçimlendirme', () => {
    expect(view).toMatch(/durumTuketildi/)
    expect(view).toMatch(/formatNumber\(data\.miktar\)/)
    expect(view).toMatch(/formatNumber\(data\.kalanMiktar\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['aciklama', 'kpiToplam', 'kpiStokta', 'kpiTuketildi', 'kpiSktYaklasan', 'kpiSktGecmis', 'aramaPlaceholder', 'veriYok']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
