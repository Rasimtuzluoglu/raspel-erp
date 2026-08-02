import { setActivePinia, createPinia } from 'pinia'
import { useNotStore } from '../notStore.js'
import { notAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  notAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

const mockNot = { id: 1, baslik: 'Test Not', icerik: 'İçerik', onemDerecesi: 'NORMAL' }

describe('notStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useNotStore()
  })

  it('initializes with default state', () => {
    expect(store.notlar).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('getAllNotlar fetches data (content form)', async () => {
    notAPI.getAll.mockResolvedValue({ data: { content: [mockNot] } })
    const result = await store.getAllNotlar()
    expect(store.notlar).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual([mockNot])
  })

  it('getAllNotlar fetches data (array form)', async () => {
    notAPI.getAll.mockResolvedValue({ data: [mockNot] })
    await store.getAllNotlar()
    expect(store.notlar).toHaveLength(1)
  })

  it('addNot unshifts to list', async () => {
    notAPI.create.mockResolvedValue({ data: mockNot })
    const result = await store.addNot({ baslik: 'Yeni' })
    expect(store.notlar[0]).toEqual(mockNot)
    expect(result).toEqual(mockNot)
  })

  it('updateNot replaces in list', async () => {
    store.notlar = [mockNot]
    const guncel = { ...mockNot, baslik: 'Güncel' }
    notAPI.update.mockResolvedValue({ data: guncel })
    await store.updateNot(1, guncel)
    expect(store.notlar[0].baslik).toBe('Güncel')
  })

  it('deleteNot removes from list', async () => {
    store.notlar = [mockNot, { id: 2, baslik: 'İkinci' }]
    notAPI.delete.mockResolvedValue({})
    await store.deleteNot(1)
    expect(store.notlar).toHaveLength(1)
  })

  it('handles error in getAllNotlar', async () => {
    notAPI.getAll.mockRejectedValue(new Error('Fail'))
    await expect(store.getAllNotlar()).rejects.toThrow('Fail')
    expect(store.loading).toBe(false)
  })
})
