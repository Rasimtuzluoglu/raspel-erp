/* RasPel ERP Service Worker — basit cache-first stratejisi */
const CACHE = 'raspel-erp-v1'

self.addEventListener('install', (event) => {
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))
    )
  )
  self.clients.claim()
})

self.addEventListener('fetch', (event) => {
  const { request } = event
  if (request.method !== 'GET') return
  const url = new URL(request.url)
  // API ve WebSocket isteklerini asla cache'leme
  if (url.pathname.startsWith('/api') || url.pathname.startsWith('/ws')) return

  event.respondWith(
    caches.match(request).then((cached) => {
      const network = fetch(request)
        .then((response) => {
          if (response && response.status === 200 && (request.mode === 'navigate' || url.origin === location.origin)) {
            const clone = response.clone()
            caches.open(CACHE).then((cache) => cache.put(request, clone))
          }
          return response
        })
        .catch(() => cached)
      return cached || network
    })
  )
})
