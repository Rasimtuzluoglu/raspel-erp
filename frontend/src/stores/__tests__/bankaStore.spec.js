import { setActivePinia, createPinia } from 'pinia'
import { useBankaStore } from '../bankaStore.js'
import { bankaAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  bankaAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

const mockBanka = { id: 1, ad: 'Ziraat', hesapNo: '123456' }

describe('bankaStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useBankaStore()
  })

  it('initializes with default state', () => {
    expect(store.bankalar).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getAllBankalar sets bankalar', async () => {
    bankaAPI.getAll.mockResolvedValue({ data: [mockBanka] })
    const result = await store.getAllBankalar()
    expect(store.bankalar).toHaveLength(1)
    expect(store.bankalar[0].ad).toBe('Ziraat')
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockBanka])
  })

  it('getAllBankalar handles error', async () => {
    bankaAPI.getAll.mockRejectedValue(new Error('Network error'))
    await expect(store.getAllBankalar()).rejects.toThrow('Network error')
    expect(store.error).toBe('Network error')
    expect(store.loading).toBe(false)
  })

  it('addBanka pushes to bankalar', async () => {
    bankaAPI.create.mockResolvedValue({ data: mockBanka })
    const result = await store.addBanka(mockBanka)
    expect(store.bankalar).toContainEqual(mockBanka)
    expect(result).toEqual(mockBanka)
  })

  it('updateBanka updates existing banka', async () => {
    store.bankalar = [{ id: 1, ad: 'Eski' }]
    const updated = { id: 1, ad: 'Yeni' }
    bankaAPI.update.mockResolvedValue({ data: updated })
    const result = await store.updateBanka(1, updated)
    expect(store.bankalar[0].ad).toBe('Yeni')
    expect(result).toEqual(updated)
  })

  it('deleteBanka removes banka', async () => {
    store.bankalar = [mockBanka, { id: 2, ad: 'Is' }]
    bankaAPI.delete.mockResolvedValue({})
    await store.deleteBanka(1)
    expect(store.bankalar).toHaveLength(1)
    expect(store.bankalar[0].id).toBe(2)
  })
})
