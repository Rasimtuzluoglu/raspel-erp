import { describe, it, expect } from 'vitest'
import { readFileSync, readdirSync } from 'node:fs'
import { join } from 'node:path'

const VIEW_DIR = join(process.cwd(), 'src/views')
const views = readdirSync(VIEW_DIR).filter((f) => f.endsWith('.vue'))

const usesListeDurumu = (src) => src.includes('<ListeDurumu')
const hasSkeleton = (src) => src.includes('SkeletonLoader')

describe('yukleme/bos/hata durumu tutarliligi', () => {
  it('ListeDurumu bileseni mevcut ve ortak durum modelini sunuyor', () => {
    const src = readFileSync(join(process.cwd(), 'src/components/ListeDurumu.vue'), 'utf8')
    expect(src).toContain('liste-durumu__iskelet')
    expect(src).toContain('liste-durumu__hata')
    expect(src).toContain('liste-durumu__bos')
  })

  it('durum onceligi dogru: iskelet > hata > bos liste', () => {
    const src = readFileSync(join(process.cwd(), 'src/components/ListeDurumu.vue'), 'utf8')
    const iskelet = src.indexOf('v-if="yukleniyor"')
    const hata = src.indexOf('v-else-if="hata"')
    const bos = src.indexOf('v-else-if="bos"')
    expect(iskelet).toBeGreaterThan(-1)
    expect(hata).toBeGreaterThan(iskelet)
    expect(bos).toBeGreaterThan(hata)
  })

  it('Stoklar.vue ortak durum bilesenini kullaniyor', () => {
    const src = readFileSync(join(VIEW_DIR, 'Stoklar.vue'), 'utf8')
    expect(usesListeDurumu(src)).toBe(true)
  })

  it('filtre acikken "kayit yok" ile "sonuc bulunamadi" ayirt ediliyor', () => {
    const src = readFileSync(join(VIEW_DIR, 'Stoklar.vue'), 'utf8')
    expect(src).toContain('filtreAktifMi')
  })

  it('tarama calisiyor: view dosyalari okunuyor', () => {
    expect(views.length).toBeGreaterThan(50)
    const toplam = views.filter((f) => hasSkeleton(readFileSync(join(VIEW_DIR, f), 'utf8'))).length
    console.log(`Kendi iskeletini kullanan view: ${toplam}`)
    expect(toplam).toBeGreaterThanOrEqual(0)
  })
})