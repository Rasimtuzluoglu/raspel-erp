import { describe, it, expect, vi, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

const sayfali = vi.fn()
vi.mock('../../api/index.js', () => ({ depoAPI: { sayfali } }))

import { useDepoStore } from '../depoStore.js'

describe('depoStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    sayfali.mockReset()
  })

  it('sayfa yanitini content/totalElements olarak map eder', async () => {
    sayfali.mockResolvedValue({ data: { content: [{ id: 1, ad: 'Ana Depo' }], totalElements: 7 } })
    const store = useDepoStore()
    await store.yukle()
    expect(store.liste).toHaveLength(1)
    expect(store.toplam).toBe(7)
    expect(sayfali).toHaveBeenCalledWith(expect.objectContaining({ page: 0, size: 20 }))
  })

  it('filtre degisince sayfa sifirlanir ve istek atilir', async () => {
    sayfali.mockResolvedValue({ data: { content: [], totalElements: 0 } })
    const store = useDepoStore()
    store.sayfa = 3
    store.filtreDegistir({ q: 'merkez', kritik: true })
    expect(store.sayfa).toBe(0)
    expect(sayfali).toHaveBeenCalledWith(expect.objectContaining({ q: 'merkez', kritik: true, page: 0 }))
  })

  it('sayfa degisince yeni sayfa istenir', async () => {
    sayfali.mockResolvedValue({ data: { content: [], totalElements: 0 } })
    const store = useDepoStore()
    store.sayfaDegistir(2)
    expect(store.sayfa).toBe(2)
    expect(sayfali).toHaveBeenCalledWith(expect.objectContaining({ page: 2 }))
  })

  it('hata durumunda liste bosalir ve mesaj yazilir', async () => {
    sayfali.mockRejectedValue({ response: { data: { message: 'Sunucu hatasi' } } })
    const store = useDepoStore()
    await store.yukle()
    expect(store.liste).toEqual([])
    expect(store.hata).toBe('Sunucu hatasi')
  })
})
