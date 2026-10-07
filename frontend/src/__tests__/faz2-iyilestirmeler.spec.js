import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Faz2 — Stok grubu, Sipariş takip, PDF/yazıcı, global yoğunluk.
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const appCss = readFileSync(join(SRC, 'assets/app.css'), 'utf8')

describe('Faz2 — Stok grubu filtresi', () => {
  const stoklar = oku('views/Stoklar.vue')
  it('filtre AutoComplete yerine yalnız mevcut değerli Dropdown', () => {
    expect(stoklar).toMatch(/v-model="filtreStokGrubu"[\s\S]{0,200}:options="stokGruplari"/)
    expect(stoklar).not.toMatch(/:suggestions="stokGrubuOnerileri"/)
    expect(stoklar).not.toMatch(/const\s+stokGrubuOnerileri/)
  })
  it('toplu fiyat diyaloğuna filtresiz grup listesi geçer', () => {
    expect(stoklar).toMatch(/:gruplar="stokGruplari"/)
  })
})

describe('Faz2 — Sipariş takip revizyonu', () => {
  const takip = oku('views/SiparisTakip.vue')
  it('durum filtresi + sayfalama + şoför ucu var', () => {
    expect(takip).toMatch(/v-model="filtreDurum"/)
    expect(takip).toMatch(/<Paginator/)
    expect(takip).toMatch(/siparisTakipAPI\.soforler/)
  })
  it('durum kodları çevrilir; ham enum gösterilmez', () => {
    expect(takip).toMatch(/const durumEtiket/)
    expect(takip).toMatch(/siparisTakip\.durum\./)
  })
  it('sayfalı yanıt (content/totalElements) okunur', () => {
    expect(takip).toMatch(/r\.data\?\.content/)
    expect(takip).toMatch(/totalElements/)
  })
  it('"tamam" yalnız gerçekten tamamlanan durumlar için', () => {
    expect(takip).toMatch(/const uretimTamam\s*=\s*\(s\)\s*=>\s*s\.uretimDurum === 'TAMAMLANDI'/)
    expect(takip).toMatch(/const sevkTamam/)
  })
})

describe('Faz2 — PDF / yazıcı', () => {
  it('termal fiş stili @page içerir (80/58mm)', () => {
    const posFis = oku('utils/posFis.js')
    expect(posFis).toMatch(/@page\s*\{\s*size:\s*\$\{[^}]*\}\s*auto;\s*margin:\s*0;/)
  })
  it('fiş penceresi görselleri bekleterek yazdırır', () => {
    const fisYazdir = oku('utils/fisYazdir.js')
    expect(fisYazdir).toMatch(/doc\.images/)
    expect(fisYazdir).toMatch(/resimleriBekleVeYazdir/)
  })
  it('fatura A4 yazdırması @page + satır kırılma koruması içerir', () => {
    const faturaDetay = oku('views/FaturaDetay.vue')
    expect(faturaDetay).toMatch(/@page\s*\{[\s\S]*?size:\s*A4/)
    expect(faturaDetay).toMatch(/break-inside:\s*avoid/)
    expect(faturaDetay).toMatch(/display:\s*table-header-group/)
  })
  it('teklif yazdırma @page kuralı @media print içinde (global sızıntı yok)', () => {
    const teklifler = oku('views/Teklifler.vue')
    // @page yalnız print blogunun içinde olmalı
    const printIdx = teklifler.indexOf('@media print')
    const pageIdx = teklifler.indexOf('@page')
    expect(pageIdx).toBeGreaterThan(printIdx)
  })
})

describe('Faz2 — Global yoğunluk', () => {
  it('main-content ve tablo yoğunluğu kompakt', () => {
    const mainIdx = appCss.indexOf('.main-content {')
    expect(appCss.slice(mainIdx, mainIdx + 200)).toMatch(/padding:\s*14px 18px 24px/)
    expect(appCss).toMatch(/\.p-datatable \.p-datatable-tbody > tr > td \{[^}]*padding:\s*8px 10px/)
  })
  it('dokunma hedefleri yalnız kaba işaretçide', () => {
    expect(appCss).toMatch(/@media \(max-width: 900px\) and \(hover: none\) and \(pointer: coarse\)/)
  })
  it('dashboard başlığı global 24px ile hizalı', () => {
    const dash = readFileSync(join(SRC, 'assets/dashboard.css'), 'utf8')
    expect(dash).toMatch(/\.dashboard-header h1 \{[^}]*font-size:\s*24px/)
  })
})
