import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Regresyon — Stoklar liste gorunurlugu.
 *
 * ListeDurumu commit'i (e6b5e10) tablo kosuluna yanlislikla `filtreAktifMi()`
 * ekledi: filtre yokken tablo HIC render edilmiyor, yerine "bos" durumu
 * cikiyordu. Bu test kosulun geri donmemesini saglar.
 */

const src = readFileSync(join(process.cwd(), 'src/views/Stoklar.vue'), 'utf8')

describe('Stoklar liste gorunurlugu (regresyon)', () => {
  it('tablo render kosulu filtre sartina bagli degil', () => {
    const satir = src.split(/\r?\n/).find((s) => s.includes("gosterim === 'tablo'") && s.includes('<template'))
    expect(satir).toBeTruthy()
    expect(satir).not.toContain('filtreAktifMi()')
  })

  it('ListeDurumu yalniz yukleme/hata icin gosterilir', () => {
    const idx = src.indexOf('<ListeDurumu')
    const blok = src.slice(idx, idx + 200)
    expect(blok).not.toContain('!filtreAktifMi()')
    expect(blok).toContain('stokStore.loading || stokStore.error')
  })

  it('bos mesaji filtre durumuna gore secilir (filtreAktifMi korunur)', () => {
    expect(src).toContain('filtreAktifMi')
  })
})
