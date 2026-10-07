import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * REDTEAM/Faz10 — "Yeni Satis" penceresi iyilestirmesi (8 sorun).
 *
 *  1) Diyalog icerigi iceride kaydirilamiyor / footer asagi kayiyordu.
 *  2) Hizli ekle satiri etiketsiz ve hizasizdi (placeholder kaybolunca ne
 *     oldugu anlasilmiyordu).
 *  3) Barkod ikonu input metninin uzerine biniyordu.
 *  4) Bos durum (EmptyState) diyalogda orantisi kacikti, cok yer kapliyordu.
 *  5) Ozet tam genislikte ve solda duruyordu.
 *  6) Birincil buton pasifken NEDEN'i gorunmuyordu.
 *  7) Iki farkli vurgu rengi (indigo/yesil) teal ile karisiyordu.
 *  8) Diyalog acilinca imlec barkod alanina gelmiyordu.
 */

const KOK = join(process.cwd(), 'src')
const satisKod = readFileSync(join(KOK, 'views/Satis.vue'), 'utf8')
const kalemKod = readFileSync(join(KOK, 'components/FaturaKalemleri.vue'), 'utf8')
const dialogKod = readFileSync(join(KOK, 'components/AppDialog.vue'), 'utf8')
const mainKod = readFileSync(join(KOK, 'main.js'), 'utf8')
const tailwindKod = readFileSync(join(process.cwd(), 'tailwind.config.cjs'), 'utf8')
const appCss = readFileSync(join(KOK, 'assets/app.css'), 'utf8')

describe('Faz10.1 — TEK vurgu rengi (teal)', () => {
  it('PrimeVue primary = teal (#14b8a6)', () => {
    expect(mainKod).toMatch(/primary:\s*\{[\s\S]*?500:\s*'#14b8a6'/)
  })

  it('Tailwind brand = teal (#14b8a6)', () => {
    expect(tailwindKod).toMatch(/brand:\s*\{[\s\S]*?500:\s*'#14b8a6'/)
  })

  it('app.css aksan teal', () => {
    expect(appCss).toMatch(/--accent:\s*#14b8a6/i)
  })

  it('Satis/Kalemlerde `p-button-success` (ikinci vurgu) KALMADI', () => {
    expect(satisKod).not.toMatch(/p-button-success/)
    expect(kalemKod).not.toMatch(/p-button-success/)
  })

  it('Satis KPI renkleri teal ailesinde (kalan/alacak kirmizisi haric)', () => {
    const kpiRenkler = [...satisKod.matchAll(/renk="#([0-9a-fA-F]{6})"/g)].map((m) => m[1].toLowerCase())
    expect(kpiRenkler).toContain('14b8a6')
    // Mavi (#3b82f6) ve yesil (#10b981) accent'leri kaldirildi.
    expect(kpiRenkler).not.toContain('3b82f6')
    expect(kpiRenkler).not.toContain('10b981')
  })
})

describe('Faz10.2 — Hizli ekle satiri etiketli ve tek sira', () => {
  const kural = (sinif) => {
    const idx = kalemKod.indexOf(`.${sinif} {`)
    return idx === -1 ? '' : kalemKod.slice(idx, kalemKod.indexOf('}', idx))
  }

  it('her alan `.hizli-alan` sarmalayicisinda', () => {
    const adet = (kalemKod.match(/class="hizli-alan[^"]*"/g) || []).length
    expect(adet).toBeGreaterThanOrEqual(6)
  })

  it('alanlarin uzerinde etiket var', () => {
    expect(kalemKod).toMatch(/\.hizli-alan\s*>\s*label/)
    expect(kalemKod).toMatch(/faturaKalemleri\.etiketBarkod/)
    expect(kalemKod).toMatch(/faturaKalemleri\.etiketStok/)
    expect(kalemKod).toMatch(/faturaKalemleri\.etiketAdet/)
    expect(kalemKod).toMatch(/faturaKalemleri\.etiketFiyat/)
    expect(kalemKod).toMatch(/faturaKalemleri\.etiketKdv/)
  })

  it('grid tek satir ve alt hizali (align-items:end)', () => {
    const grid = kural('hizli-kalem-grid')
    expect(grid).toMatch(/grid-template-columns/)
    expect(grid).toMatch(/align-items:\s*end/)
  })

  it('barkod artik tam satira kirilmiyor (grid-column:1/-1 YOK)', () => {
    const barkod = kural('hizli-barkod')
    expect(barkod).not.toMatch(/grid-column/)
  })

  it('barkod ikonu icin sol dolgu var (ikon metne binmez)', () => {
    expect(kalemKod).toMatch(/\.hizli-barkod :deep\(\.p-inputtext\)\s*\{[^}]*padding-left:\s*2\.25rem/)
  })
})

describe('Faz10.3 — Bos durum kompakt ve ozet sagda', () => {
  it('tablo ici bos durum KOMPAKT (global EmptyState kullanilmaz)', () => {
    expect(kalemKod).toMatch(/class="kalem-yok-tablo"/)
    expect(kalemKod).toMatch(/\.kalem-yok-tablo\s*\{/)
    expect(kalemKod).not.toMatch(/<EmptyState\s*\/>/)
  })

  it('ozet SAGDA ve kompakt', () => {
    const ozetIdx = kalemKod.indexOf('.summary-box {')
    const ozet = kalemKod.slice(ozetIdx, kalemKod.indexOf('}', ozetIdx))
    expect(ozet).toMatch(/margin-left:\s*auto/)
    expect(ozet).toMatch(/width:\s*fit-content/)
  })
})

describe('Faz10.4 — Footer her zaman gorunur + pasif NEDEN metni', () => {
  it('Satis diyalogu icerigi iceride kaydirilir (content-max-height:none)', () => {
    expect(satisKod).toMatch(/content-max-height="none"/)
  })

  it('AppDialog icerik alaninda overflow-y:auto tanimli', () => {
    expect(dialogKod).toMatch(/\.p-dialog-content[\s\S]*?overflow-y:\s*auto/)
  })

  it('pasif nedeni hesaplayan computed var', () => {
    expect(satisKod).toMatch(/const tamamlaNeden\s*=\s*computed/)
    expect(satisKod).toMatch(/const tamamlaAktif\s*=\s*computed/)
  })

  it('footer nedeni gorunur metin olarak basar ve butonu baglar', () => {
    expect(satisKod).toMatch(/class="footer-neden"/)
    expect(satisKod).toMatch(/:disabled="!tamamlaAktif"/)
    expect(satisKod).toMatch(/:title="tamamlaNeden/)
  })

  it('neden metni sola yaslanir (butonlar sagda kalir)', () => {
    const idx = satisKod.indexOf('.footer-neden {')
    const blok = satisKod.slice(idx, satisKod.indexOf('}', idx))
    expect(blok).toMatch(/margin-right:\s*auto/)
  })
})

describe('Faz10.5 — Barkod odagi', () => {
  it('barkod input `autofocus` tasir', () => {
    expect(kalemKod).toMatch(/ref="barkodInput"[\s\S]*?autofocus/)
  })

  it('bilesen mount olunca barkoda odaklanir', () => {
    expect(kalemKod).toMatch(/nextTick\(\(\)\s*=>\s*barkodInput\.value\?\.focus/)
  })
})
