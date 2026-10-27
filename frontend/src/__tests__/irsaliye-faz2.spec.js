import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * İrsaliye revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (detay drawer, yeni/düzenle, sevk/teslim/iptal, toplu fatura, Excel, responsive).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Irsaliyeler.vue')
const drawer = oku('components/IrsaliyeDetayDrawer.vue')
const ticaretApi = oku('api/modules/ticaret.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('İrsaliye — Faz 2 (detay + aksiyonlar)', () => {
  it('detay drawer durum, kalem, bağlantı ve zaman çizelgesi içerir', () => {
    expect(drawer).toMatch(/<Drawer/)
    expect(drawer).toMatch(/irsaliyeler\.detayBaslik/)
    expect(drawer).toMatch(/kalem-tablo/)
    expect(drawer).toMatch(/zaman-cizelgesi/)
    expect(drawer).toMatch(/irsaliyeler\.faturaBaglantisi/)
    expect(drawer).toMatch(/irsaliyeler\.teslimatKaydi/)
    expect(drawer).toMatch(/irsaliyeler\.siparis/)
    expect(drawer).toMatch(/@media|:breakpoints="\{ '575px': '92vw' \}"/)
  })

  it('drawer yalnız izinli aksiyonları gösterir ve olayları yayar', () => {
    expect(drawer).toMatch(/\['TASLAK', 'HAZIRLANDI'\]\.includes\(props\.kayit\.durum\)/)
    expect(drawer).toMatch(/kayit\.durum === 'TASLAK'/)
    expect(drawer).toMatch(/kayit\.durum === 'HAZIRLANDI'/)
    expect(drawer).toMatch(/kayit\.durum === 'SEVK_EDILDI'/)
    expect(drawer).toMatch(/kayit\.durum !== 'IPTAL'/)
    expect(drawer).toMatch(/!props\.kayit\.faturaId/)
    for (const olay of [
      "'duzenle'",
      "'hazirla'",
      "'sevk'",
      "'teslim'",
      "'sevki-geri-al'",
      "'iptal'",
      "'faturaya-donustur'"
    ]) {
      expect(drawer).toContain(olay)
    }
  })

  it('yeni/düzenle dialogu sevk alanları, kalemler ve barkod içerir', () => {
    expect(view).toMatch(/irsaliyeler\.irsaliyeDuzenle/)
    expect(view).toMatch(/form\.plaka/)
    expect(view).toMatch(/form\.soforId/)
    expect(view).toMatch(/form\.tasiyiciFirma/)
    expect(view).toMatch(/form\.teslimatAdresi/)
    expect(view).toMatch(/form\.koliAdedi/)
    expect(view).toMatch(/form\.desi/)
    expect(view).toMatch(/form\.brutKg/)
    expect(view).toMatch(/kalemEkle/)
    expect(view).toMatch(/BarcodeScannerModal/)
    expect(view).toMatch(/irsaliyeAPI\.update\(form\.value\.id, formPayload\(\)\)/)
  })

  it('sevk, teslim ve iptal diyalogları zorunlu alanları doğrular', () => {
    expect(view).toMatch(/irsaliyeler\.sevkBaslik/)
    expect(view).toMatch(/irsaliyeler\.teslimBaslik/)
    expect(view).toMatch(/irsaliyeler\.iptalBaslik/)
    expect(view).toMatch(/soforZorunlu/)
    expect(view).toMatch(/teslimAlanZorunlu/)
    expect(view).toMatch(/cariAdresi\(kayit\.cariHesapId\)/)
    expect(view).toMatch(/irsaliyeAPI\.sevk\(/)
    expect(view).toMatch(/irsaliyeAPI\.teslim\(/)
    expect(view).toMatch(/irsaliyeAPI\.sevkiGeriAl\(/)
    expect(view).toMatch(/irsaliyeAPI\.iptal\(/)
  })

  it('toplu fatura: seçim, aynı cari kuralı ve sonuç modalı', () => {
    expect(view).toMatch(/selection-mode="multiple"/)
    expect(view).toMatch(/<template #batch-actions>/)
    expect(view).toMatch(/const farkliCari = computed/)
    expect(view).toMatch(/const topluUygun = computed/)
    expect(view).toMatch(/:disabled="!topluUygun"/)
    expect(view).toMatch(/irsaliyeler\.topluFarkliCari/)
    expect(view).toMatch(/irsaliyeAPI\.topluFatura\(secili\.value\.map\(\(s\) => s\.id\)\)/)
    expect(view).toMatch(/irsaliyeler\.topluSonucBaslik/)
    expect(view).toMatch(/sonuc\.basarili/)
    expect(view).toMatch(/sonuc\.atlanan/)
    expect(ticaretApi).toMatch(/topluFatura\(ids\)\s*\{[\s\S]*?\/irsaliyeler\/toplu-fatura/)
  })

  it('API sözleşmesi: geçiş uçları ve Excel', () => {
    for (const metot of ['hazirla', 'sevk', 'teslim', 'sevkiGeriAl', 'iptal', 'update', 'filtreli', 'ozet']) {
      expect(ticaretApi).toContain(`${metot}(`)
    }
    expect(ticaretApi).toMatch(/hazirla\(id\)\s*\{[\s\S]*?\/hazirla/)
    expect(ticaretApi).toMatch(/sevkiGeriAl\(id\)\s*\{[\s\S]*?\/sevki-geri-al/)
    expect(dosyaApi).toMatch(/irsaliyeler\(params\)\s*\{[\s\S]*?\/exports\/irsaliyeler/)
  })

  it('responsive diyaloglar ve PDF', () => {
    expect(view.match(/:breakpoints="\{ '575px': '92vw' \}"/g)?.length).toBeGreaterThanOrEqual(5)
    expect(view).toMatch(/pdfAPI\.irsaliye\(kayit\.id\)/)
    expect(view).toMatch(/excelAPI\.irsaliyeler\(filtreParams\(\)\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'detayBaslik',
      'sevkBaslik',
      'teslimBaslik',
      'iptalBaslik',
      'sevkEt',
      'teslimEt',
      'sevkiGeriAl',
      'iptalEt',
      'topluFatura',
      'topluFarkliCari',
      'topluSonucBaslik',
      'topluBasarili',
      'topluAtlanan',
      'soforZorunlu',
      'teslimAlanZorunlu',
      'plakaSofor',
      'sevkTarihi',
      'teslimatAdresi'
    ]) {
      expect(tr).toContain(`"${anahtar}"`)
      expect(en).toContain(`"${anahtar}"`)
    }
  })
})
