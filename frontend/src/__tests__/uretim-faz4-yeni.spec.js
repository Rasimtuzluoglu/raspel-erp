import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Üretim revizyonu — Faz 4 kaynak-tabanlı doğrulama (yeni)
 * (maliyet ve fire analizi).
 */

const SRC = join(process.cwd(), 'src')
const ROOT = join(process.cwd(), '..')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')
const okuB = (p) => readFileSync(join(ROOT, 'backend/src/main/java/com/raspel/erp', p), 'utf8')

const view = oku('views/Uretim.vue')
const stokApi = oku('api/modules/stok.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

const controller = okuB('controller/envanter/UretimController.java')
const service = okuB('service/envanter/UretimService.java')
const repo = okuB('repository/envanter/UretimEmriRepository.java')
const analizDto = okuB('dto/envanter/UretimAnalizDTO.java')

describe('Üretim — Faz 4 (analiz)', () => {
  it('backend: dönem analizi DTO, sorgu ve hesaplar', () => {
    expect(analizDto).toMatch(/private BigDecimal fireOrani;/)
    expect(analizDto).toMatch(/private BigDecimal birimMaliyet;/)
    expect(controller).toMatch(/@GetMapping\("\/analiz"\)/)
    expect(service).toMatch(/public UretimAnalizDTO analiz\(Long sirketId, LocalDate baslangic, LocalDate bitis\)/)
    expect(service).toMatch(/tamamlananlarAralikta/)
    expect(service).toMatch(/\.trend\(trend\)/)
    expect(service).toMatch(/\.urunler\(urunler\)/)
    expect(repo).toMatch(/tamamlananlarAralikta\(/)
  })

  it('frontend: analiz sekmesi, trend ve ürün tablosu', () => {
    expect(view).toMatch(/uretim\.analizTab/)
    expect(view).toMatch(/const analizYukle = async/)
    expect(view).toMatch(/const barYuzde = \(v\) =>/)
    expect(view).toMatch(/class="trend-liste"/)
    expect(view).toMatch(/analiz\.urunler \|\| \[\]/)
    expect(view).toMatch(/analizExcelIndir/)
    expect(stokApi).toMatch(/analiz\(params = \{\}\)\s*\{[\s\S]*?\/uretim\/analiz/)
  })

  it('sekme açılınca analiz yüklenir', () => {
    expect(view).toMatch(/if \(v === 5 && !analizYuklendi\.value\) analizYukle\(\)/)
    expect(view).toMatch(/if \(aktifSekme\.value === 5\) analizYukle\(\)/)
  })

  it('i18n anahtarları tr ve en için tam', () => {
    for (const anahtar of [
      'analizTab',
      'analizDonem',
      'analizUygula',
      'analizTrend',
      'analizUrunBazli',
      'analizYok',
      'analizYuklenemedi',
      'emirSayisi'
    ]) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
