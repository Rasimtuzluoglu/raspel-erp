import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Personel revizyonu — Faz 3 kaynak-tabanlı doğrulama
 * (doğrulama + ADMIN yazma + maaş kısıtı + detay çekmecesi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Personel.vue')
const drawer = oku('components/PersonelDetayDrawer.vue')
const ikApi = oku('api/modules/ik.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/ik/PersonelController.java')
const service = okuB('service/ik/PersonelService.java')
const repo = okuB('repository/ik/PersonelRepository.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')

describe('Personel — Faz 3', () => {
  it('backend: ADMIN yazma + detay ucu', () => {
    expect(controller).toMatch(/@PostMapping[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
    expect(controller).toMatch(/@PutMapping[\s\S]*?@PreAuthorize\("hasRole\('ADMIN'\)"\)/)
    expect(controller).toMatch(/@GetMapping\("\/\{id\}\/detay"\)/)
  })

  it('backend: ad/TC/telefon/e-posta/maaş/tarih doğrulaması', () => {
    expect(service).toMatch(/adDogrula/)
    expect(service).toMatch(/alan \+ " zorunludur"/)
    expect(service).toMatch(/tcDogrula/)
    expect(service).toMatch(/TC kimlik numarası geçersiz/)
    expect(service).toMatch(/existsByTcKimlikAndIdNot/)
    expect(service).toMatch(/telefonDogrula/)
    expect(service).toMatch(/emailDogrula/)
    expect(service).toMatch(/maasDogrula/)
    expect(service).toMatch(/tarihDogrula/)
    expect(repo).toMatch(/existsByTcKimlik\(String tcKimlik\)/)
  })

  it('backend: maaş yalnızca ADMIN/MUHASEBE', () => {
    expect(service).toMatch(/maasGorebilir\(\)/)
    expect(service).toMatch(/ROLE_MUHASEBE/)
    expect(service).toMatch(/\.maas\(maasGorebilir\(\) \? p\.getMaas\(\) : null\)/)
    expect(excelController).toMatch(/boolean maasGoster = maasGorebilir\(\)/)
    expect(excelController).toMatch(/if \(maasGoster\) m\.put\("Maaş"/)
  })

  it('backend: detay DTO ve puantaj özeti', () => {
    expect(service).toMatch(/PersonelDetayDTO detay\(Long id\)/)
    expect(service).toMatch(/findByPersonelIdOrderByBaslangicDesc/)
    expect(service).toMatch(/findBySirketIdAndPersonelIdAndTarihBetweenOrderByTarihAsc/)
    expect(service).toMatch(/bugunIzinli/)
  })

  it('frontend: ⋮ aksiyonları + detay çekmecesi + İzinler linki', () => {
    expect(view).toMatch(/<SatirEylemleri/)
    expect(view).toMatch(/const personelAksiyonlari = \(d\)/)
    expect(view).toMatch(/<PersonelDetayDrawer/)
    expect(view).toMatch(/personelAPI\.detay\(d\.id\)/)
    expect(view).toMatch(/router\.push\('\/izinler'\)/)
    expect(drawer).toMatch(/puantaj-kutular/)
    expect(drawer).toMatch(/sonIzinler/)
    expect(ikApi).toMatch(/detay\(id\)\s*\{[\s\S]*?\/personel\/\$\{id\}\/detay/)
  })

  it('frontend: form doğrulama + maaş kısıtı', () => {
    expect(view).toMatch(/const canSalary = computed/)
    expect(view).toMatch(/v-if="canSalary"/)
    expect(view).toMatch(/const dogrula = \(\)/)
    expect(view).toMatch(/personel\.tcGecersizUyari/)
    expect(view).toMatch(/personel\.emailGecersiz/)
    expect(view).toMatch(/personel\.cikisGiristenOnce/)
    expect(view).toMatch(/cikisTarihi: data\.cikisTarihi \? new Date/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'detay',
      'izinlereGit',
      'cikisTarihi',
      'adZorunluUyari',
      'tcGecersizUyari',
      'emailGecersiz',
      'maasNegatifOlamaz',
      'cikisGiristenOnce'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
    expect(tr).toMatch(/"personelDetay"\s*:/)
    expect(en).toMatch(/"personelDetay"\s*:/)
  })
})
