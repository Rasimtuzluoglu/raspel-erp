import { setActivePinia, createPinia } from 'pinia'
import { useKasaStore } from '../kasaStore.js'
import { kasaAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  kasaAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

const mockKasa = { id: 1, ad: 'Ana Kasa', bakiye: 10000 }

describe('kasaStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useKasaStore()
  })

  it('initializes with default state', () => {
    expect(store.kasalar).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('getAllKasalar fetches data', async () => {
    kasaAPI.getAll.mockResolvedValue({ data: [mockKasa] })
    const result = await store.getAllKasalar()
    expect(store.kasalar).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockKasa])
  })

  it('addKasa pushes to list', async () => {
    kasaAPI.create.mockResolvedValue({ data: mockKasa })
    const result = await store.addKasa(mockKasa)
    expect(store.kasalar).toContainEqual(mockKasa)
    expect(result).toEqual(mockKasa)
  })

  it('updateKasa updates existing', async () => {
    store.kasalar = [{ id: 1, ad: 'Eski' }]
    const updated = { id: 1, ad: 'Yeni' }
    kasaAPI.update.mockResolvedValue({ data: updated })
    await store.updateKasa(1, updated)
    expect(store.kasalar[0].ad).toBe('Yeni')
  })

  it('deleteKasa removes item', async () => {
    store.kasalar = [mockKasa, { id: 2 }]
    kasaAPI.delete.mockResolvedValue({})
    await store.deleteKasa(1)
    expect(store.kasalar).toHaveLength(1)
  })

  it('handles error in getAllKasalar', async () => {
    kasaAPI.getAll.mockRejectedValue(new Error('Fail'))
    await expect(store.getAllKasalar()).rejects.toThrow('Fail')
    expect(store.loading).toBe(false)
  })
})
