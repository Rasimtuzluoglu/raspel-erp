import { describe, it, expect } from 'vitest'
import { readFileSync, existsSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Faz1 — Bağımsız düşük riskli düzeltmeler (kaynak-tabanlı doğrulama).
 *
 *  1) Denetim: kayıtlı filtreler + otomatik yenileme + kullanıcı filtresi + tek boş durum
 *  2) Hareketler: işlem ikonları tek "..." menüsüne taşındı
 *  3) Yasal: Gizlilik + Kullanım tek sayfada (sekmeli) birleşti
 *  4) TopluStok sekmesi kaldırıldı
 *  5) AI Yönetici Özeti Dashboard'dan Yönetici Kokpiti'ne taşındı
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

describe('Faz1 — Denetim', () => {
  const denetim = oku('views/Denetim.vue')

  it('tarih aralığı değişince listeyi otomatik yeniler (watch)', () => {
    expect(denetim).toMatch(/watch\(\s*\(\)\s*=>\s*filtre\.value\.tarihAraligi/)
  })
  it('kayıtlı filtre silme akışı var', () => {
    expect(denetim).toMatch(/const kayitliFiltreSil/)
    expect(denetim).toMatch(/filtreSil/)
  })
  it('kayıtlı filtreler ISO tarih olarak serileştirilir (Date bug\'ı yok)', () => {
    expect(denetim).toMatch(/const isoToDate/)
    expect(denetim).toMatch(/getLocalDateString\(filtre\.value\.tarihAraligi\[0\]\)/)
  })
  it('mükerrer kaydı engeller ve sayıyı sınırlar', () => {
    expect(denetim).toMatch(/MAKS_KAYITLI_FILTRE/)
    expect(denetim).toMatch(/findIndex/)
  })
  it('kullanıcı (kullaniciId) filtresi eklendi', () => {
    expect(denetim).toMatch(/v-model="filtre\.kullaniciId"/)
    expect(denetim).toMatch(/params\.kullaniciId/)
  })
  it('işlem kodları çeviriden geçer (ham OLUSTUR/SIL görünmez)', () => {
    expect(denetim).toMatch(/const islemEtiket/)
    expect(denetim).toMatch(/denetim\.islemOlustur/)
  })
  it('tek boş durum (eski çift boş blok kaldırıldı)', () => {
    expect(denetim).not.toMatch(/v-if="\(!logs \|\| !logs\.length\) && !yukleniyor"/)
  })
})

describe('Faz1 — Hareketler işlem ikonları', () => {
  const hareket = oku('views/Hareketler.vue')
  it('SatirEylemleri "..." menüsü kullanılır', () => {
    expect(hareket).toMatch(/<SatirEylemleri/)
    expect(hareket).toMatch(/import SatirEylemleri from '\.\.\/components\/SatirEylemleri\.vue'/)
  })
  it('satır içi uyarı/ikon butonları kaldırıldı', () => {
    expect(hareket).not.toMatch(/p-button-warning/)
    expect(hareket).not.toMatch(/pi pi-ban"\s+class="p-button-rounded/)
  })
})

describe('Faz1 — Yasal metinler birleştirme', () => {
  const router = oku('router/index.js')
  it('tek /yasal rotası var', () => {
    expect(router).toMatch(/path:\s*'\/yasal'/)
    expect(router).toMatch(/YasalMetinler\.vue/)
  })
  it('eski rotalar yönlendiriyor', () => {
    expect(router).toMatch(/path:\s*'\/kullanim-sartlari',\s*\n\s*redirect/)
    expect(router).toMatch(/path:\s*'\/gizlilik-politikasi',\s*\n\s*redirect/)
  })
  it('eski iki view silindi', () => {
    expect(existsSync(join(SRC, 'views/KullanimSartlari.vue'))).toBe(false)
    expect(existsSync(join(SRC, 'views/GizlilikPolitikasi.vue'))).toBe(false)
  })
})

describe('Faz1 — TopluStok kaldırma', () => {
  const router = oku('router/index.js')
  const sidebar = oku('components/AppSidebar.vue')
  it('rota/menü/dosya kaldırıldı', () => {
    expect(router).not.toMatch(/\/toplu-stok/)
    expect(sidebar).not.toMatch(/\/toplu-stok/)
    expect(existsSync(join(SRC, 'views/TopluStok.vue'))).toBe(false)
  })
})

describe('Faz1 — AI özeti taşıma', () => {
  const dashboard = oku('views/Dashboard.vue')
  const kokpit = oku('views/YoneticiKokpiti.vue')
  it('Dashboard\'dan kaldırıldı', () => {
    expect(dashboard).not.toMatch(/AiOzetKarti/)
  })
  it('Yönetici Kokpiti\'ne eklendi', () => {
    expect(kokpit).toMatch(/import AiOzetKarti from '\.\.\/components\/AiOzetKarti\.vue'/)
    expect(kokpit).toMatch(/<AiOzetKarti\s*\/>/)
  })
})
