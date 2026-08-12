import { setActivePinia, createPinia } from 'pinia'
import { useDovizStore } from '../dovizStore.js'
import { dovizAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  dovizAPI: {
    getKurlar: vi.fn(),
    guncelle: vi.fn()
  }
}))

describe('dovizStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useDovizStore()
  })

  it('initializes with default kurlar', () => {
    expect(store.kurlar.length).toBe(5)
    expect(store.aktifParaBirimi).toBe('TRY')
  })

  it('getKur returns TRY identity', () => {
    const kur = store.getKur('TRY')
    expect(kur.kod).toBe('TRY')
    expect(kur.satisFiyati).toBe(1)
  })

  it('getKur returns known kur', () => {
    const kur = store.getKur('USD')
    expect(kur.kod).toBe('USD')
    expect(kur.satisFiyati).toBeGreaterThan(0)
  })

  it('getKur returns fallback for unknown kod', () => {
    const kur = store.getKur('XXX')
    expect(kur.kod).toBe('XXX')
    expect(kur.satisFiyati).toBe(1)
  })

  it('convert returns 0 for null/0', () => {
    expect(store.convert(null)).toBe(0)
    expect(store.convert(0)).toBe(0)
  })

  it('convert returns same value for identical currencies', () => {
    expect(store.convert(100, 'USD', 'USD')).toBe(100)
  })

  it('convert converts through TRY', () => {
    const result = store.convert(100, 'TRY', 'USD')
    expect(result).toBeGreaterThan(0)
    expect(result).toBeLessThan(100)
  })

  it('formatPara formats TRY with symbol', () => {
    expect(store.formatPara(1000, 'TRY')).toContain('₺')
  })

  it('kurlariYukle loads from API', async () => {
    dovizAPI.getKurlar.mockResolvedValue({ data: [{ kod: 'USD', satisFiyati: 50 }] })
    await store.kurlariYukle()
    expect(store.kurlar.length).toBe(1)
    expect(store.loading).toBe(false)
  })

  it('kurlariYukle keeps defaults on error', async () => {
    dovizAPI.getKurlar.mockRejectedValue(new Error('down'))
    await store.kurlariYukle()
    expect(store.kurlar.length).toBe(5)
    expect(store.loading).toBe(false)
  })
})
