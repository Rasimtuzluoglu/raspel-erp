import { clientsClaim } from 'workbox-core'
import { precacheAndRoute, cleanupOutdatedCaches } from 'workbox-precaching'
import { registerRoute, NavigationRoute } from 'workbox-routing'
import { NetworkFirst, CacheFirst, StaleWhileRevalidate } from 'workbox-strategies'
import { ExpirationPlugin } from 'workbox-expiration'
import { CacheableResponsePlugin } from 'workbox-cacheable-response'

self.skipWaiting()
clientsClaim()
cleanupOutdatedCaches()

// Uygulama surumu. Vite `define` ile derleme aninda gercek surume cevrilir;
// cevrilmezse (test/ozel ortam) 'dev'e duser. Boylece SW hicbir zaman
// ReferenceError vermez. Onbellek adlarini surumlemek icin kullanilir:
// yeni surumde ESKI surumun onbellekleri activate'te silinir, boylece
// "hayalet kabuk" (silinmis chunk'lara referans veren eski index.html)
// imkansizlasir.
// eslint-disable-next-line no-undef
const SURUM = (typeof __APP_VERSION__ !== 'undefined' && __APP_VERSION__) ? __APP_VERSION__ : 'dev'

const PAGES_CACHE = 'pages-' + SURUM
const API_READ_CACHE = 'api-read-' + SURUM
const STATIC_CACHE = 'static-assets-' + SURUM

// Uygulamadan gelen kurtarma komutlari:
// - SKIP_WAITING: bekleyen yeni SW'yi hemen devreye alir.
// - CLEAR_CACHES: tum onbellekleri temizleyip taze icerik sunulmasini saglar.
self.addEventListener('message', (event) => {
  const tip = event.data && event.data.type
  if (tip === 'SKIP_WAITING') {
    self.skipWaiting()
  } else if (tip === 'CLEAR_CACHES') {
    event.waitUntil(
      caches.keys().then((anahtarlar) => Promise.all(anahtarlar.map((k) => caches.delete(k))))
    )
  }
})

// SPA gezinme istekleri: NetworkFirst. ONEMLI: bu rota, precache rotasindan
// ONCE kayitli olmalidir; aksi halde Workbox'un precache rotasi (cache-first),
// '/index.html' uzerinden eski (stale) uygulama kabugunu servis edebilir ve
// yeni surum yayinlandiginda tarayici eski arayuzde takilir kalir. Cevrimdisiyken
// precache'teki index.html'e duser.
registerRoute(
  new NavigationRoute(
    new NetworkFirst({
      cacheName: PAGES_CACHE,
      networkTimeoutSeconds: 3,
      plugins: [
        new CacheableResponsePlugin({ statuses: [0, 200] }),
        new ExpirationPlugin({ maxEntries: 10, maxAgeSeconds: 60 * 60 * 24 })
      ]
    }),
    { denylist: [/^\/api\//, /^\/ws\//] }
  )
)

precacheAndRoute(self.__WB_MANIFEST)

// KAPSAM: uygulamaya ait onbellekler SURUME baglidir.
//
// Sorun: onceden onbellek adlari sabitti ('pages', 'static-assets', 'api-read').
// Ayni adlar farkli surumlerde tekrar kullanildigi icin, NetworkFirst gezinme
// rotasi agin yavas oldugu durumda (networkTimeoutSeconds 3) onbellekten ESKI
// index.html'i servis edebiliyordu. Eski index yeni chunk hash'lerine degil,
// deploy'da SILINMIS eski chunk'lara referans veriyordu -> bos ekran.
//
// Cozum: adlara surum eklenir ve activate'te FARKLI surumlu + surumsuz (legacy)
// uygulama onbellekleri silinir. Boylece her surum temiz bir sayfa ile baslar.
//
// Not: Workbox'un precache onbellegi `cleanupOutdatedCaches()` tarafindan
// yonetilir; `google-fonts` dis kaynakli ve kararli oldugu icin surumsuz kalir.
const EMEKLI_ONEK_ONEKI = 'raspel-eski-'
const UYGULAMA_CACHE_TABANLARI = ['pages', 'api-read', 'static-assets']

self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((anahtarlar) => Promise.all(
      anahtarlar
        .filter((k) => {
          if (k.startsWith(EMEKLI_ONEK_ONEKI)) return true // emekli isaretli
          for (const taban of UYGULAMA_CACHE_TABANLARI) {
            if (k === taban) return true // surumsuz legacy ad -> yeni adlara tasi
            if (k.startsWith(taban + '-') && k !== taban + '-' + SURUM) return true // eski surum
          }
          return false
        })
        .map((k) => caches.delete(k))
    )).catch(() => undefined)
  )
})

