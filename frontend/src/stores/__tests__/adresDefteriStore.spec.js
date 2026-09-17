import { setActivePinia, createPinia } from 'pinia'
import { useAdresDefteriStore } from '../adresDefteriStore.js'
import { adresDefteriAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  adresDefteriAPI: {
    getAll: vi.fn(),
    create: vi.fn(),
    update: vi.fn(),
    delete: vi.fn()
  }
}))

const mockKisi = { id: 1, ad: 'Elektrikçi Ali', tur: 'Elektrikçi', telefon: '05321112233' }

describe('adresDefteriStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useAdresDefteriStore()
  })

  it('initializes with default state', () => {
    expect(store.kisiler).toEqual([])
    expect(store.loading).toBe(false)
  })

  it('getAllKisiler fetches data', async () => {
    adresDefteriAPI.getAll.mockResolvedValue({ data: [mockKisi] })
    const result = await store.getAllKisiler()
    expect(store.kisiler).toHaveLength(1)
    expect(result).toEqual([mockKisi])
  })

  it('addKisi pushes to list', async () => {
    adresDefteriAPI.create.mockResolvedValue({ data: mockKisi })
    await store.addKisi(mockKisi)
    expect(store.kisiler).toContainEqual(mockKisi)
  })

  it('updateKisi replaces item', async () => {
    store.kisiler = [{ ...mockKisi }]
    adresDefteriAPI.update.mockResolvedValue({ data: { ...mockKisi, ad: 'Ali Usta' } })
    await store.updateKisi(1, { ad: 'Ali Usta' })
    expect(store.kisiler[0].ad).toBe('Ali Usta')
  })

  it('deleteKisi removes from list', async () => {
    store.kisiler = [mockKisi, { id: 2, ad: 'Tesisatçı Veli' }]
    adresDefteriAPI.delete.mockResolvedValue({})
    await store.deleteKisi(1)
    expect(store.kisiler).toHaveLength(1)
  })

  it('handles error in getAllKisiler', async () => {
    adresDefteriAPI.getAll.mockRejectedValue(new Error('Fail'))
    await expect(store.getAllKisiler()).rejects.toThrow('Fail')
    expect(store.loading).toBe(false)
  })
})
