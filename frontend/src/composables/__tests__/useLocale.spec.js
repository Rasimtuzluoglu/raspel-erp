import { describe, it, expect, beforeEach, vi } from 'vitest'

const localeRef = { value: 'tr' }
const primevueRef = { config: { locale: {} } }

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ locale: localeRef })
}))

vi.mock('primevue/config', () => ({
  usePrimeVue: () => primevueRef
}))

import { useLocale } from '../useLocale.js'

describe('useLocale', () => {
  beforeEach(() => {
    localStorage.clear()
    localeRef.value = 'tr'
    primevueRef.config.locale = {}
  })

  it('returns active locale', () => {
    const { aktifDil } = useLocale()
    expect(aktifDil.value).toBe('tr')
  })

  it('switches to English and persists', () => {
    const { aktifDil, dilDegistir } = useLocale()
    dilDegistir('en')
    expect(aktifDil.value).toBe('en')
    expect(localStorage.getItem('lang')).toBe('en')
    expect(primevueRef.config.locale.dayNames[0]).toBe('Sunday')
  })

  it('falls back to Turkish for unknown values', () => {
    const { aktifDil, dilDegistir } = useLocale()
    dilDegistir('de')
    expect(aktifDil.value).toBe('tr')
    expect(primevueRef.config.locale.dayNames[0]).toBe('Pazar')
  })
})
