import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * İrsaliye revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + 4 KPI + sekmeler + sunucu filtreleri + hafıza + iskelet + erişilebilirlik).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Irsaliyeler.vue')
const ticaretApi = oku('api/modules/ticaret.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('İrsaliye — Faz 1 (liste + KPI + filtre)', () => {
  it('sayfa iskeleti: PageHeader, Excel/Yeni aksiyonları ve 4 KPI', () => {
    expect(view).toMatch(/<PageHeader/)
    expect(view).toMatch(/irsaliyeler\.altBaslik/)
    expect(view).toMatch(/icon="pi pi-truck"/)
    expect(view).toMatch(/irsaliyeler\.excel/)
    expect(view).toMatch(/irsaliyeler\.yeniIrsaliye/)
    expect(view.match(/<KpiKart/g)).toHaveLength(4)
    for (const anahtar of ['kpiHazirlanan', 'kpiSevkEdilen', 'kpiTeslimEdilen', 'kpiFaturasiz']) {
      expect(view).toContain(`irsaliyeler.${anahtar}`)
    }
  })

  it('sekmeler: Tümü / Hazırlanan / Sevk / Teslim / Faturalanan ve varsayılan Hazırlanan', () => {
    expect(view).toMatch(/<TabView v-model:active-index="aktifSekme">/)
    for (const anahtar of ['tumu', 'tabHazirlanan', 'tabSevkEdilen', 'tabTeslimEdilen', 'tabFaturalanan']) {
      expect(view).toContain(`irsaliyeler.${anahtar}`)
    }
    expect(view).toMatch(/gecerliSekmeler\.includes\(deger\) \? deger : 1/)
    expect(view).toMatch(/'HAZIRLANDI', 'SEVK_EDILDI', 'TESLIM_EDILDI', null/)
  })

  it('sunucu taraflı liste: filtreli + sıralama + sayfalama', () => {
    expect(view).toMatch(/irsaliyeAPI\.filtreli\(/)
    expect(view).toMatch(/const filtreParams = \(\) =>/)
    expect(view).toMatch(/const sayfaDegisti = \(e\) =>/)
    expect(view).toMatch(/const siralaDegisti = \(e\) =>/)
    expect(view).toMatch(/:total-records="toplam"/)
    expect(view).toMatch(/lazy/)
    expect(view).toMatch(/sort: sirala\.value/)
    expect(ticaretApi).toMatch(/filtreli\(params\)\s*\{[\s\S]*?\/irsaliyeler\/filtreli/)
    expect(ticaretApi).toMatch(/ozet\(params\)\s*\{[\s\S]*?\/irsaliyeler\/ozet/)
  })

  it('filtre çubuğu: arama, cari, tür, durum, tarih ve faturasız', () => {
    expect(view).toMatch(/irsaliyeler\.aramaPlaceholder/)
    expect(view).toMatch(/irsaliyeler\.cariHesap/)
    expect(view).toMatch(/turSecenekleri/)
    expect(view).toMatch(/durumSecenekleri/)
    expect(view).toMatch(/<DatePicker/)
    expect(view).toMatch(/faturasiz-cip/)
    expect(view).toMatch(/params\.faturali = false/)
    expect(view).toMatch(/aktifSekme\.value === 4\) params\.faturali = true/)
  })

  it('hafıza: sekme ve filtre anahtarları', () => {
    expect(view).toMatch(/const TAB_ANAHTAR = 'raspel_irsaliye_tab'/)
    expect(view).toMatch(/const FILTRE_ANAHTAR = 'raspel_irsaliye_filtre'/)
    expect(view).toMatch(/localStorage\.setItem\(TAB_ANAHTAR, String\(index\)\)/)
    expect(view).toMatch(/localStorage\.setItem\(\s*FILTRE_ANAHTAR/)
  })

  it('iskelet, mobil kart, aria-live ve aria-pressed', () => {
    expect(view).toMatch(/const ozetIskelet = computed/)
    expect(view).toMatch(/const listeIskelet = computed/)
    expect(view).toMatch(/<SkeletonLoader[\s\S]*?:count="6"/)
    expect(view).toMatch(/class="sr-only"\s+aria-live="polite"/)
    expect(view).toMatch(/:aria-pressed="aktifSekme === 1"/)
    expect(view).toMatch(/:aria-pressed="faturasiz"/)
    expect(view).toMatch(/class="irsaliye-mobil"/)
    expect(view).toMatch(/@media \(max-width: 768px\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'altBaslik',
      'excel',
      'kpiHazirlanan',
      'kpiSevkEdilen',
      'kpiTeslimEdilen',
      'kpiFaturasiz',
      'tumu',
      'tabHazirlanan',
      'tabSevkEdilen',
      'tabTeslimEdilen',
      'tabFaturalanan',
      'aramaPlaceholder',
      'faturasizFiltre',
      'filtreYok',
      'kayitDuyuru'
    ]) {
      expect(tr).toContain(`"${anahtar}"`)
      expect(en).toContain(`"${anahtar}"`)
    }
  })
})
