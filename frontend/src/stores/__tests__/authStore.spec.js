import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../authStore.js'
import { kullaniciAPI, apiClient } from '../../api/index.js'
import { vi, describe, it, expect, beforeEach } from 'vitest'

vi.mock('../../api/index.js', () => ({
  apiClient: { get: vi.fn(), post: vi.fn(), put: vi.fn() },
  kullaniciAPI: {
    giris: vi.fn(),
    girisSirket: vi.fn(),
    giris2fa: vi.fn(),
    getAll: vi.fn(),
    getById: vi.fn(),
    cikis: vi.fn()
  }
}))

const mockUser = {
  id: 1,
  username: 'test',
  displayName: 'Test User',
  avatarUrl: null,
  companyName: 'Test Co',
  role: 'ADMIN',
  token: 'abc123',
  sirketId: 1,
  sirketAdi: 'Test Sirket'
}

describe('authStore', () => {
  let store

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    sessionStorage.clear()
    apiClient.get.mockReset()
    store = useAuthStore()
  })

  it('initializes with default state', () => {
    expect(store.kullanici).toBeNull()
    expect(store.token).toBe('')
    expect(store.loading).toBe(false)
    expect(store.isLoggedIn).toBe(false)
  })

  it('restores auth from localStorage (token haric — httpOnly cookie)', () => {
    const authData = {
      kullanici: { id: 1, username: 'test' },
      token: 'xyz',
      companyName: 'Co',
      sirketId: 1,
      sirketAdi: 'Sirket',
      tokenExpiresAt: 9999999999999
    }
    localStorage.setItem('raspel_erp_auth', JSON.stringify(authData))
    store.init()
    expect(store.kullanici).toEqual(authData.kullanici)
    expect(store.token).toBe('') // token localStorage'dan geri yüklenmez (XSS koruması)
    expect(store.tokenExpiresAt).toBe(9999999999999)
    expect(store.isLoggedIn).toBe(true)
  })

  it('oturumKur tokeni depoya yazmaz (beni hatirla yoksa sessionStorage)', async () => {
    kullaniciAPI.girisSirket.mockResolvedValue({ data: { ...mockUser, tokenExpiresAt: 1234567890123 } })
    await store.girisSirket('pending-token', 1)
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('abc123')
    const kayitli = JSON.parse(sessionStorage.getItem('raspel_erp_auth'))
    expect(kayitli.token).toBeUndefined()
    expect(kayitli.tokenExpiresAt).toBe(1234567890123)
    expect(localStorage.getItem('raspel_erp_auth')).toBeNull()
  })

  it('girisYap returns user without setting session (sirket secimi beklenir)', async () => {
    kullaniciAPI.giris.mockResolvedValue({ data: mockUser })
    const result = await store.girisYap('test', 'pass')
    expect(store.kullanici).toBeNull()
    expect(store.token).toBe('')
    expect(store.isLoggedIn).toBe(false)
    expect(store.loading).toBe(false)
    expect(result).toEqual(mockUser)
  })

  it('girisYap handles error', async () => {
    kullaniciAPI.giris.mockRejectedValue(new Error('API Error'))
    await expect(store.girisYap('test', 'wrong')).rejects.toThrow('API Error')
    expect(store.loading).toBe(false)
    expect(store.isLoggedIn).toBe(false)
  })

  it('girisYap returns 2FA flow without setting session', async () => {
    kullaniciAPI.giris.mockResolvedValue({ data: { twoFactorGerekli: true, girisToken: 'pending-token' } })
    const result = await store.girisYap('test', 'pass')
    expect(result.twoFactorGerekli).toBe(true)
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe('')
  })

  it('girisSirket completes login and sets session', async () => {
    kullaniciAPI.girisSirket.mockResolvedValue({ data: mockUser })
    await store.girisSirket('pending-token', 1)
    expect(store.isLoggedIn).toBe(true)
    expect(store.token).toBe('abc123')
    expect(store.kullanici.username).toBe('test')
  })

  it('giris2fa returns company list without setting session', async () => {
    kullaniciAPI.giris2fa.mockResolvedValue({ data: mockUser })
    const result = await store.giris2fa('pending-token', '123456')
    expect(store.isLoggedIn).toBe(false)
    expect(store.token).toBe('')
    expect(result).toEqual(mockUser)
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

  describe('hasPermission', () => {
    it('ADMIN her zaman geçer', () => {
      store.kullanici = { role: 'ADMIN' }
      expect(store.hasPermission('STOK_DELETE')).toBe(true)
    })

    it('giriş yapılmamışken false döner', () => {
      expect(store.hasPermission('STOK_READ')).toBe(false)
    })

    it('yetkiler yüklenene kadar hiçbir şeyi gizlemez', () => {
      store.kullanici = { role: 'USER' }
      store.yetkiYuklendi = false
      store.yetkiler = []
      expect(store.hasPermission('STOK_DELETE')).toBe(true)
    })

    it('yüklenmiş yetkiler arasında varsa true', () => {
      store.kullanici = { role: 'USER' }
      store.yetkiler = ['STOK_READ', 'STOK_WRITE']
      store.yetkiYuklendi = true
      expect(store.hasPermission('STOK_WRITE')).toBe(true)
      expect(store.hasPermission('STOK_DELETE')).toBe(false)
    })
  })

  describe('yetkileriYukle', () => {
    it('ADMIN için joker yetki atar', async () => {
      store.kullanici = { role: 'ADMIN' }
      await store.yetkileriYukle()
      expect(store.yetkiler).toEqual(['*'])
      expect(store.yetkiYuklendi).toBe(true)
    })

    it('rolü ad alanından eşleştirip kodları alır', async () => {
      store.kullanici = { role: 'USER' }
      apiClient.get.mockResolvedValue({
        data: [{ ad: 'USER', yetkiler: [{ kod: 'STOK_READ' }, { kod: 'STOK_WRITE' }] }]
      })
      await store.yetkileriYukle()
      expect(store.yetkiler).toEqual(['STOK_READ', 'STOK_WRITE'])
      expect(store.yetkiYuklendi).toBe(true)
    })

    it('yetkileri string olarak da kabul eder', async () => {
      store.kullanici = { role: 'USER' }
      apiClient.get.mockResolvedValue({ data: [{ ad: 'USER', yetkiler: ['CARI_READ'] }] })
      await store.yetkileriYukle()
      expect(store.yetkiler).toEqual(['CARI_READ'])
    })

    it('rol tanımı yoksa yetkisiz sayar ama bayrağı yüklü yapar', async () => {
      store.kullanici = { role: 'DRIVER' }
      apiClient.get.mockResolvedValue({ data: [{ ad: 'USER', yetkiler: [] }] })
      await store.yetkileriYukle()
      expect(store.yetkiler).toEqual([])
      expect(store.yetkiYuklendi).toBe(true)
      expect(store.hasPermission('SIPARIS_READ')).toBe(false)
    })

    it('istek hatasında gizleme yapılmaz', async () => {
      store.kullanici = { role: 'USER' }
      apiClient.get.mockRejectedValue(new Error('network'))
      await store.yetkileriYukle()
      expect(store.yetkiYuklendi).toBe(true)
      expect(store.hasPermission('SIPARIS_READ')).toBe(false)
    })
  })
})
