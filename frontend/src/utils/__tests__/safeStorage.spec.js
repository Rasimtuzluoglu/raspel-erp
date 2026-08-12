import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import { safeGet, safeSet } from '../safeStorage.js'

describe('safeStorage.js', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('safeGet returns default when key missing', () => {
    expect(safeGet('yok', 'varsayilan')).toBe('varsayilan')
    expect(safeGet('yok')).toBeNull()
  })

  it('safeSet/safeGet round-trips JSON', () => {
    safeSet('anahtar', { a: 1, b: [1, 2, 3] })
    expect(safeGet('anahtar')).toEqual({ a: 1, b: [1, 2, 3] })
  })

  it('safeGet returns default on corrupt JSON', () => {
    localStorage.setItem('bozuk', '{not json')
    expect(safeGet('bozuk', 'fallback')).toBe('fallback')
  })

  it('safeSet swallows quota errors', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('quota')
    })
    expect(() => safeSet('a', 'b')).not.toThrow()
  })
})
