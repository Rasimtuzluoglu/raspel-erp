import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('vue-i18n', () => ({
  useI18n: () => ({ t: (k) => k })
}))

vi.mock('../../api/index.js', () => ({
  faturaAPI: {
    cariUrunFiyatGecmisi: vi.fn(() => Promise.resolve({ data: { sonFiyat: 120 } }))
  },
  cariHesapAPI: {
    getFiyatlar: vi.fn(() => Promise.resolve({ data: [{ stokId: 7, fiyat: 110 }] }))
  },
  stokAPI: {
    getFiyatlar: vi.fn(() => Promise.resolve({ data: [{ ad: 'Toptan', fiyat: 100 }] }))
  }
}))

import { stokAPI, cariHesapAPI, faturaAPI } from '../../api/index.js'
import { useUrunFiyatlari } from '../useUrunFiyatlari.js'

describe('useUrunFiyatlari', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('cari+urun icin tum fiyat seviyelerini toplar', async () => {
    const { secenekler, yukle } = useUrunFiyatlari()
    await yukle(3, 7, 90)
    // son satis 120, cari ozel 110, toptan 100, liste 90
    expect(secenekler.value.map((s) => s.fiyat)).toEqual([120, 110, 100, 90])
    expect(faturaAPI.cariUrunFiyatGecmisi).toHaveBeenCalledWith(3, 7)
    expect(cariHesapAPI.getFiyatlar).toHaveBeenCalledWith(3)
    expect(stokAPI.getFiyatlar).toHaveBeenCalledWith(7)
  })

  it('aynı fiyat degerlerini tekillestirir', async () => {
    stokAPI.getFiyatlar.mockResolvedValueOnce({ data: [{ ad: 'A', fiyat: 120 }] })
    const { secenekler, yukle } = useUrunFiyatlari()
    await yukle(3, 7, 120)
    // 120 iki kez gelirse tek olur
    expect(secenekler.value.filter((s) => s.fiyat === 120).length).toBe(1)
  })

  it('cari yoksa sadece stok seviyeleri ve liste fiyati gelir', async () => {
    const { secenekler, yukle } = useUrunFiyatlari()
    await yukle(null, 7, 90)
    expect(faturaAPI.cariUrunFiyatGecmisi).not.toHaveBeenCalled()
    expect(cariHesapAPI.getFiyatlar).not.toHaveBeenCalled()
    expect(secenekler.value.map((s) => s.fiyat)).toEqual([100, 90])
  })

  it('stok secilmezse secenek olusmaz', async () => {
    const { secenekler, yukle } = useUrunFiyatlari()
    await yukle(3, null, 90)
    expect(secenekler.value).toEqual([])
  })
})
