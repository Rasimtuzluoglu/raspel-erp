import { describe, it, expect, beforeEach } from 'vitest'
import { useTheme, aksanMetinRengi, kontrastOrani } from '../useTheme.js'

const hexDizi = (hex) => {
  const h = String(hex).replace('#', '')
  return [0, 2, 4].map((i) => parseInt(h.substring(i, i + 2), 16))
}
const karistir = (a, b, oran) => a.map((v, i) => v + (b[i] - v) * oran)
const rgbHex = (dizi) => '#' + dizi.map((v) => Math.round(v).toString(16).padStart(2, '0')).join('')
/** `useTheme` ile ayni yumusak aksan zemini: %18 aksan + kart. */
const yumusakZemin = (renk, koyu) => rgbHex(
  karistir(koyu ? [20, 27, 38] : [255, 255, 255], hexDizi(renk), 0.18)
)

describe('useTheme', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('defaults to dark mode', () => {
    const { mode, isDark } = useTheme()
    expect(mode.value).toBe('dark')
    expect(isDark.value).toBe(true)
  })

  it('switches to light mode and persists', () => {
    const { mode, isDark, applyMode } = useTheme()
    applyMode('light')
    expect(mode.value).toBe('light')
    expect(isDark.value).toBe(false)
    expect(localStorage.getItem('raspel_erp_theme')).toBe('light')
  })

  it('applies accent color and persists', () => {
    const { accentColor, applyColor } = useTheme()
    applyColor('#ff0000')
    expect(accentColor.value).toBe('#ff0000')
    expect(localStorage.getItem('raspel_primary_color')).toBe('#ff0000')
    expect(document.documentElement.style.getPropertyValue('--primary-color')).toBe('#ff0000')
  })

  it('ignores empty color', () => {
    const { accentColor, applyColor } = useTheme()
    const onceki = accentColor.value
    applyColor('')
    expect(accentColor.value).toBe(onceki)
  })

  it('system mode persists and resolves isDark from media query', () => {
    const { mode, applyMode } = useTheme()
    applyMode('system')
    expect(mode.value).toBe('system')
    expect(localStorage.getItem('raspel_erp_theme')).toBe('system')
    expect(typeof useTheme().isDark.value).toBe('boolean')
  })
})

describe('aksen metin rengi (WCAG)', () => {
  const renkler = ['#000000', '#ffffff', '#0f766e', '#3b82f6', '#8b5cf6', '#f59e0b']

  for (const koyu of [true, false]) {
    it(`${koyu ? 'koyu' : 'acik'} temada marka renkleri >= 4.6 kontrast verir`, () => {
      for (const renk of renkler) {
        const metin = aksanMetinRengi(renk, koyu)
        expect(metin).toMatch(/^#[0-9a-f]{6}$/)
        expect(
          kontrastOrani(metin, yumusakZemin(renk, koyu)),
          `${renk} -> ${metin}`
        ).toBeGreaterThanOrEqual(4.6)
      }
    })
  }

  it('gecersiz renkte guvenli varsayilana duser', () => {
    expect(aksanMetinRengi('', true)).toBe('#2dd4bf')
    expect(aksanMetinRengi('', false)).toBe('#115e59')
    expect(kontrastOrani('gecersiz', '#ffffff')).toBe(0)
  })

  it('tema degisince inline --accent-text guncellenir', () => {
    const { applyColor, applyMode } = useTheme()
    applyColor('#0f766e')
    applyMode('dark')
    const koyu = document.documentElement.style.getPropertyValue('--accent-text')
    applyMode('light')
    const acik = document.documentElement.style.getPropertyValue('--accent-text')
    expect(koyu).toMatch(/^#[0-9a-f]{6}$/)
    expect(acik).toMatch(/^#[0-9a-f]{6}$/)
    expect(acik).not.toBe(koyu)
    expect(kontrastOrani(acik, '#ffffff')).toBeGreaterThanOrEqual(4.5)
  })
})
