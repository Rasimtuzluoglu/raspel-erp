import { describe, it, expect, vi, beforeEach } from 'vitest'

const { ozet } = vi.hoisted(() => ({ ozet: vi.fn() }))
vi.mock('../../api/index.js', () => ({ sahaAPI: { ozet } }))

import { useSahaOzet } from '../useSahaOzet.js'

describe('useSahaOzet', () => {
  beforeEach(() => {
    ozet.mockReset()
  })

  it('ozeti yukler', async () => {
    ozet.mockResolvedValue({ data: { teslimat: { bekleyen: 2 }, ziyaret: { bugun: 1 } } })
    const { ozet: veri, yukle, yukleniyor } = useSahaOzet()

    const sonuc = await yukle({ zorla: true })

    expect(ozet).toHaveBeenCalledWith()
    expect(sonuc.teslimat.bekleyen).toBe(2)
    expect(veri.value.ziyaret.bugun).toBe(1)
    expect(yukleniyor.value).toBe(false)
  })

  it('hata durumunda mesaji saklar, ozeti null birakir', async () => {
    ozet.mockRejectedValue(new Error('ozet yok'))
    const { ozet: veri, hata, yukle } = useSahaOzet()

    await yukle({ zorla: true })

    expect(veri.value).toBeNull()
    expect(hata.value).toBe('ozet yok')
  })

  it('60 saniye icinde tekrar istek atmaz', async () => {
    ozet.mockResolvedValue({ data: { teslimat: {} } })
    const { yukle } = useSahaOzet()

    await yukle({ zorla: true })
    await yukle()

    expect(ozet).toHaveBeenCalledTimes(1)
  })

  it('zorla parametresi throttle i atlar', async () => {
    ozet.mockResolvedValue({ data: { teslimat: {} } })
    const { yukle } = useSahaOzet()

    await yukle({ zorla: true })
    await yukle({ zorla: true })

    expect(ozet).toHaveBeenCalledTimes(2)
  })
})
