import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Tahsilat revizyonu — Faz 1 kaynak-tabanlı doğrulama
 * (PageHeader + 5 KPI + sekmeler + sunucu yaşlandırma filtreleri).
 */

const SRC = join(process.cwd(), 'src')
const oku = (p) => readFileSync(join(SRC, p), 'utf8')

const view = oku('views/Tahsilat.vue')
const finans = oku('api/modules/finans.js')
const tr = oku('locales/tr.json')
const en = oku('locales/en.json')

describe('Tahsilat — Faz 1 (KPI, sekmeler, yaşlandırma)', () => {
  it('PageHeader başlık/alt başlık/ikon ve aksiyonlar içerir', () => {
    expect(view).toMatch(/<PageHeader/)
    expect(view).toMatch(/:title="t\('tahsilat\.title'\)"/)
    expect(view).toMatch(/:description="t\('tahsilat\.aciklama'\)"/)
    expect(view).toMatch(/icon="pi pi-money-bill"/)
    expect(view).toMatch(/tahsilat\.excelYaslandirma/)
    expect(view).toMatch(/tahsilat\.tahsilatGir/)
  })

  it('5 KPI kartı ve tıklama davranışı tanımlıdır', () => {
    expect(view).toMatch(/v-for="i in 5"/)
    expect(view).toMatch(/tahsilat\.toplamAlacak/)
    expect(view).toMatch(/tahsilat\.vadesiGecmis/)
    expect(view).toMatch(/tahsilat\.otuzGunIcinde/)
    expect(view).toMatch(/tahsilat\.kpiBuAyTahsilat/)
    expect(view).toMatch(/tahsilat\.kpiHedefGerceklesme/)
    expect(view).toMatch(/const kpiTikla = \(anahtar\)/)
    expect(view).toMatch(/:aria-pressed="kpiAktif\(/)
  })

  it('üç sekme ve sekme hafızası vardır', () => {
    expect(view).toMatch(/<TabView v-model:active-index="aktifSekme">/)
    expect(view).toMatch(/tahsilat\.tabYaslandirma/)
    expect(view).toMatch(/tahsilat\.tabGecmis/)
    expect(view).toMatch(/tahsilat\.tabPerformans/)
    expect(view).toMatch(/raspel_tahsilat_tab/)
    expect(view).toMatch(/const aktifSekme = ref\(sekmeOku\(\)\)/)
  })

  it('sunucu yaşlandırma filtreleri ve filtre hafızası bağlıdır', () => {
    expect(view).toMatch(/raspel_tahsilat_filtre/)
    expect(view).toMatch(/tahsilatAPI\.filtreliOzet\(filtreParams\(\)\)/)
    expect(view).toMatch(/if \(q\.value\?\.trim\(\)\) p\.q = q\.value\.trim\(\)/)
    expect(view).toMatch(/p\.aralik = aralik\.value/)
    expect(view).toMatch(/p\.sadeceGecikmis = true/)
    expect(view).toMatch(/p\.minTutar = minTutar\.value/)
  })

  it('finans API yaşlandırma/özet uçlarını sunar', () => {
    expect(finans).toMatch(/filtreliOzet\(params = \{\}\)\s*{\s*return apiClient\.get\('\/tahsilat', \{ params \}\)/)
    expect(finans).toMatch(/ozet\(params = \{\}\)/)
    expect(finans).toMatch(/performans\(params = \{\}\)\s*{\s*return apiClient\.get\('\/tahsilat\/performans'/)
  })

  it('erişilebilirlik ve mobil davranışlar korunur', () => {
    expect(view).toMatch(/aria-live="polite"/)
    expect(view).toMatch(/prefers-reduced-motion: reduce/)
    expect(view).toMatch(/yaslandirma-mobil/)
    expect(view).toMatch(/<SkeletonLoader/)
  })

  it('i18n anahtarları tr ve en için simetriktir', () => {
    const anahtarlar = [
      'kpiBuAyTahsilat',
      'kpiHedefGerceklesme',
      'tabYaslandirma',
      'tabGecmis',
      'tabPerformans',
      'aralikVadesiGelmemis',
      'aralik90Plus',
      'sadeceGecikmis',
      'minTutar',
      'filtreTemizle',
      'kayitDuyuru'
    ]
    for (const anahtar of anahtarlar) {
      expect(tr).toMatch(new RegExp(`"${anahtar}"`))
      expect(en).toMatch(new RegExp(`"${anahtar}"`))
    }
  })
})
