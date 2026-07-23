import { vi, describe, it, expect, beforeEach, beforeAll } from 'vitest'

let mockInterceptors

const mockAxiosInstance = {
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
  interceptors: {
    request: { handlers: [], use: vi.fn((f, r) => { mockAxiosInstance.interceptors.request.handlers.push({ fulfilled: f, rejected: r }) }) },
    response: { handlers: [], use: vi.fn((f, r) => { mockAxiosInstance.interceptors.response.handlers.push({ fulfilled: f, rejected: r }) }) }
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
    expect(axios.default.create).toHaveBeenCalledWith({
      baseURL: 'http://localhost:8081/api',
      headers: { 'Content-Type': 'application/json' }
    })
  })

  it('request interceptor adds auth token from localStorage', () => {
    const authData = { token: 'test-token-123', kullanici: { id: 1 } }
    localStorage.setItem('raspel_erp_auth', JSON.stringify(authData))
    const config = { headers: {} }
    const handler = mockAxiosInstance.interceptors.request.handlers[0]
    const result = handler.fulfilled(config)
    expect(result.headers.Authorization).toBe('Bearer test-token-123')
  })

  it('request interceptor does not add token if not present', () => {
    localStorage.setItem('raspel_erp_auth', JSON.stringify({ kullanici: { id: 1 } }))
    const config = { headers: {} }
    const handler = mockAxiosInstance.interceptors.request.handlers[0]
    const result = handler.fulfilled(config)
    expect(result.headers.Authorization).toBeUndefined()
  })

  it('request interceptor handles malformed localStorage', () => {
    localStorage.setItem('raspel_erp_auth', 'not-json')
    const config = { headers: {} }
    const handler = mockAxiosInstance.interceptors.request.handlers[0]
    const result = handler.fulfilled(config)
    expect(result.headers.Authorization).toBeUndefined()
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
