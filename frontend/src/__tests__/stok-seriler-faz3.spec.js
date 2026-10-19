import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Seri/Lot Takibi revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (güvenli düzenleme + detay çekmecesi + doğrulama + silme guard).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/StokSeriler.vue')
const drawer = oku('components/StokSeriDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/envanter/StokSeriService.java'),
  'utf8'
)
const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokSeriRepository.java'),
  'utf8'
)
const hareketRepo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/envanter/StokHareketRepository.java'),
  'utf8'
)

describe('StokSeriler — Faz 3', () => {
  it('güvenli düzenleme: ürün/miktar kilitli', () => {
    expect(view).toMatch(/:disabled="duzenleme"/)
    expect(view).toMatch(/stokSeriler\.guvenliDuzenlemeNotu/)
    expect(view).toMatch(/seriNo: form\.value\.seriNo\.trim\(\)/)
  })

  it('⋮ eylem menüsü + detay çekmecesi', () => {
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const seriAksiyonlari = /)
    expect(view).toMatch(/<StokSeriDetayDrawer/)
    expect(view).toMatch(/const detayAc = /)
    expect(drawer).toMatch(/seri\.durum === 'STOKTA'/)
  })

  it('ürün seçici sunucu aramalı', () => {
    expect(view).toMatch(/useStokOnerileri/)
    expect(view).toMatch(/:options="stokOnerileri"/)
    expect(view).toMatch(/@filter="stokAra"/)
  })

  it('form doğrulaması', () => {
    expect(view).toMatch(/stokSeriler\.urunZorunlu/)
    expect(view).toMatch(/stokSeriler\.seriNoZorunlu/)
    expect(view).toMatch(/stokSeriler\.miktarPozitifOlmali/)
  })

  it('backend guard: tekil seri no + düzenleme/silme koruması', () => {
    expect(repo).toMatch(/existsByStokIdAndSeriNo/)
    expect(repo).toMatch(/existsByStokIdAndSeriNoAndIdNot/)
    expect(hareketRepo).toMatch(/existsBySeriId/)
    expect(service).toMatch(/Yalnızca stoktaki seri\/lotlar düzenlenebilir/)
    expect(service).toMatch(/Tüketilmiş seri\/lot silinemez/)
    expect(service).toMatch(/bir stok hareketine bağlı olduğu için silinemez/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'girisTarihi', 'olusturma', 'guvenliDuzenlemeNotu', 'miktarPozitifOlmali']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