// Finansal API yanitlari NetworkFirst ile tazelenir; cache yalnizca kisa sureli
// cevrimdisi tampon olarak tutulur (finansal veri guncelligi + gizlilik).
registerRoute(
  ({ url, request }) => request.method === 'GET' && url.pathname.startsWith('/api/'),
  new NetworkFirst({
    cacheName: API_READ_CACHE,
    networkTimeoutSeconds: 4,
    plugins: [
      new CacheableResponsePlugin({ statuses: [0, 200] }),
      new ExpirationPlugin({ maxEntries: 50, maxAgeSeconds: 5 * 60 })
    ]
  })
)

// Mutasyon (POST/PUT/PATCH/DELETE) sonrasi okuma onbellegini gecersiz kil.
// Aksi halde silme/guncelleme sonrasi ayni GET URL'si eski (stale) yaniti
// dondurebiliyor (or. taksit silindikten sonra listede gorunmeye devam eder).
self.addEventListener('fetch', (event) => {
  const { request } = event
  if (!request.url.includes('/api/')) return
  if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(request.method)) return
  event.waitUntil(
    caches
      .open(API_READ_CACHE)
      .then((cache) => cache.keys().then((keys) => Promise.all(keys.map((k) => cache.delete(k)))))
      .catch(() => undefined)
  )
})

registerRoute(
  ({ request }) => ['image', 'font', 'style', 'script'].includes(request.destination),
  new CacheFirst({
    cacheName: STATIC_CACHE,
    plugins: [
      new CacheableResponsePlugin({ statuses: [0, 200] }),
      new ExpirationPlugin({ maxEntries: 100, maxAgeSeconds: 60 * 60 * 24 * 365 })
    ]
  })
)

registerRoute(
  ({ url }) => /^https:\/\/fonts\.(googleapis|gstatic)\.com\//.test(url.href),
  new StaleWhileRevalidate({
    cacheName: 'google-fonts',
    plugins: [
      new CacheableResponsePlugin({ statuses: [0, 200] }),
      new ExpirationPlugin({ maxEntries: 20, maxAgeSeconds: 60 * 60 * 24 * 365 })
    ]
  })
)

// Web Push: sunucudan gelen bildirimi gosterir.
self.addEventListener('push', (event) => {
  let data = {}
  if (event.data) {
    try {
      data = event.data.json()
    } catch {
      data = { title: 'RasPel ERP', body: event.data.text() }
    }
  }
  const title = data.title || 'RasPel ERP'
  const options = {
    body: data.body || '',
    icon: '/icon-192.png',
    badge: '/icon-192.png',
    data: { url: data.url || '/' }
  }
  event.waitUntil(self.registration.showNotification(title, options))
})

// Bildirime tiklaninca uygulamayi acar/odaklar.
self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  const hedef = (event.notification.data && event.notification.data.url) || '/'
  event.waitUntil(
    self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then((clientList) => {
      for (const client of clientList) {
        if ('focus' in client) {
          client.focus()
          if (client.navigate && client.url !== hedef) client.navigate(hedef)
          return undefined
        }
      }
      if (self.clients.openWindow) return self.clients.openWindow(hedef)
      return undefined
    })
  )
})
