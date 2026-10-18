import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Denetim Logları revizyonu — kaynak-tabanlı doğrulama
 * (KPI + toolbar/popover filtreler + arama + yenileme + mobil kart +
 *  riskli vurgu + zengin detay dialog).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const denetim = oku('views/Denetim.vue')
const api = oku('api/modules/sistem.js')

describe('Denetim Logları revizyonu', () => {
  it('KPI özeti için /audit-log/ozet ucu kullanılır', () => {
    expect(api).toMatch(/ozet\(params\)\s*\{[\s\S]*?\/audit-log\/ozet/)
    expect(denetim).toMatch(/auditLogAPI\.ozet\(/)
    expect(denetim).toMatch(/<KpiKart/)
    expect(denetim).toMatch(/const ozetIslem/)
  })

  it('serbest metin arama + filtre popover + riskli filtresi', () => {
    expect(denetim).toMatch(/const aramaMetni/)
    expect(denetim).toMatch(/<Popover ref="filtrePanel"/)
    expect(denetim).toMatch(/filtrePanel\?\.toggle/)
    expect(denetim).toMatch(/const riskli = ref\(false\)/)
    expect(denetim).toMatch(/params\.riskli = true/)
    // Arama backend ile tutarli: yalnizca 2+ karakterde gonderilir.
    expect(denetim).toMatch(/q\.length >= 2/)
  })

  it('manuel ve periyodik yenileme (otomatik yenile)', () => {
    expect(denetim).toMatch(/const yenile =/)
    expect(denetim).toMatch(/otomatikYenile/)
    expect(denetim).toMatch(/OTOMATIK_ANAHTAR/)
    expect(denetim).toMatch(/setInterval\(yenile, 30000\)/)
    expect(denetim).toMatch(/clearInterval\(otomatikTimer\)/)
  })

  it('mobil kart görünümü ve gösterim tercihi', () => {
    expect(denetim).toMatch(/const gosterim = ref/)
    expect(denetim).toMatch(/class="denetim-kartlar"/)
    expect(denetim).toMatch(/GORUNUM_ANAHTAR/)
    expect(denetim).toMatch(/matchMedia\('\(max-width: 768px\)'\)/)
  })

  it('riskli (SIL/HATA) kayıtlar vurgulanır + entity#id çipi', () => {
    expect(denetim).toMatch(/const riskliMi = \(l\)/)
    expect(denetim).toMatch(/riskli-satir/)
    expect(denetim).toMatch(/class="entity-cip"/)
    // Ayrı entityId kolonu kaldırıldı; tek çipte birleşti.
    expect(denetim).not.toMatch(/field="entityId"/)
  })

  it('zengin detay dialog: JSON bloğu + kopyala + katla', () => {
    expect(denetim).toMatch(/detay-json-blok/)
    expect(denetim).toMatch(/usePanoyaKopyala/)
    expect(denetim).toMatch(/const detayAcik = ref/)
    expect(denetim).toMatch(/const detayOzet = computed/)
  })

  it('ARSIVLE / ARSIVLE_GERI_AL işlem kodları çeviride', () => {
    expect(denetim).toMatch(/ARSIVLE: t\('denetim\.islemArsivle'\)/)
    expect(denetim).toMatch(/ARSIVLE_GERI_AL: t\('denetim\.islemArsivleGeriAl'\)/)
  })
})
