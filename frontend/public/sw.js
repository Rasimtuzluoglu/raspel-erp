/* RasPel ERP Service Worker — offline-first KOBİ modu */
const STATIC_CACHE = 'raspel-erp-static-v2'
const API_CACHE = 'raspel-erp-api-v2'

const API_ROUTES = ['/api/cari-hesaplar', '/api/faturalar', '/api/hareketler',
  '/api/stoklar', '/api/bankalar', '/api/kasalar', '/api/personel',
  '/api/sirketler', '/api/kullanicilar', '/api/dashboard']

function isApiGet(request) {
  return request.method === 'GET' && API_ROUTES.some(r => new URL(request.url).pathname.startsWith(r))
}

self.addEventListener('install', () => self.skipWaiting())

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== STATIC_CACHE && k !== API_CACHE).map(k => caches.delete(k)))
    )
  )
  self.clients.claim()
})

self.addEventListener('fetch', (event) => {
  const { request } = event
  const url = new URL(request.url)

  // WebSocket — hicbir zaman cache'leme
  if (url.pathname.startsWith('/ws')) return

  // API GET — stale-while-revalidate (once cache, sonra agdan guncelle)
  if (isApiGet(request)) {
    event.respondWith(
      caches.open(API_CACHE).then(cache =>
        cache.match(request).then(cached => {
          const network = fetch(request).then(response => {
            if (response.ok) cache.put(request, response.clone())
            return response
          }).catch(() => cached)
          return cached || network
        })
      )
    )
    return
  }

  // Statik dosyalar — cache-first
  if (request.method === 'GET' && (request.mode === 'navigate' || url.origin === location.origin)) {
    event.respondWith(
      caches.open(STATIC_CACHE).then(cache =>
        cache.match(request).then(cached => {
          const network = fetch(request).then(response => {
            if (response.ok) cache.put(request, response.clone())
            return response
          }).catch(() => cached)
          return cached || network
        })
      )
    )
  }
})

// Arka planda API cache guncelleme
self.addEventListener('message', (event) => {
  if (event.data === 'refresh-cache') {
    API_ROUTES.forEach(route => {
      fetch(route).then(res => {
        if (res.ok) caches.open(API_CACHE).then(c => c.put(route, res))
      }).catch(() => {})
    })
  }
})
