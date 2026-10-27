import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Ajanda revizyonu — Faz 2 kaynak-tabanlı doğrulama
 * (Yaklaşan sekmesi + sunucu taraflı /ajanda/yaklasan + Excel + çapraz gezinme).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Ajanda.vue')
const sistemApi = oku('api/modules/sistem.js')
const dosyaApi = oku('api/modules/dosya.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/sistem/AjandaController.java')
const excelController = okuB('controller/sistem/ExcelExportController.java')
const service = okuB('service/sistem/AjandaService.java')
const dto = okuB('dto/sistem/AjandaOlayDTO.java')

describe('Ajanda — Faz 2', () => {
  it('backend: /ajanda/yaklasan + DTO kaynak alanları', () => {
    expect(controller).toMatch(/@GetMapping\("\/yaklasan"\)/)
    expect(controller).toMatch(/defaultValue = "30"\) int gun/)
    expect(service).toMatch(/List<AjandaOlayDTO> yaklasan\(Long sirketId, Long kullaniciId, int gun\)/)
    expect(service).toMatch(/bugun\.plusDays\(Math\.max\(0, gun\)\)/)
    expect(dto).toMatch(/private Long kaynakId;/)
    expect(dto).toMatch(/private String kaynakTip;/)
    expect(dto).toMatch(/private String kaynakNo;/)
    expect(service).toMatch(/kaynakTip\("FATURA"\)\.kaynakNo\(f\.getFaturaNumarasi\(\)\)/)
    expect(service).toMatch(/kaynakTip\("PROJE_GOREVI"\)/)
    expect(service).toMatch(/kaynakTip\("KISISEL_GOREV"\)/)
    expect(service).toMatch(/kaynakTip\("HATIRLATICI"\)/)
  })

  it('backend: /exports/ajanda tip filtresiyle dosya üretir', () => {
    expect(excelController).toMatch(/@GetMapping\("\/ajanda"\)/)
    expect(excelController).toMatch(/ajandaService\.yaklasan\(sirketId, kullaniciId, gun\)/)
    expect(excelController).toMatch(/tip\.equals\(o\.getTip\(\)\)/)
    expect(excelController).toMatch(/excel\("Ajanda",/)
  })

  it('frontend: yaklaşan sekmesi tablo + filtre + Excel', () => {
    expect(view).toMatch(/ajandaAPI\.yaklasan\(yaklasanGun\.value\)/)
    expect(view).toMatch(/yaklasanTipSecenekleri/)
    expect(view).toMatch(/excelAPI\.ajanda\(params\)/)
    expect(view).toMatch(/<AppDataTable/)
    expect(view).toMatch(/:empty-message="t\('ajanda\.yaklasanYok'\)"/)
    expect(view).toMatch(/ajanda\.olayaGit/)
    expect(sistemApi).toMatch(/yaklasan\(gun\)\s*\{[\s\S]*?\/ajanda\/yaklasan/)
    expect(dosyaApi).toMatch(/ajanda\(params\)\s*\{[\s\S]*?\/exports\/ajanda/)
  })

  it('çapraz gezinme kaynak tipine göre yönlenir', () => {
    expect(view).toMatch(/case 'FATURA':[\s\S]*?path: '\/faturalar', query: \{ no: o\.kaynakNo \}/)
    expect(view).toMatch(/case 'PROJE_GOREVI':[\s\S]*?router\.push\('\/projeler'\)/)
    expect(view).toMatch(/case 'KISISEL_GOREV':[\s\S]*?aktifTab\.value = 2/)
    expect(view).toMatch(/case 'HATIRLATICI':[\s\S]*?aktifTab\.value = 3/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    const anahtarlar = [
      'yaklasanYok',
      'gun7',
      'gun30',
      'gun60',
      'gun90',
      'tumu',
      'tarih',
      'tip',
      'baslik',
      'islem',
      'olayaGit',
      'excel',
      'excelIndirildi',
      'excelHata'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
