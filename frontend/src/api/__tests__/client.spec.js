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

  /**
   * Rapor/Excel/PDF üretimi sunucuda senkron ve veri hacmine göre yavaştır.
   * 30 sn'lik global timeout istemcide iptal ediyor, sunucu ise çalışmaya
   * devam ediyordu. Bu uçlara daha uzun süre tanınır.
   */
  it('istek: export/rapor uclarina uzun timeout uygulanir', () => {
    const handler = requestHandlers[0].fulfilled
    expect(handler({ url: '/exports/faturalar', timeout: 30000 }).timeout).toBe(180000)
    expect(handler({ url: '/rapor/fatura/5', timeout: 30000 }).timeout).toBe(180000)
    expect(handler({ url: '/backups/download/x.sql.gz', timeout: 30000 }).timeout).toBe(180000)
    expect(handler({ url: '/belge/indir', timeout: 30000 }).timeout).toBe(180000)
  })

  it('istek: normal uclar varsayilan 30 saniyede kalir', () => {
    const handler = requestHandlers[0].fulfilled
    const cfg = handler({ url: '/stoklar', timeout: 30000 })
    expect(cfg.timeout).toBe(30000)
  })

  it('istek: cagiran kendi timeout degerini verirse dokunulmaz', () => {
    const handler = requestHandlers[0].fulfilled
    expect(handler({ url: '/exports/faturalar', timeout: 5000 }).timeout).toBe(5000)
    expect(handler({ url: '/exports/faturalar', timeout: 0 }).timeout).toBe(0)
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

  it('yanit: sunucuya ulasilamayan ag hatasinda hem banner hem api-error', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const error = { code: 'ERR_NETWORK', config: { url: '/stoklar' }, response: undefined }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(client.networkStatus.sunucuyaUlasilamiyor).toBe(true)
    expect(client.networkStatus.showBanner).toBe(true)
    expect(dinleyici).toHaveBeenCalledTimes(1)
    expect(dinleyici.mock.calls[0][0].detail).toEqual({ status: 0, anahtar: 'common.baglantiHatasi' })
    window.removeEventListener('api-error', dinleyici)
  })

  it('yanit: zaman asimi banner gostermez, sadece bilgilendirir', async () => {
    const error = { code: 'ECONNABORTED', config: { url: '/rapor/xyz' }, response: undefined }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(client.networkStatus.sunucuyaUlasilamiyor).toBe(false)
    expect(client.networkStatus.showBanner).toBe(false)
  })

  it('yanit: basarisiz yanit sonrasi sunucu erisilebilirlik bayragi temizlenir', () => {
    client.networkStatus.sunucuyaUlasilamiyor = true
    client.networkStatus.showBanner = true
    const before = responseHandlers[0].fulfilled
    before({ data: 'ok' })
    expect(client.networkStatus.sunucuyaUlasilamiyor).toBe(false)
    expect(client.networkStatus.showBanner).toBe(false)
  })

  it('yanit: 500 bos govdeli hata yerellestirilmis anahtarla bildirilir', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const error = { response: { status: 500, data: '' }, config: { url: '/faturalar' } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(dinleyici).toHaveBeenCalledTimes(1)
    expect(dinleyici.mock.calls[0][0].detail).toEqual({ status: 500, anahtar: 'common.sunucuHatasi' })
    window.removeEventListener('api-error', dinleyici)
  })

  it('yanit: 502 HTML govdeli gateway hatasinda bildirim yine cikar', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const error = { response: { status: 502, data: '<html>502 Bad Gateway</html>' }, config: { url: '/' } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(dinleyici).toHaveBeenCalledTimes(1)
    expect(dinleyici.mock.calls[0][0].detail.anahtar).toBe('common.sunucuHatasi')
    window.removeEventListener('api-error', dinleyici)
  })

  it('yanit: 404 sunucu mesaji yoksa global bildirim uretmez (yerel catch yeter)', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const error = { response: { status: 404, data: {} }, config: { url: '/stok/9' } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(dinleyici).not.toHaveBeenCalled()
    window.removeEventListener('api-error', dinleyici)
  })

  it('yanit: sunucu mesaji varsa o onceliklidir', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const error = { response: { status: 503, data: { message: 'Yetersiz stok!' } }, config: { url: '/faturalar' } }

    await expect(responseHandlers[0].rejected(error)).rejects.toEqual(error)

    expect(dinleyici.mock.calls[0][0].detail).toEqual({ status: 503, message: 'Yetersiz stok!' })
    window.removeEventListener('api-error', dinleyici)
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

  it('yanit: ayni URL icin tekrar eden hatalarda tek api-error yayilir', async () => {
    const dinleyici = vi.fn()
    window.addEventListener('api-error', dinleyici)
    const hata = () => ({ config: { url: '/stoklar' }, response: { status: 500, data: { message: 'Sunucu hatasi' } } })

    await expect(responseHandlers[0].rejected(hata())).rejects.toBeDefined()
    await expect(responseHandlers[0].rejected(hata())).rejects.toBeDefined()

    expect(dinleyici).toHaveBeenCalledTimes(1)
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
