import { setActivePinia, createPinia } from 'pinia'
import { useFaturaStore } from '../faturaStore.js'
import { faturaAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  faturaAPI: {
    getAll: vi.fn(),
    getById: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    updateDurum: vi.fn(),
    delete: vi.fn()
  }
}))

const mockFatura = { id: 1, faturaNo: 'F001', tutar: 500 }

describe('faturaStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useFaturaStore()
  })

  it('initializes with default state', () => {
    expect(store.faturalar).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getAllFaturalar fetches data', async () => {
    faturaAPI.getAll.mockResolvedValue({ data: [mockFatura] })
    const result = await store.getAllFaturalar()
    expect(store.faturalar).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockFatura])
  })

  it('getAllFaturalar handles error', async () => {
    faturaAPI.getAll.mockRejectedValue(new Error('Err'))
    await expect(store.getAllFaturalar()).rejects.toThrow('Err')
    expect(store.error).toBe('Err')
    expect(store.loading).toBe(false)
  })

  it('getFaturaById returns data', async () => {
    faturaAPI.getById.mockResolvedValue({ data: mockFatura })
    const result = await store.getFaturaById(1)
    expect(result).toEqual(mockFatura)
  })

  it('addFatura unshifts to list', async () => {
    faturaAPI.create.mockResolvedValue({ data: mockFatura })
    await store.addFatura(mockFatura)
    expect(store.faturalar[0].faturaNo).toBe('F001')
  })

  it('updateDurum updates status', async () => {
    store.faturalar = [{ id: 1, durum: 'TASLAK' }]
    const updated = { id: 1, durum: 'ONAYLANDI' }
    faturaAPI.updateDurum.mockResolvedValue({ data: updated })
    await store.updateDurum(1, 'ONAYLANDI')
    expect(store.faturalar[0].durum).toBe('ONAYLANDI')
  })

  it('updateFatura updates item', async () => {
    store.faturalar = [{ id: 1, tutar: 100 }]
    const updated = { id: 1, tutar: 200 }
    faturaAPI.update.mockResolvedValue({ data: updated })
    await store.updateFatura(1, updated)
    expect(store.faturalar[0].tutar).toBe(200)
  })

  it('deleteFatura removes item', async () => {
    store.faturalar = [mockFatura, { id: 2 }]
    faturaAPI.delete.mockResolvedValue({})
    await store.deleteFatura(1)
    expect(store.faturalar).toHaveLength(1)
  })
})
