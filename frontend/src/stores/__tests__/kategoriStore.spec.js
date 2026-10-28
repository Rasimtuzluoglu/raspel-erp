import { setActivePinia, createPinia } from 'pinia'
import { useKategoriStore } from '../kategoriStore.js'
import { kategoriAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  kategoriAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    delete: vi.fn(),
    update: vi.fn(),
    agac: vi.fn(),
    ozet: vi.fn(),
    secenekler: vi.fn(),
    kullanim: vi.fn(),
    arsivle: vi.fn(),
    aktiflestir: vi.fn(),
    birlestir: vi.fn(),
    sistemKategorileri: vi.fn()
  }
}))

const mockKategori = { id: 1, ad: 'Gelir', tur: 'GELIR' }

describe('kategoriStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    store = useKategoriStore()
  })

  it('initializes with default state', () => {
    expect(store.kategoriler).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('getAllKategoriler fetches data', async () => {
    kategoriAPI.getAll.mockResolvedValue({ data: [mockKategori] })
    const result = await store.getAllKategoriler()
    expect(store.kategoriler).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockKategori])
  })

  it('addKategori pushes to list', async () => {
    kategoriAPI.create.mockResolvedValue({ data: mockKategori })
    const result = await store.addKategori(mockKategori)
    expect(store.kategoriler).toContainEqual(mockKategori)
    expect(result).toEqual(mockKategori)
  })

  it('deleteKategori removes from list', async () => {
    store.kategoriler = [mockKategori, { id: 2, ad: 'Gider' }]
    kategoriAPI.delete.mockResolvedValue({})
    await store.deleteKategori(1)
    expect(store.kategoriler).toHaveLength(1)
  })

  it('handles error in getAllKategoriler', async () => {
    kategoriAPI.getAll.mockRejectedValue(new Error('Fail'))
    await expect(store.getAllKategoriler()).rejects.toThrow('Fail')
    expect(store.loading).toBe(false)
  })

  it('agacGetir agaci state e yazar ve dizi olmayan yaniti bosaltir', async () => {
    const agac = [{ id: 1, ad: 'Satış', altKategoriler: [] }]
    kategoriAPI.agac.mockResolvedValue({ data: agac })
    await expect(store.agacGetir({ kapsam: 'KASA_GELIR' })).resolves.toEqual(agac)
    expect(kategoriAPI.agac).toHaveBeenCalledWith({ kapsam: 'KASA_GELIR' })
    expect(store.agac).toEqual(agac)

    kategoriAPI.agac.mockResolvedValue({ data: {} })
    await store.agacGetir()
    expect(store.agac).toEqual([])
  })

  it('ozetGetir varsayilan alanlarla birlestirir', async () => {
    kategoriAPI.ozet.mockResolvedValue({ data: { toplam: 3, aktif: 2 } })
    const ozet = await store.ozetGetir({ kapsam: 'STOK' })
    expect(ozet).toEqual({ toplam: 3, aktif: 2, pasif: 0, altKategori: 0 })
    expect(store.ozet.toplam).toBe(3)
  })

  it('seceneklerGetir kapsam basina onbellekler; zorla tazeler', async () => {
    const secenekler = [{ id: 1, ad: 'Gelir', ustKategoriId: null }]
    kategoriAPI.secenekler.mockResolvedValue({ data: secenekler })

    const ilk = await store.seceneklerGetir('KASA_GELIR')
    const ikinci = await store.seceneklerGetir('KASA_GELIR')

    expect(ilk).toEqual(secenekler)
    expect(ikinci).toEqual(secenekler)
    expect(kategoriAPI.secenekler).toHaveBeenCalledTimes(1)

    await store.seceneklerGetir('KASA_GELIR', { zorla: true })
    expect(kategoriAPI.secenekler).toHaveBeenCalledTimes(2)

    await store.seceneklerGetir('KASA_GIDER')
    expect(kategoriAPI.secenekler).toHaveBeenCalledTimes(3)
  })

  it('kullanim, arsiv, aktiflestir, birlestir ve sistem kategorileri uclarini kullanir', async () => {
    const kullanim = { kategoriId: 1, toplam: 4, detaylar: [] }
    kategoriAPI.kullanim.mockResolvedValue({ data: kullanim })
    await expect(store.kullanimGetir(1)).resolves.toEqual(kullanim)

    kategoriAPI.arsivle.mockResolvedValue({ data: { id: 1 } })
    kategoriAPI.aktiflestir.mockResolvedValue({ data: { id: 1 } })
    kategoriAPI.birlestir.mockResolvedValue({ data: { kaynakId: 1, hedefId: 2 } })

    await store.seceneklerGetir('STOK')
    await store.arsivle(1)
    expect(store.secenekOnbellek).toEqual({})

    await store.aktiflestir(1)
    await store.birlestir({ kaynakId: 1, hedefId: 2 })
    expect(kategoriAPI.birlestir).toHaveBeenCalledWith({ kaynakId: 1, hedefId: 2 })

    kategoriAPI.sistemKategorileri.mockResolvedValue({ data: [{ kod: 'YAKIT', ad: 'Yakıt', kapsam: 'MASRAF' }] })
    const sistem = await store.sistemKategorileriGetir()
    expect(sistem).toHaveLength(1)
    expect(store.sistemKategoriler).toHaveLength(1)
  })
})
