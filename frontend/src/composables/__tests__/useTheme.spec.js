import { describe, it, expect, beforeEach } from 'vitest'
import { useTheme } from '../useTheme.js'

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
