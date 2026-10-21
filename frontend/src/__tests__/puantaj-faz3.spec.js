import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Puantaj revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (çift kayıt engeli + doğrulama + yetki + detay çekmecesi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Puantaj.vue')
const drawer = oku('components/PuantajDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelPuantajController.java')
const service = okuB('service/ik/PersonelPuantajService.java')
const repo = okuB('repository/ik/PersonelPuantajRepository.java')

describe('Puantaj — Faz 3', () => {
  it('backend: çift kayıt engeli + doğrulama', () => {
    expect(repo).toMatch(/existsByPersonelIdAndTarih\(Long personelId, LocalDate tarih\)/)
    expect(repo).toMatch(/existsByPersonelIdAndTarihAndIdNot/)
    expect(service).toMatch(/existsByPersonelIdAndTarih\(dto\.getPersonelId\(\), dto\.getTarih\(\)\)/)
    expect(service).toMatch(/existsByPersonelIdAndTarihAndIdNot\(personelId, tarih, id\)/)
    expect(service).toMatch(/Bu personel için seçilen tarihte puantaj kaydı zaten mevcut/)
    expect(service).toMatch(/puantajDogrula\(/)
    expect(service).toMatch(/Puantaj tarihi zorunludur/)
    expect(service).toMatch(/Geçersiz puantaj durumu/)
    expect(service).toMatch(/GECERLI_DURUMLAR/)
  })

  it('backend: silme yalnızca ADMIN', () => {
    expect(controller).toMatch(/@DeleteMapping\("\/\{id\}"\)[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
  })

  it('frontend: ⋮ aksiyonları + ADMIN silme + detay çekmecesi', () => {
    expect(view).toMatch(/useAuthStore/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const puantajAksiyonlari = \(k\)/)
    expect(view).toMatch(/if \(authStore\.isAdmin\)/)
    expect(view).toMatch(/<PuantajDetayDrawer/)
    expect(drawer).toMatch(/durumEtiket/)
    expect(drawer).toMatch(/olusturma/)
  })

  it('frontend: form doğrulama', () => {
    expect(view).toMatch(/puantaj\.tarihZorunluUyari/)
    expect(view).toMatch(/puantaj\.durumZorunluUyari/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'olusturma', 'tarihZorunluUyari', 'durumZorunluUyari']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
