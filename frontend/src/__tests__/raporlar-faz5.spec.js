import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Raporlar revizyonu — kaynak-tabanlı doğrulama
 * (PageHeader + sol dikey menü + ortak toolbar + EmptyState + responsive +
 *  iskelet + arama/a11y).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const raporlar = oku('views/Raporlar.vue')
const araclar = oku('components/RaporAraclari.vue')

describe('Raporlar revizyonu', () => {
  it('PageHeader + sol dikey rapor menüsü (native şerit gizli)', () => {
    expect(raporlar).toMatch(/<PageHeader/)
    expect(raporlar).toMatch(/const RAPOR_NAV_GRUPLARI = computed/)
    expect(raporlar).toMatch(/class="raporlar-nav"/)
    expect(raporlar).toMatch(/\.p-tabview-tablist-container\)\s*\{[\s\S]{0,40}display: none/)
  })

  it('ortak rapor araç çubuğu tüm sekmelerde', () => {
    expect(raporlar).toMatch(/import RaporAraclari from '\.\.\/components\/RaporAraclari\.vue'/)
    const sayi = (raporlar.match(/<RaporAraclari/g) || []).length
    expect(sayi).toBe(10)
    expect(araclar).toMatch(/favori-degistir/)
    expect(araclar).toMatch(/raporlar\.pdf/)
    expect(araclar).toMatch(/raporlar\.excelIndir/)
    expect(araclar).toMatch(/raporlar\.epostaGonder/)
  })

  it('boş durumlar EmptyState (Message boş blokları kaldırıldı)', () => {
    expect(raporlar).toMatch(/<EmptyState/)
    // Eski boş-durum Message blokları artık EmptyState.
    expect(raporlar).toMatch(/EmptyState[\s\S]{0,160}raporlar\.hareketYok/)
    expect(raporlar).toMatch(/EmptyState[\s\S]{0,160}raporlar\.donemdeSatisYok/)
  })

  it('yükleme sırasında iskelet gösterilir', () => {
    expect(raporlar).toMatch(/<SkeletonLoader/)
    for (const y of ['ekstreLoading', 'ggLoading', 'kdvLoading', 'yasLoading', 'ckLoading']) {
      expect(raporlar).toMatch(new RegExp(`v-if="${y}"`))
    }
  })

  it('responsive: filtre/grafik/tablo uyarlanır', () => {
    expect(raporlar).toMatch(/@media \(max-width: 640px\)/)
    expect(raporlar).toMatch(/\.rapor-filtre \{[\s\S]{0,60}flex-direction: column/)
    expect(raporlar).toMatch(/p-datatable-table-container\)\s*\{[\s\S]{0,40}overflow-x: auto/)
  })

  it('arama: ikon/grup + boş sonuç; a11y favori', () => {
    expect(raporlar).toMatch(/<template #option="slotProps">/)
    expect(raporlar).toMatch(/raporlar\.aramaBos/)
    expect(raporlar).toMatch(/role="button"/)
    expect(raporlar).toMatch(/:aria-pressed="raporFavori\(b\.key\)/)
  })
})
