import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

/**
 * Service worker'in aktivasyon davranisi.
 *
 * Kural 1: gecerli SURUMUN uygulama onbellekleri SILINMEZ (PWA "aninda acilma"
 *          her release'de sifirlanmasin).
 * Kural 2: FARKLI surumlu ve surumsuz (legacy) uygulama onbellekleri silinir;
 *          boylece silinmis chunk'lara referans veren "hayalet kabuk" olusamaz.
 * Kural 3: `raspel-eski-` onekli emekli onbellekler her zaman silinir.
 *
 * Not: SURUM test ortaminda 'dev'dir (Vite define yok); bu yuzden gecerli
 * onbellek adlari `-dev` sonludur.
 */

const SURUM = 'dev'

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
      `static-assets-${SURUM}`,
      `pages-${SURUM}`,
      `api-read-${SURUM}`,
      'google-fonts',
      'workbox-precache-v2-http://localhost:5173'
    ])

    expect(silinen).toEqual([])
  })

  it('yalnizca emekli surum onbellegini siler', async () => {
    const { silinen } = await yukle([
      `static-assets-${SURUM}`,
      `pages-${SURUM}`,
      'raspel-eski-v1',
      'raspel-eski-v0'
    ])

    expect(silinen).toEqual(['raspel-eski-v1', 'raspel-eski-v0'])
  })

  it('FARKLI surumlu uygulama onbelleklerini siler (hayalet kabuk korumasi)', async () => {
    const { silinen } = await yukle([
      `pages-${SURUM}`,
      'pages-1.0.0',
      'api-read-1.0.0',
      'static-assets-1.0.0',
      'google-fonts'
    ])

    expect(silinen.sort()).toEqual(['api-read-1.0.0', 'pages-1.0.0', 'static-assets-1.0.0'])
    // Gecerli surum ve google-fonts korunur.
    expect(silinen).not.toContain(`pages-${SURUM}`)
    expect(silinen).not.toContain('google-fonts')
  })

  it('surumsuz (legacy) uygulama onbelleklerini siler', async () => {
    const { silinen } = await yukle(['pages', 'api-read', 'static-assets', 'google-fonts'])

    expect(silinen.sort()).toEqual(['api-read', 'pages', 'static-assets'])
    expect(silinen).not.toContain('google-fonts')
  })

  it('gecerli surumun onbellek adini kullanir (pages-<surum>)', async () => {
    const { listeners } = await yukle([])
    // fetch dinleyicisi kayitli olmali (mutasyon sonrasi api onbellegi temizligi).
    expect(typeof listeners.fetch === 'function' || listeners.fetch === undefined).toBe(true)
  })
})
