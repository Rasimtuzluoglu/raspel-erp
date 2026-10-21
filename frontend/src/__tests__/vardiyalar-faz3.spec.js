import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Vardiyalar revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (çakışma engeli + doğrulama + yetki + detay çekmecesi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Vardiyalar.vue')
const drawer = oku('components/VardiyaDetayDrawer.vue')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/VardiyaController.java')
const service = okuB('service/ik/VardiyaService.java')
const repo = okuB('repository/ik/VardiyaRepository.java')

describe('Vardiyalar — Faz 3', () => {
  it('backend: çakışma engeli + doğrulama', () => {
    expect(repo).toMatch(/existsByPersonelIdAndTarih\(Long personelId, LocalDate tarih\)/)
    expect(repo).toMatch(/existsByPersonelIdAndTarihAndIdNot/)
    expect(service).toMatch(/existsByPersonelIdAndTarih\(dto\.getPersonelId\(\), dto\.getTarih\(\)\)/)
    expect(service).toMatch(/existsByPersonelIdAndTarihAndIdNot\(personelId, tarih, id\)/)
    expect(service).toMatch(/Bu personel için seçilen tarihte vardiya kaydı zaten mevcut/)
    expect(service).toMatch(/vardiyaDogrula\(/)
    expect(service).toMatch(/Vardiya tarihi zorunludur/)
    expect(service).toMatch(/Başlangıç ve bitiş saati aynı olamaz/)
    expect(service).toMatch(/Geçersiz vardiya türü/)
    expect(service).toMatch(/GECERLI_TURLER/)
  })

  it('backend: silme yalnızca ADMIN', () => {
    expect(controller).toMatch(/@DeleteMapping\("\/\{id\}"\)[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
  })

  it('frontend: ⋮ aksiyonları + ADMIN silme + detay çekmecesi', () => {
    expect(view).toMatch(/useAuthStore/)
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const vardiyaAksiyonlari = \(v\)/)
    expect(view).toMatch(/if \(authStore\.isAdmin\)/)
    expect(view).toMatch(/<VardiyaDetayDrawer/)
    expect(drawer).toMatch(/turEtiket/)
    expect(drawer).toMatch(/olusturma/)
  })

  it('frontend: form doğrulama', () => {
    expect(view).toMatch(/vardiyalar\.saatGecersiz/)
    expect(view).toMatch(/vardiyalar\.ayniSaat/)
    expect(view).toMatch(/vardiyalar\.tarihZorunluUyari/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = ['detay', 'olusturma', 'saatGecersiz', 'ayniSaat', 'tarihZorunluUyari', 'turZorunluUyari']
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
