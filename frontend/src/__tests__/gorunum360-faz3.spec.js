import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * 360° Görünümler revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (Çalışan arama/Excel + Segment filtre/Excel + çapraz gezinme + hedef query-seed).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Gorunumler360.vue')
const dosyaApi = oku('api/modules/dosya.js')
const stoklar = oku('views/Stoklar.vue')
const cariHesaplar = oku('views/CariHesaplar.vue')
const teslimatlar = oku('views/Teslimatlar.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Gorunumler360 — Faz 3', () => {
  it('çalışan arama + Excel', () => {
    expect(view).toMatch(/const calisanArama = ref/)
    expect(view).toMatch(/const filtreliCalisanlar = computed/)
    expect(view).toMatch(/calisanExcelIndir/)
    expect(dosyaApi).toMatch(/gorunum360Calisan\(\)\s*\{[\s\S]*?\/exports\/gorunum360-calisan/)
  })

  it('segment kartı filtre + Excel', () => {
    expect(view).toMatch(/const segmentFiltresi = ref/)
    expect(view).toMatch(/const filtreliMusteriler = computed/)
    expect(view).toMatch(/const segmentFiltreUygula = /)
    expect(view).toMatch(/segmentExcelIndir/)
    expect(dosyaApi).toMatch(/gorunum360Segment\(\)\s*\{[\s\S]*?\/exports\/gorunum360-segment/)
  })

  it('çapraz gezinme: ürün→Stoklar, çalışan→Teslimatlar, müşteri→CariHesaplar', () => {
    expect(view).toMatch(/const urunGit = /)
    expect(view).toMatch(/name: 'Stoklar', query: \{ q: u\.stokKodu \|\| u\.ad/)
    expect(view).toMatch(/const calisanGit = /)
    expect(view).toMatch(/name: 'Teslimatlar', query: \{ sofor: c\?\.ad/)
    expect(view).toMatch(/const musteriGit = /)
    expect(view).toMatch(/name: 'CariHesaplar', query: \{ q: ad \}/)
    expect(view).toMatch(/@row-click="urunGit"/)
    expect(view).toMatch(/@row-click="musteriGit"/)
  })

  it('hedef ekranlar query-seed destekler', () => {
    expect(stoklar).toMatch(/useRoute\(\)/)
    expect(stoklar).toMatch(/route\?\.query\?\.q/)
    expect(cariHesaplar).toMatch(/useRoute\(\)/)
    expect(cariHesaplar).toMatch(/route\?\.query\?\.q/)
    expect(teslimatlar).toMatch(/useRoute\(\)/)
    expect(teslimatlar).toMatch(/route\?\.query\?\.sofor/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of ['calisanAra', 'segmentSec']) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
