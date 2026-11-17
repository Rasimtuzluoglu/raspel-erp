import { describe, it, expect } from 'vitest'
import { readFileSync } from 'node:fs'
import { join } from 'node:path'

/**
 * Arama kutularında büyüteç ikonunun metinle çakışmasını önleyen ortak
 * kurallar. Bu kurallar kaldırılırsa PrimeVue `.p-inputtext` dolgusu devreye
 * girer ve ikon placeholder metninin üstüne biner (Denetim/Teslimatlar'da
 * görülen hata).
 */
const css = readFileSync(join(process.cwd(), 'src', 'assets', 'app.css'), 'utf8')

const ARAMA_BLOGU = css.slice(css.indexOf('Arama kutulari - tek gorsel dil'))

describe('arama kutusu ortak stili', () => {
  it('tum arama sarmalayicilarini kapsar', () => {
    expect(ARAMA_BLOGU).toContain('.arama-kutu')
    expect(ARAMA_BLOGU).toContain('.arac-arama')
    expect(ARAMA_BLOGU).toContain('.tablo-arama')
    expect(ARAMA_BLOGU).toContain('.surucu-arama')
  })

  it('ikonu absolute konumdan cikarip akista tutar', () => {
    expect(ARAMA_BLOGU).toMatch(/> i\.pi-search \{[\s\S]*position: static !important/)
    expect(ARAMA_BLOGU).toMatch(/> i\.pi-search \{[\s\S]*transform: none !important/)
  })

  it('input dolgusunu sifirlar (ikon metne binmesin)', () => {
    expect(ARAMA_BLOGU).toMatch(/\.arama-input \{[\s\S]*padding-left: 0 !important/)
    expect(ARAMA_BLOGU).toMatch(/\.arama-input \{[\s\S]*border: none !important/)
  })

  it('sarmalayiciya pill cercevesi ve odak halkasi verir', () => {
    expect(ARAMA_BLOGU).toMatch(/border: 1px solid var\(--border\)/)
    expect(ARAMA_BLOGU).toContain(':focus-within')
    expect(ARAMA_BLOGU).toContain('var(--accent-soft)')
  })

  it('IconField icindeki ikon icin sol dolguyu korur', () => {
    expect(ARAMA_BLOGU).toMatch(/\.p-iconfield \.p-inputtext \{[\s\S]*padding-inline-start: 2\.5rem !important/)
    expect(ARAMA_BLOGU).toMatch(/\.p-iconfield \.p-inputicon \{[\s\S]*var\(--text-muted\) !important/)
  })
})
