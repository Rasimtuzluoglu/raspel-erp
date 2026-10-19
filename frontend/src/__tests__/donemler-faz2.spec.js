import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Dönemler revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (arama/filtre/sıralama + form doğrulama + çakışma reddi + detay çekmecesi + Excel).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Donemler.vue')
const drawer = oku('components/DonemDetayDrawer.vue')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const repo = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/repository/sistem/DonemRepository.java'),
  'utf8'
)
const service = readFileSync(
  join(ROOT, 'backend/src/main/java/com/raspel/erp/service/sistem/DonemService.java'),
  'utf8'
)

describe('Donemler — Faz 2', () => {
  it('arama + durum çipleri + sıralama', () => {
    expect(view).toMatch(/const arama = ref/)
    expect(view).toMatch(/const durumFiltresi = ref/)
    expect(view).toMatch(/const sirala = ref/)
    expect(view).toMatch(/const filtreliDonemler = computed/)
    expect(view).toMatch(/class="donem-cipler"/)
    expect(view).toMatch(/const filtreTemizle = /)
  })

  it('form doğrulaması', () => {
    expect(view).toMatch(/donemler\.tarihZorunlu/)
    expect(view).toMatch(/donemler\.bitisOnceOlamaz/)
    expect(view).toMatch(/new Date\(form\.value\.bitis\) < new Date\(form\.value\.baslangic\)/)
  })

  it('kilitle onayı + detay çekmecesi', () => {
    expect(view).toMatch(/donemler\.kilitleOnay/)
    expect(view).toMatch(/<DonemDetayDrawer/)
    expect(view).toMatch(/const detayAc = /)
    expect(drawer).toMatch(/donemler\.kilitTarihi/)
    expect(drawer).toMatch(/donemler\.kilitKullanici/)
  })

  it('Excel export (uç + indirme)', () => {
    expect(dosyaApi).toMatch(/donemler\(\)\s*\{[\s\S]*?\/exports\/donemler/)
    expect(view).toMatch(/excelAPI\.donemler\(\)/)
    expect(view).toMatch(/donemler\.excelIndirildi/)
  })

  it('backend çakışma kontrolü', () => {
    expect(repo).toMatch(/cakisanDonemSayisi/)
    expect(repo).toMatch(/d\.baslangic <= :bitis AND d\.bitis >= :baslangic/)
    expect(service).toMatch(/private void cakismaDogrula/)
    expect(service).toMatch(/çakışıyor/)
    expect(service).toMatch(/private void tarihAraligiDogrula/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'aramaPlaceholder',
      'cipTumu',
      'filtreBos',
      'temizle',
      'excel',
      'detay',
      'tarihZorunlu',
      'bitisOnceOlamaz',
      'kilitleOnay',
      'sure',
      'kilitTarihi',
      'kilitKullanici'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
