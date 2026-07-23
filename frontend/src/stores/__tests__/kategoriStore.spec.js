import { setActivePinia, createPinia } from 'pinia'
import { useKategoriStore } from '../kategoriStore.js'
import { kategoriAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  kategoriAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    delete: vi.fn()
  }
}))

const mockKategori = { id: 1, ad: 'Gelir', tur: 'GELIR' }

describe('kategoriStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
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
})
