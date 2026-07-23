import { setActivePinia, createPinia } from 'pinia'
import { useDashboardStore } from '../dashboardStore.js'
import { dashboardAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  dashboardAPI: {
    getData: vi.fn()
  }
}))

const mockData = {
  toplamCariSayisi: 10,
  toplamBakiye: 50000,
  sonHareketler: [{ id: 1, tutar: 100 }]
}

describe('dashboardStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    store = useDashboardStore()
  })

  it('initializes with default state', () => {
    expect(store.toplamCariSayisi).toBe(0)
    expect(store.toplamBakiye).toBe(0)
    expect(store.sonHareketler).toEqual([])
    expect(store.loading).toBe(false)
    expect(store.error).toBeNull()
  })

  it('getDashboardData sets all fields', async () => {
    dashboardAPI.getData.mockResolvedValue({ data: mockData })
    const result = await store.getDashboardData()
    expect(store.toplamCariSayisi).toBe(10)
    expect(store.toplamBakiye).toBe(50000)
    expect(store.sonHareketler).toHaveLength(1)
    expect(store.loading).toBe(false)
    expect(result).toEqual(mockData)
  })

  it('getDashboardData handles error', async () => {
    dashboardAPI.getData.mockRejectedValue(new Error('API down'))
    await expect(store.getDashboardData()).rejects.toThrow('API down')
    expect(store.error).toBe('API down')
    expect(store.loading).toBe(false)
  })
})
