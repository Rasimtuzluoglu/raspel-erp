import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { useGeriAl } from '../useGeriAl.js'

describe('useGeriAl', () => {
  beforeEach(() => { vi.useFakeTimers() })
  afterEach(() => { vi.useRealTimers() })

  it('silVeGeriAl çubuğu gösterir', () => {
    const { durum, silVeGeriAl } = useGeriAl()
    silVeGeriAl({ veri: { id: 1 }, metin: 'Silindi', geriYukle: vi.fn() })
    expect(durum.gorunur).toBe(true)
    expect(durum.metin).toBe('Silindi')
  })

  it('geriAl kaydı geri yükler ve çubuğu gizler', async () => {
    const { durum, silVeGeriAl, geriAl } = useGeriAl()
    const geriYukle = vi.fn().mockResolvedValue()
    silVeGeriAl({ veri: { id: 5 }, metin: 'Silindi', geriYukle })
    await geriAl()
    expect(geriYukle).toHaveBeenCalledWith({ id: 5 })
    expect(durum.gorunur).toBe(false)
  })

  it('6 saniye sonra otomatik gizlenir', () => {
    const { durum, silVeGeriAl } = useGeriAl()
    silVeGeriAl({ veri: { id: 1 }, metin: 'Silindi', geriYukle: vi.fn() })
    expect(durum.gorunur).toBe(true)
    vi.advanceTimersByTime(6000)
    expect(durum.gorunur).toBe(false)
  })

  it('gizle çağrısı verileri temizler', () => {
    const { durum, silVeGeriAl, gizle } = useGeriAl()
    silVeGeriAl({ veri: { id: 1 }, metin: 'Silindi', geriYukle: vi.fn() })
    gizle()
    expect(durum.gorunur).toBe(false)
    expect(durum.veri).toBeNull()
  })
})
