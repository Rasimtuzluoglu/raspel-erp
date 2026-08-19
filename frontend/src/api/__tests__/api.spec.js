import { vi, describe, it, expect, beforeEach, beforeAll } from 'vitest'

const mockAxiosInstance = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  interceptors: {
    request: {
      handlers: [],
      use: vi.fn((f, r) => {
        mockAxiosInstance.interceptors.request.handlers.push({ fulfilled: f, rejected: r })
      })
    },
    response: {
      handlers: [],
      use: vi.fn((f, r) => {
        mockAxiosInstance.interceptors.response.handlers.push({ fulfilled: f, rejected: r })
      })
    }
  }
}

vi.mock('axios', () => ({
  default: {
    create: vi.fn(() => mockAxiosInstance)
  },
  create: vi.fn(() => mockAxiosInstance)
}))

describe('API client', () => {
  beforeAll(async () => {
    await import('../index.js')
  })

  beforeEach(() => {
    localStorage.clear()
  })

  it('creates axios instance with correct base URL', async () => {
    const axios = await import('axios')
    expect(axios.default.create).toHaveBeenCalledWith(
      expect.objectContaining({
        baseURL: '/api',
        headers: { 'Content-Type': 'application/json' }
      })
    )
  })

  it('response interceptor passes through successful response', () => {
    const handler = mockAxiosInstance.interceptors.response.handlers[0]
    const response = { data: 'ok' }
    expect(handler.fulfilled(response)).toEqual(response)
  })

  it('response interceptor rejects on 401', async () => {
    const handler = mockAxiosInstance.interceptors.response.handlers[0]
    const error = { response: { status: 401 } }
    await expect(handler.rejected(error)).rejects.toEqual(error)
  })

  it('response interceptor rejects on 403', async () => {
    const handler = mockAxiosInstance.interceptors.response.handlers[0]
    const error = { response: { status: 403 } }
    await expect(handler.rejected(error)).rejects.toEqual(error)
  })

  it('exports all API modules', async () => {
    const api = await import('../index.js')
    expect(api.cariHesapAPI).toBeDefined()
    expect(api.hareketAPI).toBeDefined()
    expect(api.dashboardAPI).toBeDefined()
    expect(api.faturaAPI).toBeDefined()
    expect(api.bankaAPI).toBeDefined()
    expect(api.kasaAPI).toBeDefined()
    expect(api.kategoriAPI).toBeDefined()
    expect(api.stokAPI).toBeDefined()
    expect(api.kullaniciAPI).toBeDefined()
    expect(api.raporAPI).toBeDefined()
    expect(api.sirketAPI).toBeDefined()
    expect(api.donemAPI).toBeDefined()
  })
})
