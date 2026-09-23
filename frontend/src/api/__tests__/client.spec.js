import { vi, describe, it, expect, beforeEach } from 'vitest'

const { requestHandlers, responseHandlers } = vi.hoisted(() => ({
  requestHandlers: [],
  responseHandlers: []
}))

vi.mock('axios', () => {
  const instance = {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    interceptors: {
      request: { use: (f, r) => requestHandlers.push({ fulfilled: f, rejected: r }) },
      response: { use: (f, r) => responseHandlers.push({ fulfilled: f, rejected: r }) }
    }
  }
  return { default: { create: () => instance }, create: () => instance }
})

vi.mock('axios-retry', () => ({ default: vi.fn() }))
vi.mock('nprogress', () => ({ default: { start: vi.fn(), done: vi.fn() } }))

const cikisYap = vi.fn()
vi.mock('../../stores/authStore.js', () => ({
  useAuthStore: () => ({ token: 'tok123', cikisYap })
}))

const push = vi.fn()
vi.mock('../../router/index.js', () => ({
  default: { push, currentRoute: { value: { fullPath: '/faturalar' } } }
}))

const flush = () => new Promise((r) => setTimeout(r, 0))

describe('api/client.js interceptors', () => {
  let client

  beforeEach(async () => {
    requestHandlers.length = 0
    responseHandlers.length = 0
    cikisYap.mockClear()
    push.mockClear()
    localStorage.clear()
    sessionStorage.clear()
    window.history.replaceState({}, '', '/faturalar')
    vi.resetModules()
    client = await import('../client.js')
  })

  it('istek: Accept-Language aktif dile gore eklenir', () => {
    const handler = requestHandlers[0].fulfilled
    const cfgTr = handler({ headers: {} })
    expect(cfgTr.headers['Accept-Language']).toBe('tr')

    localStorage.setItem('lang', 'en')
    const cfgEn = handler({ headers: {} })
    expect(cfgEn.headers['Accept-Language']).toBe('en')
  })

  it('istek: bellek icindeki token Authorization olarak eklenir', () => {
    const handler = requestHandlers[1].fulfilled
    const cfg = handler({ headers: {} })
    expect(cfg.headers.Authorization).toBe('Bearer tok123')
  })

  it('yanit: basarili yanit gecer', () => {
    const response = { data: 'ok' }
    expect(responseHandlers[0].fulfilled(response)).toEqual(response)
  })

  it('yanit: ag hatasinda banner gosterilir', async () => {
    const error = { response: undefined }
    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)
    expect(client.networkStatus.showBanner).toBe(true)
  })

  it('yanit: 400 mesajli hatada api-error event tetiklenir', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const error = { response: { status: 400, data: { message: 'Gecersiz' } } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(dinleyici).toHaveBeenCalledTimes(1)
    expect(dinleyici.mock.calls[0][0].detail).toEqual({ status: 400, message: 'Gecersiz' })
    window.removeEventListener('api-error', dinleyici)
  })

  it('yanit: 401 oturumu kapatir ve giris sayfasina yonlendirir', async () => {
    const error = { response: { status: 401, data: {} } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)
    await flush()

    expect(cikisYap).toHaveBeenCalledTimes(1)
    expect(push).toHaveBeenCalledWith(expect.objectContaining({ name: 'Giris' }))
  })

  it('yanit: 401 /giris sayfasinda yonlendirme yapmaz', async () => {
    window.history.replaceState({}, '', '/giris')
    const error = { response: { status: 401, data: {} } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)
    await flush()

    expect(cikisYap).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('yanit: 403 token suresi dolmussa oturumu kapatir', async () => {
    localStorage.setItem('raspel_erp_auth', JSON.stringify({ tokenExpiresAt: Date.now() - 1000 }))
    const error = { response: { status: 403, data: {} } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)
    await flush()

    expect(cikisYap).toHaveBeenCalledTimes(1)
  })

  it('yanit: 403 token gecerliyse (rol kaynakli) oturumu kapatmaz', async () => {
    localStorage.setItem('raspel_erp_auth', JSON.stringify({ tokenExpiresAt: Date.now() + 100000 }))
    const error = { response: { status: 403, data: {} } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)
    await flush()

    expect(cikisYap).not.toHaveBeenCalled()
    expect(push).not.toHaveBeenCalled()
  })

  it('yanit: blob hata govdesi JSON olarak cozulur', async () => {
    const blob = new Blob(['placeholder'], { type: 'application/json' })
    // jsdom Blob'unda text() olmayabilir; gercek axios blob'u gibi davrandir.
    blob.text = vi.fn().mockResolvedValue(JSON.stringify({ message: 'BlobHata' }))
    const error = { response: { status: 400, data: blob } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(error.response.data).toEqual({ message: 'BlobHata' })
  })
})
