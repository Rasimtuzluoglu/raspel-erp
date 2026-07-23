import { setActivePinia, createPinia } from 'pinia'
import { useHareketStore } from '../hareketStore.js'
import { hareketAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  hareketAPI: {
    getByCariHesap: vi.fn(),
    getSon: vi.fn(),
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

const mockHareket = { id: 1, aciklama: 'Test', tutar: 200 }

describe('hareketStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useHareketStore()
  })

  it('initializes with default state', () => {
    expect(store.hareketler).toEqual([])
    expect(store.sonHareketler).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getHareketlerByCariHesap fetches', async () => {
    hareketAPI.getByCariHesap.mockResolvedValue({ data: [mockHareket] })
    const result = await store.getHareketlerByCariHesap(1)
    expect(store.hareketler).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockHareket])
  })

  it('getSonHareketler fetches recent', async () => {
    hareketAPI.getSon.mockResolvedValue({ data: [mockHareket] })
    const result = await store.getSonHareketler(5)
    expect(store.sonHareketler).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockHareket])
  })

  it('getAllHareketler fetches all', async () => {
    hareketAPI.getAll.mockResolvedValue({ data: [mockHareket] })
    const result = await store.getAllHareketler()
    expect(store.hareketler).toHaveLength(1)
    expect(result).toEqual([mockHareket])
  })

  it('addHareket pushes and refreshes recent', async () => {
    hareketAPI.create.mockResolvedValue({ data: mockHareket })
    hareketAPI.getSon.mockResolvedValue({ data: [mockHareket] })
    await store.addHareket(mockHareket)
    expect(store.hareketler).toContainEqual(mockHareket)
    expect(hareketAPI.getSon).toHaveBeenCalledWith(5)
  })

  it('updateHareket updates and refreshes recent', async () => {
    store.hareketler = [{ id: 1, aciklama: 'Eski' }]
    const updated = { id: 1, aciklama: 'Yeni' }
    hareketAPI.update.mockResolvedValue({ data: updated })
    hareketAPI.getSon.mockResolvedValue({ data: [updated] })
    await store.updateHareket(1, updated)
    expect(store.hareketler[0].aciklama).toBe('Yeni')
  })

  it('deleteHareket removes from both lists', async () => {
    store.hareketler = [mockHareket, { id: 2 }]
    store.sonHareketler = [mockHareket, { id: 3 }]
    hareketAPI.delete.mockResolvedValue({})
    await store.deleteHareket(1)
    expect(store.hareketler).toHaveLength(1)
    expect(store.sonHareketler).toHaveLength(1)
  })

  it('handles error in getHareketlerByCariHesap', async () => {
    hareketAPI.getByCariHesap.mockRejectedValue(new Error('Not found'))
    await expect(store.getHareketlerByCariHesap(999)).rejects.toThrow('Not found')
    expect(store.error).toBe('Not found')
    expect(store.loading).toBe(false)
  })
})
