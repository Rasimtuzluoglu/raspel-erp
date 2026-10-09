import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

/**
 * Service worker'in aktivasyon davranisi.
 *
 * Kritik kural: gecerli surumun onbellekleri SILINMEZ. `static-assets`
 * (100 girdi, 1 yil TTL, CacheFirst), `pages`, `api-read` ve `google-fonts`
 * her release'de yeniden kurulmak zorundaydi; kullanici her surumden sonra
 * tum asset'leri yeniden indiriyordu.
 */

const yukle = async (onbellekler) => {
  const silinen = []
  const listeners = {}

  vi.stubGlobal('self', {
    skipWaiting: vi.fn(),
    clientsClaim: vi.fn(),
    registration: { showNotification: vi.fn() },
    addEventListener: (tip, cb) => { listeners[tip] = cb },
    clients: { matchAll: vi.fn(() => Promise.resolve([])), openWindow: vi.fn() }
  })
  vi.stubGlobal('caches', {
    keys: () => Promise.resolve(onbellekler),
    delete: (k) => { silinen.push(k); return Promise.resolve(true) },
    open: () => Promise.resolve({ keys: () => Promise.resolve([]), delete: () => Promise.resolve(true) })
  })

  vi.doMock('workbox-core', () => ({ clientsClaim: vi.fn() }))
  vi.doMock('workbox-precaching', () => ({ precacheAndRoute: vi.fn(), cleanupOutdatedCaches: vi.fn() }))
  vi.doMock('workbox-routing', () => ({
    registerRoute: vi.fn(),
    NavigationRoute: class { constructor(c, o) { this.c = c; this.o = o } }
  }))
  vi.doMock('workbox-strategies', () => ({
    NetworkFirst: class {}, CacheFirst: class {}, StaleWhileRevalidate: class {}
  }))
  vi.doMock('workbox-expiration', () => ({ ExpirationPlugin: class {} }))
  vi.doMock('workbox-cacheable-response', () => ({ CacheableResponsePlugin: class {} }))

  vi.resetModules()
  await import('../sw.js')

  const bekleyenler = []
  if (listeners.activate) {
    listeners.activate({ waitUntil: (p) => bekleyenler.push(p) })
    await Promise.all(bekleyenler)
  }
  return { silinen, listeners }
}

describe('sw.js aktivasyonu', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
    vi.doUnmock('workbox-core')
    vi.doUnmock('workbox-precaching')
    vi.doUnmock('workbox-routing')
    vi.doUnmock('workbox-strategies')
    vi.doUnmount?.()
  })

  it('gecerli surumun onbelleklerini KORUR', async () => {
    const { silinen } = await yukle([
      'static-assets',
      'pages',
      'api-read',
      'google-fonts',
      'workbox-precache-v2-http://localhost:5173'
    ])

    expect(silinen).toEqual([])
  })

  it('yalnizca emekli surum onbellegini siler', async () => {
    const { silinen } = await yukle([
      'static-assets',
      'pages',
      'raspel-eski-v1',
      'raspel-eski-v0'
    ])

    expect(silinen).toEqual(['raspel-eski-v1', 'raspel-eski-v0'])
  })
})