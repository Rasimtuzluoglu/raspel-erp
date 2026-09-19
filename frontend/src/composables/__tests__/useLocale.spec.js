import { describe, it, expect, beforeEach, vi } from 'vitest'

const localeRef = { value: 'tr' }
const primevueRef = { config: { locale: {} } }

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ locale: localeRef })
}))

vi.mock('primevue/config', () => ({
  usePrimeVue: () => primevueRef
}))

// useLocale artik i18n.js'ten tembel dil yukleme fonksiyonunu import eder;
// burada no-op ile mock'lanir (gercek vue-i18n ornegi testte kurulmaz).
vi.mock('../../i18n.js', () => ({
  ingilizceYukle: vi.fn(() => Promise.resolve())
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

  it('switches to English and persists', async () => {
    const { aktifDil, dilDegistir } = useLocale()
    await dilDegistir('en')
    expect(aktifDil.value).toBe('en')
    expect(localStorage.getItem('lang')).toBe('en')
    expect(primevueRef.config.locale.dayNames[0]).toBe('Sunday')
  })

  it('falls back to Turkish for unknown values', async () => {
    const { aktifDil, dilDegistir } = useLocale()
    await dilDegistir('de')
    expect(aktifDil.value).toBe('tr')
    expect(primevueRef.config.locale.dayNames[0]).toBe('Pazar')
  })
})
