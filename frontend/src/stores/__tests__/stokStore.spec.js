import { setActivePinia, createPinia } from 'pinia'
import { useStokStore } from '../stokStore.js'
import { stokAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  stokAPI: {
    getAll: vi.fn(),
    ara: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

const mockStok = { id: 1, ad: 'Kalem', miktar: 100, minMiktar: 10 }

describe('stokStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useStokStore()
  })

  it('initializes with default state', () => {
    expect(store.stoklar).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.dusukStoklar).toEqual([])
  })

  it('getAll fetches data', async () => {
    stokAPI.getAll.mockResolvedValue({ data: [mockStok] })
    const result = await store.getAll()
    expect(store.stoklar).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockStok])
  })

  it('ara searches and sets results', async () => {
    stokAPI.ara.mockResolvedValue({ data: [mockStok] })
    const result = await store.ara('Kalem')
    expect(store.stoklar).toEqual([mockStok])
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockStok])
  })

  it('addStok pushes to list', async () => {
    stokAPI.create.mockResolvedValue({ data: mockStok })
    const result = await store.addStok(mockStok)
    expect(store.stoklar).toContainEqual(mockStok)
    expect(result).toEqual(mockStok)
  })

  it('updateStok updates existing', async () => {
    store.stoklar = [{ id: 1, ad: 'Eski', miktar: 50 }]
    const updated = { id: 1, ad: 'Yeni', miktar: 75 }
    stokAPI.update.mockResolvedValue({ data: updated })
    await store.updateStok(1, updated)
    expect(store.stoklar[0].ad).toBe('Yeni')
  })

  it('deleteStok removes from list', async () => {
    store.stoklar = [mockStok, { id: 2, ad: 'Silgi' }]
    stokAPI.delete.mockResolvedValue({})
    await store.deleteStok(1)
    expect(store.stoklar).toHaveLength(1)
  })

  it('dusukStoklar computed filters correctly', () => {
    store.stoklar = [
      { id: 1, ad: 'A', miktar: 5, minMiktar: 10 },
      { id: 2, ad: 'B', miktar: 20, minMiktar: 10 },
      { id: 3, ad: 'C', miktar: 15, minMiktar: 10 }
    ]
    expect(store.dusukStoklar).toHaveLength(1)
    expect(store.dusukStoklar[0].ad).toBe('A')
  })

  it('handles error in getAll', async () => {
    stokAPI.getAll.mockRejectedValue(new Error('API error'))
    await expect(store.getAll()).rejects.toThrow('API error')
    expect(store.loading).toBe(false)
  })
})
