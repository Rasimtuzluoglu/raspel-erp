import { setActivePinia, createPinia } from 'pinia'
import { createCrudStore } from '../createCrudStore.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

const mockApi = {
  getAll: vi.fn(),
  create: vi.fn(),
  update: vi.fn(),
  delete: vi.fn()
}

const useTestStore = createCrudStore('test', mockApi)

const item = { id: 1, ad: 'Test' }

describe('createCrudStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useTestStore()
    vi.clearAllMocks()
  })

  it('initializes with default state', () => {
    expect(store.liste).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getAll fetches content form', async () => {
    mockApi.getAll.mockResolvedValue({ data: { content: [item] } })
    const result = await store.getAll()
    expect(store.liste).toEqual([item])
    expect(result).toEqual([item])
    expect(store.loading).toBe(false)
  })

  it('add pushes to list', async () => {
    mockApi.create.mockResolvedValue({ data: item })
    await store.add(item)
    expect(store.liste).toEqual([item])
  })

  it('update replaces in list', async () => {
    store.liste = [item]
    const guncel = { ...item, ad: 'Güncel' }
    mockApi.update.mockResolvedValue({ data: guncel })
    await store.update(1, guncel)
    expect(store.liste[0].ad).toBe('Güncel')
  })

  it('remove filters from list', async () => {
    store.liste = [item, { id: 2, ad: 'İkinci' }]
    mockApi.delete.mockResolvedValue({})
    await store.remove(1)
    expect(store.liste).toHaveLength(1)
  })

  it('sets error on getAll failure', async () => {
    mockApi.getAll.mockRejectedValue({ response: { data: { message: 'DB error' } } })
    await expect(store.getAll()).rejects.toBeTruthy()
    expect(store.error).toBe('DB error')
  })
})

describe('createCrudStore with custom options', () => {
  it('exposes customized action names', () => {
    const customApi = { getAll: vi.fn(), create: vi.fn(), update: vi.fn(), delete: vi.fn() }
    const useCustom = createCrudStore('custom', customApi, {
      stateKey: 'kayitlar',
      actions: { getAll: 'hepsiniGetir', add: 'ekle', update: 'guncelle', remove: 'sil' }
    })
    setActivePinia(createPinia())
    const s = useCustom()
    expect(s.kayitlar).toEqual([])
    expect(typeof s.hepsiniGetir).toBe('function')
    expect(typeof s.ekle).toBe('function')
    expect(typeof s.guncelle).toBe('function')
    expect(typeof s.sil).toBe('function')
  })
})
