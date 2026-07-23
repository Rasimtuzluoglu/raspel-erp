import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../authStore.js'
import { kullaniciAPI } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  kullaniciAPI: {
    giris: vi.fn(),
    getAll: vi.fn(),
    getById: vi.fn()
  }
}))

const mockUser = {
  id: 1, username: 'test', displayName: 'Test User',
  avatarUrl: null, companyName: 'Test Co', role: 'ADMIN',
  token: 'abc123', sirketId: 1, sirketAdi: 'Test Sirket'
}

describe('authStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    store = useAuthStore()
  })

  it('initializes with default state', () => {
    expect(store.kullanici).toBeNull()
    expect(store.token).toBe('')
    expect(store.loading).toBe(false)
    expect(store.isLoggedIn).toBe(false)
  })

  it('restores auth from localStorage', () => {
    const authData = {
      kullanici: { id: 1, username: 'test' },
      token: 'xyz', companyName: 'Co',
      sirketId: 1, sirketAdi: 'Sirket'
    }
    localStorage.setItem('raspel_erp_auth', JSON.stringify(authData))
    store.init()
    expect(store.kullanici).toEqual(authData.kullanici)
    expect(store.token).toBe('xyz')
    expect(store.isLoggedIn).toBe(true)
  })

  it('girisYap sets state on success', async () => {
    kullaniciAPI.giris.mockResolvedValue({ data: mockUser })
    const result = await store.girisYap('test', 'pass', 'Co')
    expect(store.kullanici.username).toBe('test')
    expect(store.token).toBe('abc123')
    expect(store.isLoggedIn).toBe(true)
    expect(store.loading).toBe(false)
    expect(result).toEqual(mockUser)
  })

  it('girisYap handles error', async () => {
    kullaniciAPI.giris.mockRejectedValue(new Error('API Error'))
    await expect(store.girisYap('test', 'wrong', 'Co')).rejects.toThrow('API Error')
    expect(store.loading).toBe(false)
    expect(store.isLoggedIn).toBe(false)
  })

  it('cikisYap clears state', () => {
    localStorage.setItem('raspel_erp_auth', JSON.stringify({ kullanici: { id: 1 }, token: 'x' }))
    store.init()
    store.cikisYap()
    expect(store.kullanici).toBeNull()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('raspel_erp_auth')).toBeNull()
  })

  it('kullanicilariGetir returns users', async () => {
    const users = [{ id: 1, username: 'admin' }]
    kullaniciAPI.getAll.mockResolvedValue({ data: users })
    const result = await store.kullanicilariGetir()
    expect(result).toEqual(users)
  })
})
