import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * 360° Görünümler revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader aksiyonu + sekme hatırlama/rozet + KPI düzeltmeleri).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Gorunumler360.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Gorunumler360 — Faz 1', () => {
  it('PageHeader ikon + yenile aksiyonu', () => {
    expect(view).toMatch(/icon="pi pi-compass"/)
    expect(view).toMatch(/<template #actions>/)
    expect(view).toMatch(/common\.refresh/)
    expect(view).toMatch(/@click="yukle"/)
  })

  it('sekme hatırlama + aktif sekme rozetleri', () => {
    expect(view).toMatch(/v-model:active-index="aktifSekme"/)
    expect(view).toMatch(/raspel_gorunum360_tab/)
    expect(view).toMatch(/localStorage\.setItem\(AKTIF_SEKME_ANAHTAR/)
    expect(view).toMatch(/class="tab-rozet"/)
  })

  it('KPI düzeltmeleri: suffix kaldırıldı, #deger slotu + ikonlar', () => {
    expect(view).not.toMatch(/suffix="%"/)
    expect(view).toMatch(/<template #deger>/)
    expect(view).toMatch(/ikon="pi pi-dollar"/)
    expect(view).toMatch(/ikon="pi pi-percentage"/)
  })

  it('yeni alanlar: toplamUrun + tamamlanma oranı + teslim tutarı', () => {
    expect(view).toMatch(/gorunum360\.toplamUrun/)
    expect(view).toMatch(/const tamamlanmaOrani = computed/)
    expect(view).toMatch(/gorunum360\.tamamlanmaOrani/)
    expect(view).toMatch(/calisan-tutar/)
    expect(view).toMatch(/formatCurrency\(c\.teslimTutari\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of ['toplamUrun', 'tamamlanmaOrani']) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
