import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

describe('useChartTema', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    document.documentElement.setAttribute('data-theme', 'dark')
    document.documentElement.style.setProperty('--text-secondary', '#cbd5e1')
    document.documentElement.style.setProperty('--border', 'rgba(148, 163, 184, 0.18)')
  })

  it('CSS degiskenlerinden tema renklerini okur', async () => {
    const { useChartTema } = await import('../useChartTema.js')
    const { palet } = useChartTema()
    expect(palet.value.metin).toBe('#cbd5e1')
    expect(palet.value.izgara).toContain('148')
    expect(typeof palet.value.vurgu).toBe('string')
  })

  it('lejant renkli ve noktali secenek uretir', async () => {
    const { useChartTema } = await import('../useChartTema.js')
    const { lejant } = useChartTema()
    const l = lejant()
    expect(l.position).toBe('bottom')
    expect(l.labels.color).toBe('#cbd5e1')
    expect(l.labels.usePointStyle).toBe(true)
  })

  it('CSS degiskeni yoksa varsayilana duser', async () => {
    vi.resetModules()
    document.documentElement.style.removeProperty('--text-secondary')
    document.documentElement.style.removeProperty('--border')
    const { useChartTema } = await import('../useChartTema.js')
    const { palet } = useChartTema()
    expect(palet.value.metin).toBe('#94a3b8')
    expect(palet.value.izgara).toContain('148')
  })
})
