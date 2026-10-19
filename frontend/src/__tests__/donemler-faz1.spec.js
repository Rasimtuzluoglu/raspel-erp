import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Dönemler revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (KpiKart + EmptyState + SatirEylemleri + Güncel Dönem + responsive diyalog).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Donemler.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Donemler — Faz 1', () => {
  it('PageHeader yenile aksiyonu', () => {
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yenile"/)
    expect(view).toMatch(/const yenile = async/)
  })

  it('KPI kartları KpiKart ile + güncel dönem', () => {
    expect(view).toMatch(/<KpiKart/)
    expect(view).toMatch(/donemler\.kpiGuncel/)
    expect(view).toMatch(/const guncelDonem = computed/)
    expect(view).toMatch(/donemler\.guncel/)
  })

  it('EmptyState + SatirEylemleri kullanımı', () => {
    expect(view).toMatch(/import EmptyState from/)
    expect(view).toMatch(/<EmptyState/)
    expect(view).toMatch(/import SatirEylemleri from/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const donemAksiyonlari = /)
  })

  it('responsive diyaloglar + yeni dönem çağrısı', () => {
    expect((view.match(/:breakpoints=/g) || []).length).toBeGreaterThanOrEqual(2)
    expect(view).toMatch(/@click="dialogAc\(\)"/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of ['kpiGuncel', 'guncel']) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
