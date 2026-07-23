import { setActivePinia, createPinia } from 'pinia'
import { useCariHesapStore } from '../cariHesapStore.js'
import { cariHesapAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  cariHesapAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn(),
    search: vi.fn()
  }
}))

const mockCari = { id: 1, ad: 'Acme', bakiye: 1000 }

describe('cariHesapStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useCariHesapStore()
  })

  it('initializes with default state', () => {
    expect(store.cariHesaplar).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getAllCariHesaplar fetches data', async () => {
    cariHesapAPI.getAll.mockResolvedValue({ data: [mockCari] })
    const result = await store.getAllCariHesaplar()
    expect(store.cariHesaplar).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockCari])
  })

  it('getAllCariHesaplar handles error', async () => {
    cariHesapAPI.getAll.mockRejectedValue(new Error('Fail'))
    await expect(store.getAllCariHesaplar()).rejects.toThrow('Fail')
    expect(store.error).toBe('Fail')
    expect(store.loading).toBe(false)
  })

  it('addCariHesap pushes to list', async () => {
    cariHesapAPI.create.mockResolvedValue({ data: mockCari })
    const result = await store.addCariHesap(mockCari)
    expect(store.cariHesaplar).toContainEqual(mockCari)
    expect(result).toEqual(mockCari)
  })

  it('updateCariHesap updates existing item', async () => {
    store.cariHesaplar = [{ id: 1, ad: 'Eski' }]
    const updated = { id: 1, ad: 'Yeni' }
    cariHesapAPI.update.mockResolvedValue({ data: updated })
    await store.updateCariHesap(1, updated)
    expect(store.cariHesaplar[0].ad).toBe('Yeni')
  })

  it('deleteCariHesap removes item', async () => {
    store.cariHesaplar = [mockCari, { id: 2, ad: 'B' }]
    cariHesapAPI.delete.mockResolvedValue({})
    await store.deleteCariHesap(1)
    expect(store.cariHesaplar).toHaveLength(1)
  })

  it('ara searches and sets results', async () => {
    cariHesapAPI.search.mockResolvedValue({ data: [mockCari] })
    const result = await store.ara('Acme')
    expect(store.cariHesaplar).toEqual([mockCari])
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockCari])
  })
})
