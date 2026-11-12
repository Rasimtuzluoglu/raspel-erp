import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * FAZ 1 — Faturalar liste ekrani: PageHeader + KPI seridi + mobil kart.
 * (Kaynak duzeyinde sozlesme; agir view mount edilmez.)
 */

const src = readFileSync(join(process.cwd(), 'src/views/Faturalar.vue'), 'utf8')

describe('Faturalar liste ekrani — Faz1', () => {
  it('PageHeader kullanir (duz h1 yerine)', () => {
    expect(src).toContain('<PageHeader')
    expect(src).toContain("t('faturalar.title')")
    expect(src).not.toContain('<h1 class="page-title">')
  })

  it('KPI seridi fatura ozetinden beslenir', () => {
    expect(src).toContain('fatura-kpi')
    expect(src).toContain('faturaAPI.ozet')
    expect(src).toContain('faturaOzet')
    expect(src).toContain("t('faturalar.kpiCiro')")
  })

  it('kucuk ekran icin kart listesi icerir', () => {
    expect(src).toContain('fatura-kart-liste')
    expect(src).toContain('fatura-kart')
  })

  it('arama filtre cubuguna tasinmistir (Toolbar kaldirildi)', () => {
    expect(src).not.toContain('<Toolbar')
  })

  it('KPI ozeti tum aktif filtreleri gonderir', () => {
    expect(src).toContain('ozetYukle')
    for (const p of ['search', 'tur', 'durum', 'odemeDurumu', 'vadesiGecen', 'teslimatVar']) {
      expect(src).toContain(`p.${p}`)
    }
    expect(src).toMatch(/faturaAPI\.ozet\(p\)/)
  })

  it('arama ve filtre degisiminde KPI yenilenir', () => {
    const aramaBlok = src.slice(src.indexOf('const aramaDebounce'), src.indexOf('const filtreTemizle'))
    expect(aramaBlok).toContain('ozetYukle()')
    const temizleBlok = src.slice(src.indexOf('const filtreTemizle'), src.indexOf('const filtreTemizle') + 500)
    expect(temizleBlok).toContain('ozetYukle()')
    expect(temizleBlok).toContain("arama.value = ''")
  })

  it('sunucu tarafli siralama parametresi gonderir', () => {
    expect(src).toContain("const siralama = ref({ field: 'tarih', order: -1 })")
    expect(src).toContain('sort: `${siralama.value.field},${siralama.value.order === -1 ? \'desc\' : \'asc\'}`')
    expect(src).toContain('@sort="sirala"')
    expect(src).toContain(':sort-field="siralama.field"')
    expect(src).toContain(':sort-order="siralama.order"')
  })

  it('siralanabilir kolonlar beyaz liste ile uyumludur', () => {
    for (const alan of ['faturaNumarasi', 'tarih', 'cariHesapAd', 'genelToplam', 'kalanTutar']) {
      const blok = src.slice(src.indexOf(`field="${alan}"`), src.indexOf(`field="${alan}"`) + 500)
      expect(blok).toContain('sortable')
    }
  })

  it('kolon ayarlari TabloAyarlari ile yonetilir', () => {
    expect(src).toContain('<TabloAyarlari')
    expect(src).toContain('tablo-key="fatura"')
    expect(src).toContain(':kolonlar="kolonlar"')
    expect(src).toContain('@update:kolonlar="kolonGuncelle"')
    expect(src).toContain('@update:yogunluk="tabloYogunluk = $event"')
  })

  it('kompakt yogunluk tabloya uygulanir', () => {
    expect(src).toContain(':size="tabloYogunluk === \'compact\' ? \'small\' : \'normal\'"')
  })

  it('tur filtresi kompakt bir secimdir', () => {
    expect(src).toContain('data-test="fatura-tur-filtre"')
    expect(src).toContain('class="filtre-secim tur-filtre-secim"')
    expect(src).not.toContain(':allow-empty="false"')
  })

  it('varsayilan gorunumde ikincil kolonlar gizlidir', () => {
    for (const alan of ['tur', 'yazdirmaSayisi', 'olusturanKullaniciAdi', 'teslimEden', 'teslimatDurum']) {
      expect(src).toContain(`${alan}: false`)
      expect(src).toContain(`kolonGorunur('${alan}')`)
    }
    // Gunluk kullanim kolonlari varsayilan olarak acik.
    for (const alan of ['faturaNumarasi', 'tarih', 'cariHesapAd', 'genelToplam', 'odemeDurumu', 'kalanTutar', 'durum']) {
      expect(src).toContain(`kolonGorunur('${alan}')`)
    }
  })

  it('secim, fatura no ve islem kolonlari sabittir', () => {
    expect(src).toContain('class="fatura-secim-kolon"')
    expect(src).toContain('class="fatura-no-kolon"')
    expect(src).toContain('class="fatura-islem-kolon"')
    expect(src).toContain('.fatura-secim-kolon')
    expect(src).toContain('.fatura-no-kolon')
    expect(src).toContain('.fatura-islem-kolon')
    expect(src).toContain('position: sticky')
  })

  it('tutar kolonlari sag hizali ve tek satirda kalir', () => {
    for (const alan of ['genelToplam', 'kalanTutar']) {
      const blok = src.slice(src.indexOf(`field="${alan}"`), src.indexOf(`field="${alan}"`) + 500)
      expect(blok).toContain('class="sayisal"')
    }
    expect(src).toContain('white-space: nowrap')
  })

  it('aktif filtre cipleri ve global Temizle vardir', () => {
    expect(src).toContain('fatura-aktif-filtreler')
    expect(src).toContain('aktifFiltreler')
    expect(src).toContain('filtreCipKaldir')
    expect(src).toContain('v-if="aktifFiltreler.length > 0"')
    expect(src).toContain("t('faturalar.temizle')")
  })

  it('filtre temizleme arama ve tarih araligini da sifirlar', () => {
    const blok = src.slice(src.indexOf('const filtreTemizle'), src.indexOf('const filtreTemizle') + 400)
    expect(blok).toContain("arama.value = ''")
    expect(blok).toContain('tarihAraligi.value = null')
    expect(blok).toContain('ozetYukle()')
  })

  it('ilk yukleme skeletonu sonraki yuklemelerde KPI/tabloyu kaldirmaz', () => {
    expect(src).toContain('const ilkYukleme = ref(true)')
    expect(src).toContain('v-if="ilkYukleme"')
    expect(src).toContain(':loading="loading"')
    expect(src).toContain('v-if="!ilkYukleme"')
  })
})
