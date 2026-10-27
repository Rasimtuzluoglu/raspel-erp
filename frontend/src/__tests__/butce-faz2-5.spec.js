import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Bütçe revizyonu — Faz 2-5 kaynak-tabanlı doğrulama
 * (tür ayrımlı gerçekleşen + Excel/PDF, 12 aylık matris + kopyalama,
 *  onay akışı + revizyon geçmişi + detay drawer).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')
const okuR = (p) => readFileSync(join(ROOT, 'backend/src/main/resources', p), 'utf8')

const view = oku('views/Butceler.vue')
const drawer = oku('components/ButceDetayDrawer.vue')
const finansApi = oku('api/modules/finans.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const service = okuB('service/finans/ButceService.java')
const controller = okuB('controller/finans/ButceController.java')
const entity = okuB('entity/finans/Butce.java')
const revizyon = okuB('entity/finans/ButceRevizyon.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')
const migration = okuR('db/migration/V168__butce_onay_revizyon.sql')

describe('Bütçe — Faz 2 (gerçekleşen)', () => {
  it('tür ayrımlı rapor: gider masraf, gelir tahsilat', () => {
    expect(service).toMatch(/ButceGerceklesenRaporDTO gerceklesen\(/)
    expect(service).toMatch(/Hareket\.HareketTuru\.TAHSILAT/)
    expect(service).toMatch(/masrafRepository\s*\.findBySirketIdAndTarihBetween/)
    expect(controller).toMatch(/@GetMapping\("\/gerceklesen"\)/)
    expect(controller).toMatch(/@GetMapping\("\/gerceklesen\/pdf"\)/)
    expect(finansApi).toMatch(/gerceklesen\(params = \{\}\)\s*\{[\s\S]*?\/butceler\/gerceklesen/)
    expect(dosyaApi).toMatch(/butceGerceklesen\(params\)\s*\{[\s\S]*?\/exports\/butce-gerceklesen/)
  })

  it('gerçekleşen sekmesi tür geçişi, özet ve indirmeler', () => {
    expect(view).toMatch(/butceler\.tabGerceklesen/)
    expect(view).toMatch(/const gerTurSec = \(deger\) =>/)
    expect(view).toMatch(/butceAPI\.gerceklesen\(params\)/)
    expect(view).toMatch(/excelAPI\.butceGerceklesen\(params\)/)
    expect(view).toMatch(/butceAPI\.gerceklesenPdf\(params\)/)
    expect(view).toMatch(/role="progressbar"/)
  })
})

describe('Bütçe — Faz 3 (matris)', () => {
  it('12 aylık matris + önceki yıldan kopyalama', () => {
    expect(service).toMatch(/ButceMatrisDTO matris\(/)
    expect(service).toMatch(/public Map<String, Integer> kopyala\(/)
    expect(controller).toMatch(/@GetMapping\("\/matris"\)/)
    expect(controller).toMatch(/@PostMapping\("\/kopyala"\)/)
    expect(excelController).toMatch(/@GetMapping\("\/butce-matris"\)/)
    expect(finansApi).toMatch(/matris\(params = \{\}\)/)
    expect(finansApi).toMatch(/kopyala\(data\)/)
    expect(view).toMatch(/butceler\.tabMatris/)
    expect(view).toMatch(/class="matris"/)
    expect(view).toMatch(/const kopyalaKaydet = async/)
  })
})

describe('Bütçe — Faz 4 (onay + revizyon)', () => {
  it('migration: durum/revizyon kolonları ve snapshot tablosu', () => {
    expect(migration).toMatch(/ADD COLUMN IF NOT EXISTS durum VARCHAR\(20\) NOT NULL DEFAULT 'ONAYLANDI'/)
    expect(migration).toMatch(/ADD COLUMN IF NOT EXISTS revizyon INTEGER NOT NULL DEFAULT 1/)
    expect(migration).toMatch(/CREATE TABLE IF NOT EXISTS finans\.butce_revizyon/)
    expect(migration).toMatch(/FOREIGN KEY \(butce_id\)/)
  })

  it('entity ve servis: onayla/reddet + revizyon snapshot', () => {
    expect(entity).toMatch(/private String durum/)
    expect(entity).toMatch(/private Integer revizyon/)
    expect(revizyon).toMatch(/@Table\(name = "butce_revizyon", schema = "finans"\)/)
    expect(service).toMatch(/public ButceDTO onayla\(Long id, Long kullaniciId\)/)
    expect(service).toMatch(/public ButceDTO reddet\(Long id, String gerekce, Long kullaniciId\)/)
    expect(service).toMatch(/revizyonYaz\(kayit, "ONAY"/)
    expect(service).toMatch(/revizyonYaz\(kayit, "RED"/)
    expect(controller).toMatch(/@PutMapping\("\/\{id\}\/onayla"\)/)
    expect(controller).toMatch(/@PutMapping\("\/\{id\}\/reddet"\)/)
    expect(controller).toMatch(/@GetMapping\("\/onay-bekleyen"\)/)
  })

  it('detay drawer + onay sekmesi + satır eylemleri', () => {
    expect(view).toMatch(/<ButceDetayDrawer/)
    expect(view).toMatch(/butceler\.tabOnay/)
    expect(view).toMatch(/const satirEylemleri = \(b\) =>/)
    expect(view).toMatch(/butceAPI\.onayla\(b\.id\)/)
    expect(view).toMatch(/butceAPI\.reddet\(redHedef\.value\.id, redGerekce\.value\)/)
    expect(drawer).toMatch(/butceler\.revizyonlar/)
    expect(drawer).toMatch(/islemSinif/)
    expect(drawer).toMatch(/durumSeverity/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'tabGerceklesen',
      'tabMatris',
      'tabOnay',
      'gerceklesenBos',
      'matrisKopyala',
      'kopyalaBaslik',
      'kopyalandi',
      'durumTaslak',
      'durumOnaylandi',
      'durumReddedildi',
      'onayla',
      'reddet',
      'redGerekcesi',
      'detayBaslik',
      'revizyonlar',
      'islemOnay'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
