import { describe, it, expect, beforeEach } from 'vitest'
import { useYakinZamanda } from '../useYakinZamanda.js'

describe('useYakinZamanda', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('returns empty list initially', () => {
    const { liste } = useYakinZamanda()
    expect(liste()).toEqual([])
  })

  it('saves an item to recent list', () => {
    const { kaydet, liste } = useYakinZamanda()
    kaydet('fatura', 1, 'FTR-1', '/faturalar/1')
    const items = liste()
    expect(items).toHaveLength(1)
    expect(items[0].tur).toBe('fatura')
    expect(items[0].id).toBe(1)
    expect(items[0].baslik).toBe('FTR-1')
  })

  it('deduplicates by tur+id and moves to front', () => {
    const { kaydet, liste } = useYakinZamanda()
    kaydet('fatura', 1, 'FTR-1')
    kaydet('cari', 2, 'Cari 2')
    kaydet('fatura', 1, 'FTR-1 güncel')
    const items = liste()
    expect(items).toHaveLength(2)
    expect(items[0].baslik).toBe('FTR-1 güncel')
  })

  it('caps list at 10 items', () => {
    const { kaydet, liste } = useYakinZamanda()
    for (let i = 1; i <= 12; i++) kaydet('fatura', i, 'FTR-' + i)
    expect(liste()).toHaveLength(10)
  })
})
