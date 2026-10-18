import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Sipariş Takibi revizyonu — kaynak-tabanlı doğrulama
 * (KPI + hızlı filtre çipleri + EmptyState + sıralama/sayfalama + timeline +
 *  Drawer detay + Excel).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const takip = oku('views/SiparisTakip.vue')
const drawer = oku('components/SiparisTakipDetayDrawer.vue')
const ticaretApi = oku('api/modules/ticaret.js')
const dosyaApi = oku('api/modules/dosya.js')

describe('Sipariş Takibi revizyonu', () => {
  it('KPI özeti (/siparis-takip/ozet) + PageHeader', () => {
    expect(takip).toMatch(/<PageHeader/)
    expect(takip).toMatch(/<KpiKart/)
    expect(ticaretApi).toMatch(/ozet\(\)\s*\{[\s\S]*?\/siparis-takip\/ozet/)
    expect(takip).toMatch(/siparisTakipAPI\.ozet\(/)
  })

  it('hızlı filtre çipleri + sunucu parametreleri', () => {
    expect(takip).toMatch(/const hizliCiplar = computed/)
    expect(takip).toMatch(/const hizliFiltreUygula/)
    expect(takip).toMatch(/class="takip-cipler"/)
    expect(takip).toMatch(/params\.geciken = true/)
    expect(takip).toMatch(/params\.uretimde = true/)
  })

  it('EmptyState + sıralama + sayfa boyutu hatırlama', () => {
    expect(takip).toMatch(/<EmptyState/)
    expect(takip).toMatch(/const siralamaSecenekleri = computed/)
    expect(takip).toMatch(/SAYFA_BOYUTU_ANAHTAR/)
    expect(takip).toMatch(/localStorage\.setItem\(SAYFA_BOYUTU_ANAHTAR/)
  })

  it('adım timeline (tamam ✓) + Drawer detay', () => {
    expect(takip).toMatch(/<SiparisTakipDetayDrawer/)
    expect(takip).toMatch(/const satirAc/)
    expect(takip).toMatch(/pi pi-check' : 'pi pi-file'/)
    expect(drawer).toMatch(/zaman-cizelgesi/)
    expect(drawer).toMatch(/const adimlar = computed/)
    expect(drawer).toMatch(/gecikme-uyari/)
  })

  it('Excel export (uç + bildirim)', () => {
    expect(dosyaApi).toMatch(/siparisTakip\(params\)\s*\{[\s\S]*?\/exports\/siparis-takip/)
    expect(takip).toMatch(/excelYukleniyor/)
    expect(takip).toMatch(/excelAPI\.siparisTakip\(/)
    expect(takip).toMatch(/siparisTakip\.excelIndirildi/)
    expect(takip).toMatch(/siparisTakip\.excelHata/)
  })

  it('filtre parametreleri liste ve Excel için ortak', () => {
    expect(takip).toMatch(/const filtreParams = \(\)/)
    // Excel de ayni filtreleri gonderir.
    expect(takip).toMatch(/excelAPI\.siparisTakip\(filtreParams\(\)\)/)
  })
})
