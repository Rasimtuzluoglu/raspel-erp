// Erken kurtarma scripti (CSP uyumlu harici dosya).
// Eski bir service worker eski/bozuk asset'leri onbellekten sunarsa veya
// dinamik chunk yuklenemezse beyaz ekran olusabilir. Bu durumda onbellegi
// temizleyip tek seferlik yeniler.
;(function () {
  var KILIT = 'raspel_kurtarma_denendi'

  function cacheTemizle() {
    try {
      if (window.caches && caches.keys) {
        caches.keys().then(function (k) {
          return Promise.all(k.map(function (x) { return caches.delete(x) }))
        })
      }
    } catch (e) { /* yoksay */ }
  }

  function swKaldir() {
    try {
      if (navigator.serviceWorker && navigator.serviceWorker.getRegistrations) {
        navigator.serviceWorker.getRegistrations().then(function (r) {
          r.forEach(function (x) { x.unregister() })
        })
      }
    } catch (e) { /* yoksay */ }
  }

  window.__raspelKurtar = function () {
    if (sessionStorage.getItem(KILIT) === '1') return
    sessionStorage.setItem(KILIT, '1')
    cacheTemizle()
    swKaldir()
    setTimeout(function () { window.location.reload() }, 120)
  }

  // Asset/chunk yukleme hatasi (or. eski index.html yeni chunk'i bulamaz).
  window.addEventListener('error', function (ev) {
    var hedef = ev && (ev.target || ev.srcElement)
    if (hedef && hedef.tagName === 'SCRIPT' && hedef.src) {
      window.__raspelKurtar()
    }
  }, true)
  window.addEventListener('unhandledrejection', function (ev) {
    var msg = (ev && ev.reason && (ev.reason.message || ev.reason)) || ''
    if (typeof msg === 'string' && /dynamically imported module|Loading chunk|ChunkLoadError/i.test(msg)) {
      window.__raspelKurtar()
    }
  })
})()
