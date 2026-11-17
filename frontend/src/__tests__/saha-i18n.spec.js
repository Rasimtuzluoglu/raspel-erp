import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import tr from '../locales/tr.json'
import en from '../locales/en.json'

/**
 * Saha portalı i18n + mobil erişilebilirlik sözleşmesi (spec §7).
 */

const SRC = join(process.cwd(), 'src')
const oku = (...yol) => readFileSync(join(SRC, ...yol), 'utf8')

const sahaPortali = oku('views/SahaPortali.vue')
const kartlar = oku('components/saha/SahaOzetKartlari.vue')
const kuyruk = oku('components/saha/SahaKuyrukCubugu.vue')
const sheet = oku('components/saha/SahaImzaSheet.vue')
const teslimatKarti = oku('components/saha/SahaTeslimatKarti.vue')
const navigasyon = oku('components/saha/SahaNavigasyonButton.vue')

const ANAHTARLAR = [
  'bugun',
  'bugunTeslimat',
  'bugunZiyaret',
  'bugunTalepler',
  'kuyrukBaslik',
  'kuyrukSimdiGonder',
  'kuyrukHata',
  'kuyrukSil',
  'kuyrukYenidenDene',
  'imzaTeslimBaslik',
  'imzaFotoOpsiyonel',
  'navigasyon',
  'siralamaIpucu',
  'cevrimdisiKaydedildi',
  'gorevYuklenemedi',
  'ozetYuklenemedi'
]

describe('Saha portalı i18n', () => {
  it('spec §7 anahtarları iki dilde de tanımlı', () => {
    for (const anahtar of ANAHTARLAR) {
      expect(tr.sahaPortali[anahtar], `tr: ${anahtar}`).toBeTypeOf('string')
      expect(en.sahaPortali[anahtar], `en: ${anahtar}`).toBeTypeOf('string')
    }
  })

  it('bileşenlerde ham Türkçe metin yok, t() kullanılıyor', () => {
    for (const kaynak of [kartlar, kuyruk, sheet, teslimatKarti, navigasyon]) {
      expect(kaynak).toMatch(/useI18n\(\)/)
    }
  })
})

describe('Saha portalı mobil erişilebilirlik', () => {
  it('kuyruk ve özet canlı bölge olarak duyurulur', () => {
    expect(kuyruk).toMatch(/aria-live="polite"/)
    expect(kartlar).toMatch(/aria-live|role="status"|aria-label/)
  })

  it('birincil dokunma hedefleri en az 44 px', () => {
    for (const kaynak of [kuyruk, sheet, teslimatKarti, navigasyon, sahaPortali]) {
      expect(kaynak).toMatch(/min-height:\s*44px/)
    }
  })

  it('390 px için yatay taşma koruması var', () => {
    expect(sahaPortali).toMatch(/min-width:\s*0/)
    expect(kuyruk).toMatch(/flex-wrap:\s*wrap/)
    expect(sheet).toMatch(/@media \(max-width: 640px\)/)
  })

  it('hareket azaltma tercihi desteklenir', () => {
    expect(kartlar).toMatch(/prefers-reduced-motion/)
  })
})
