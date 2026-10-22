import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Masraflar revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (kategori/ödeme doğrulaması + cari hesap + yetki + detay çekmecesi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Masraflar.vue')
const drawer = oku('components/MasrafDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const service = okuB('service/finans/MasrafService.java')
const controller = okuB('controller/finans/MasrafController.java')

describe('Masraflar — Faz 3', () => {
  it('backend: kategori/ödeme/cari doğrulaması', () => {
    expect(service).toMatch(/masrafDogrula\(/)
    expect(service).toMatch(/Masraf kategorisi zorunludur/)
    expect(service).toMatch(/Geçersiz ödeme yöntemi/)
    expect(service).toMatch(/ODEME_YONTEMLERI/)
    expect(service).toMatch(/cariHesapRepository\.findById/)
    expect(service).toMatch(/tenantChecker\.check\(cari\.getSirketId\(\), "Cari hesap"\)/)
    expect(service).toMatch(/normalizeOdemeYontemi/)
    expect(service).toMatch(/\.toUpperCase\(Locale\.ROOT\)/)
  })

  it('backend: yazma/silme yetkileri', () => {
    expect(controller).toMatch(/@PostMapping[\s\S]*?@PreAuthorize\("hasAnyRole\('ADMIN', 'MUHASEBE'\)"\)/)
    expect(controller).toMatch(/@PutMapping[\s\S]*?@PreAuthorize\("hasAnyRole\('ADMIN', 'MUHASEBE'\)"\)/)
    expect(controller).toMatch(/@DeleteMapping\("\/\{id\}"\)[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
  })

  it('frontend: kategori/cari/ödeme alanları + düzenlemede ödeme açık', () => {
    expect(view).toMatch(/kategoriFormSecenekleri/)
    expect(view).toMatch(/cariHesapAPI\.getAll/)
    expect(view).toMatch(/odemeYontemleri/)
    expect(view).toMatch(/masraflar\.odemeYontemi/)
    expect(view).not.toMatch(/kasaId: undefined/)
  })

  it('frontend: yetki gating + ⋮ + detay çekmecesi', () => {
    expect(view).toMatch(/const canWrite = computed/)
    expect(view).toMatch(/const isAdmin = computed/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const masrafAksiyonlari = \(m\)/)
    expect(view).toMatch(/<MasrafDetayDrawer/)
    expect(drawer).toMatch(/odemeEtiket/)
    expect(drawer).toMatch(/cariAdi/)
    expect(drawer).toMatch(/kdvTutar/)
  })

  it('frontend: form doğrulama', () => {
    expect(view).toMatch(/masraflar\.kategoriZorunluUyari/)
    expect(view).toMatch(/masraflar\.tutarPozitifUyari/)
    expect(view).toMatch(/masraflar\.tekHesap/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['cariHesap', 'odemeYontemi', 'odemeNakit', 'odemeHavale', 'odemeKart', 'detay', 'olusturma']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
