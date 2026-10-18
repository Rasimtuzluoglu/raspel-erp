import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Üretim revizyonu — kaynak-tabanlı doğrulama
 * (sunucu taraflı filtreli liste + KPI tıklama + hızlı çipler + Drawer detay +
 *  responsive diyaloglar + mobil kart + Excel export).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const uretim = oku('views/Uretim.vue')
const drawer = oku('components/UretimEmriDetayDrawer.vue')
const stokApi = oku('api/modules/stok.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Üretim revizyonu', () => {
  it('sunucu taraflı filtreli/sayfalı emir listesi', () => {
    expect(uretim).toMatch(/uretimAPI\.emirlerFiltreli\(/)
    expect(uretim).toMatch(/const emirParams = \(\)/)
    expect(uretim).toMatch(/const emirleriYukle = async/)
    expect(uretim).toMatch(/emirToplam/)
    expect(uretim).toMatch(/lazy/)
    expect(stokApi).toMatch(/emirlerFiltreli\(params = \{\}\)\s*\{[\s\S]*?\/uretim\/emirler\/filtreli/)
  })

  it('KPI kartları tıklanabilir ve filtreyi kurar', () => {
    expect(uretim).toMatch(/<KpiKart/)
    expect(uretim).toMatch(/kpi-tiklanabilir/)
    expect(uretim).toMatch(/const kpiEmirFiltre = \(tur\)/)
    expect(uretim).toMatch(/const aktifCip = computed/)
    // Bu Ay Tamamlanan KPI, buAy sunucu filtresini gönderir.
    expect(uretim).toMatch(/emirBuAy\.value = tur === 'BUAY'/)
    expect(uretim).toMatch(/if \(emirBuAy\.value\) p\.buAy = true/)
  })

  it('hızlı filtre çipleri + temizle', () => {
    expect(uretim).toMatch(/const emirCipleri = computed/)
    expect(uretim).toMatch(/class="emir-cipler"/)
    expect(uretim).toMatch(/const emirCipUygula = \(deger\)/)
    expect(uretim).toMatch(/const emirFiltreTemizle = \(\)/)
  })

  it('Detay / Geçmiş Drawer + timeline', () => {
    expect(uretim).toMatch(/<UretimEmriDetayDrawer/)
    expect(uretim).toMatch(/const detayAc = async/)
    expect(drawer).toMatch(/zaman-cizelgesi/)
    expect(drawer).toMatch(/const durumSeverity/)
    expect(drawer).toMatch(/detay-aksiyonlar/)
  })

  it('responsive diyaloglar (breakpoints)', () => {
    expect(uretim).toMatch(/:breakpoints="\{ '575px': '92vw' \}"/)
    const adet = (uretim.match(/:breakpoints=/g) || []).length
    expect(adet).toBeGreaterThanOrEqual(4)
  })

  it('mobil kart görünümü', () => {
    expect(uretim).toMatch(/class="emir-mobil"/)
    expect(uretim).toMatch(/emir-mobil-kart/)
    expect(uretim).toMatch(/@media \(max-width: 768px\)/)
  })

  it('reçete aktif/pasif filtresi', () => {
    expect(uretim).toMatch(/const filtreliReceteler = computed/)
    expect(uretim).toMatch(/const receteDurumSecenekleri = computed/)
  })

  it('Excel export (emirler + reçeteler)', () => {
    expect(dosyaApi).toMatch(/uretimEmirleri\(params\)\s*\{[\s\S]*?\/exports\/uretim-emirleri/)
    expect(dosyaApi).toMatch(/uretimReceteler\(\)\s*\{[\s\S]*?\/exports\/uretim-receteler/)
    expect(uretim).toMatch(/excelAPI\.uretimEmirleri\(/)
    expect(uretim).toMatch(/excelAPI\.uretimReceteler\(/)
    expect(uretim).toMatch(/uretim\.excelIndirildi/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of ['cipTumu', 'cipGeciken', 'emirFiltreYok', 'aramaPlaceholder', 'toplamUretilen', 'toplamFire']) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
