import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Teslimatlar revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + KPI + EmptyState + şoför arama + ⋮ menü + imza zorunluluğu +
 *  fiş blob + Excel + filtre/sıralama + mobil).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const teslimat = oku('views/Teslimatlar.vue')
const ticaretApi = oku('api/modules/ticaret.js')
const dosyaApi = oku('api/modules/dosya.js')

describe('Teslimatlar revizyonu', () => {
  it('PageHeader + KPI özeti (/deliveries/ozet)', () => {
    expect(teslimat).toMatch(/<PageHeader/)
    expect(teslimat).toMatch(/<KpiKart/)
    expect(ticaretApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/deliveries\/ozet/)
    expect(teslimat).toMatch(/teslimatAPI\.ozet\(/)
  })

  it('EmptyState + şoför arama', () => {
    expect(teslimat).toMatch(/<EmptyState/)
    expect(teslimat).toMatch(/teslimatlar\.soforBulunamadi/)
    expect(teslimat).toMatch(/const surucuArama = ref/)
    expect(teslimat).toMatch(/const filtreliSuruculer = computed/)
  })

  it('kart ⋮ menüsü (SatirEylemleri + teslimMenuItems)', () => {
    expect(teslimat).toMatch(/<SatirEylemleri/)
    expect(teslimat).toMatch(/const teslimMenuItems/)
    expect(teslimat).toMatch(/duzenle: false, sil: false/)
  })

  it('imza zorunluluğu: TESLIM_EDILDI imza modalını açar', () => {
    expect(teslimat).toMatch(/const durumSecildi/)
    expect(teslimat).toMatch(/if \(d === 'TESLIM_EDILDI'\)/)
    expect(teslimat).toMatch(/@update:model-value="\(d\) => durumSecildi\(teslim, d\)"/)
  })

  it('fiş blob ile açılır (401 düzeltmesi)', () => {
    expect(teslimat).toMatch(/teslimatAPI\.fis\(teslim\.id\)/)
    expect(teslimat).not.toMatch(/window\.open\(`\/api\/deliveries/)
  })

  it('Excel export (uç + bildirim)', () => {
    expect(dosyaApi).toMatch(/teslimatlar\(params\)\s*\{[\s\S]*?\/exports\/teslimatlar/)
    expect(teslimat).toMatch(/excelYukleniyor/)
    expect(teslimat).toMatch(/excelAPI\.teslimatlar\(/)
    expect(teslimat).toMatch(/teslimatlar\.excelIndirildi/)
    expect(teslimat).toMatch(/teslimatlar\.excelHata/)
  })

  it('filtre çipleri + sıralama + mobil yığma', () => {
    expect(teslimat).toMatch(/filtre === 'TESLIM'/)
    expect(teslimat).toMatch(/const siralamaSecenekleri = computed/)
    expect(teslimat).toMatch(/@media \(max-width: 768px\)/)
    expect(teslimat).toMatch(/\.teslimat-duzen \{[\s\S]*?flex-direction: column/)
  })
})
