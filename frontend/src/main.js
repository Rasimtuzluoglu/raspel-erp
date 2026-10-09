import { createApp, defineAsyncComponent } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Lara from '@primevue/themes/lara'
import { definePreset } from '@primevue/themes'

// Faz 4.4: kurumsal tasarim dili. Marka TEAL paleti Lara preset'ine islenir;
// `app.css` icindeki `--accent` (#14b8a6) ve `tailwind.config.cjs` `brand`
// token'lariyla AYNI degerler. (Once indigo idi; app.css teal kullandigi icin
// ekranda IKI farkli vurgu rengi olusuyordu.)
const RaspelPreset = definePreset(Lara, {
  semantic: {
    primary: {
      50: '#f0fdfa',
      100: '#ccfbf1',
      200: '#99f6e4',
      300: '#5eead4',
      400: '#2dd4bf',
      500: '#14b8a6',
      600: '#0d9488',
      700: '#0f766e',
      800: '#115e59',
      900: '#134e4a',
      950: '#042f2e'
    }
  }
})
import App from './App.vue'
import router from './router/index.js'
import i18n from './i18n.js'

import ToastService from 'primevue/toastservice'
import ConfirmationService from 'primevue/confirmationservice'

import PageHeader from './components/PageHeader.vue'
import EmptyState from './components/EmptyState.vue'
import SkeletonLoader from './components/SkeletonLoader.vue'

// Agir global bilesenler tembel yuklenir; boylece entry chunk'i (ilk yuk) kuculur.
// Ilk kullanimda indirilir (oturum acildiktan sonra), login ekranini etkilemez.
const AppDataTable = defineAsyncComponent(() => import('./components/AppDataTable.vue'))
const GecmisZamanCizelgesi = defineAsyncComponent(() => import('./components/GecmisZamanCizelgesi.vue'))
const FaturaGecmisDialog = defineAsyncComponent(() => import('./components/FaturaGecmisDialog.vue'))
const SatirEylemleri = defineAsyncComponent(() => import('./components/SatirEylemleri.vue'))

import permissionDirective from './directives/permission.js'
import tabloEtiketDirective, { initTabloEtiketleri } from './directives/tabloEtiket.js'

import 'primeicons/primeicons.css'
import 'primeflex/primeflex.css'
import './assets/tailwind.css'
import './assets/app.css'
import './assets/pos-panels.css'
import { useTheme } from './composables/useTheme.js'
import { formatCurrency, formatDate, formatDateTime } from './utils/format.js'
import { pvTr } from './utils/primevueLocales.js'

const { initTheme } = useTheme()
initTheme()

// Surum uyusmazligi kontrolu: yeni surum yayinlandiysa eski onbellegi temizle
// (tek seferlik) ve taze icerik alinmasini sagla. Ayrica sirket disi/cihaza kalan
// eski is taslaklarini ve sepet/kutu verilerini surum gecisinde gecersiz kil.
;(function () {
  try {
    const SURUM = __APP_VERSION__
    const ANAHTAR = 'raspel_gorulen_surum'
    const onceki = localStorage.getItem(ANAHTAR)
    if (onceki && onceki !== SURUM) {
      if (window.caches && caches.keys) {
        caches.keys().then((k) => Promise.all(k.map((x) => caches.delete(x))))
      }
      if (navigator.serviceWorker && navigator.serviceWorker.getRegistrations) {
        navigator.serviceWorker.getRegistrations().then((r) => r.forEach((x) => x.unregister()))
      }
      // Surum atlarken gecici arayuz verileri temizlenir. Ancak cevrimdisi SATIS KUYRUGU
      // is verisidir; ASLA silinmez (senkronize edilmemis satislar kaybolmasin).
      localStorage.removeItem('raspel_kayitli_sepet')
      Object.keys(localStorage)
        .filter((k) => k.startsWith('raspel_taslak_'))
        .forEach((k) => localStorage.removeItem(k))
      try {
        const bekleyen = JSON.parse(localStorage.getItem('raspel_offline_satis_kuyrugu') || '[]')
        if (bekleyen.length > 0) {
          // eslint-disable-next-line no-console
          console.warn(`[RasPel] Senkronize edilmemis ${bekleyen.length} cevrimdisi satis kuyrukta bekliyor.`)
        }
      } catch {
        /* yoksay */
      }
    }
    localStorage.setItem(ANAHTAR, SURUM)
  } catch (e) { /* yoksay */ }
})()

const app = createApp(App)

app.config.globalProperties.formatCurrency = formatCurrency
app.config.globalProperties.formatDate = formatDate
app.config.globalProperties.formatDateTime = formatDateTime

app.use(createPinia())
app.use(router)
app.use(PrimeVue, {
    theme: {
      preset: RaspelPreset,
      options: { darkModeSelector: false }
    },
    locale: pvTr,
    // REDTEAM/Faz2.5: Katman merdiveni acikca tanimlandi.
    //
    // Varsayilanlar: overlay=1000, modal=1100. Uygulama ici z-index'ler
    // bunlarla KESISIYORDU:
    //   - mobil offline banner 1001 > overlay 1000 -> POS'ta cevrimdisi
    //     kuyruk banner'i acildiginda urun arama sonuclari GORUNMEZDI
    //   - mobil sidebar 1000 == overlay 1000 -> DOM sirasina bagliydi,
    //     yani kazara calisiyordu
    //   - SatirEylemleri menusu 1200 > modal 1100 -> Dialog maskinin
    //     USTUNDE, modal disinda gorunuyordu
    //
    // Siralama (kucukten buyuge): sticky < sidebar < overlay < menu < modal
    // < tooltip. Boylece bir menü bir Dialog'un ICINDE acildiginda maskinin
    // altinda kalir (dogru davranis).
    zIndex: {
      overlay: 1200,
      menu: 1250,
      modal: 1300,
      tooltip: 1400
    }
  })
app.use(ToastService)
app.use(ConfirmationService)
app.use(i18n)

app.component('AppDataTable', AppDataTable)
  app.component('ListeDurumu', defineAsyncComponent(() => import('./components/ListeDurumu.vue')))
app.component('PageHeader', PageHeader)
app.component('EmptyState', EmptyState)
app.component('SkeletonLoader', SkeletonLoader)
app.component('GecmisZamanCizelgesi', GecmisZamanCizelgesi)
app.component('FaturaGecmisDialog', FaturaGecmisDialog)
app.component('SatirEylemleri', SatirEylemleri)

app.directive('permission', permissionDirective)
app.directive('tablo-etiket', tabloEtiketDirective)

// PrimeVue 4'te bazi bilesenler yeniden adlandirildi (Dropdown->Select,
// InputSwitch->ToggleSwitch, TabView->Tabs...). Eski adlar hala calisir ancak
// her mount'ta "Deprecated since v4" uyarisi basar. Konsolu temiz tutmak icin
// yalnizca bu bilinen deprecation mesajlari filtrelenir.
const _orijinalWarn = console.warn
console.warn = (...args) => {
  const ilk = typeof args[0] === 'string' ? args[0] : ''
  if (ilk.includes('Deprecated since v4')) return
  _orijinalWarn.apply(console, args)
}

// Vue genel hata yakalayici: bir gorunum cokerse beyaz ekran yerine
// kullaniciya bilgi ver ve gerekiyorsa onbellek kurtarmasini tetikle.
app.config.errorHandler = (err, instance, info) => {
  // eslint-disable-next-line no-console
  console.error('[RasPel] Uygulama hatasi:', err, info)
}

// Yakalanmayan Promise redleri (or. store action'lari) konsolda kaybolmasin;
// kullaniciya gereksiz teknik detay gostermeden loglayalim. HTTP hatalari zaten
// axios interceptor tarafindan toast ile bildirilir.
window.addEventListener('unhandledrejection', (event) => {
  const reason = event.reason
  // Iptal edilmis istekler (AbortError) beklenen durumdur; gurultu yapmasin.
  if (reason && (reason.name === 'AbortError' || reason.code === 'ERR_CANCELED')) return
  // eslint-disable-next-line no-console
  console.error('[RasPel] Yakalanmayan promise hatasi:', reason)
})

app.mount('#app')

initTabloEtiketleri()

// Erisilebilirlik guvenlik agi: `title` (veya tooltip) tasiyip `aria-label`'i olmayan
// butonlara aria-label kopyala. Boylece mevcut tum gorunumlerdeki ikon-only butonlar
// ekran okuyucularda anlamli etiket kazanir; tek tek gorunum duzenlemek gerekmez.
// Performans: gozlemci tum dokumani degil yalnizca eklenen dugumleri isler; ilk
// yuklemede bir kez tam tarama yapilir.
;(function () {
  const etiketle = (kok) => {
    try {
      kok.querySelectorAll('button[title]:not([aria-label])').forEach((b) => {
        const baslik = b.getAttribute('title')
        if (baslik) b.setAttribute('aria-label', baslik)
      })
    } catch {
      /* yoksay */
    }
  }
  const duzelt = () => etiketle(document)
  duzelt()
  let zamanlayici = null
  let bekleyenler = []
  const planla = (dugumler) => {
    if (dugumler && dugumler.length) bekleyenler.push(...dugumler)
    if (zamanlayici) return
    // DOM toplu guncellemelerinde tek seferde calis (kisa debounce).
    zamanlayici = setTimeout(() => {
      zamanlayici = null
      const dugumler = bekleyenler
      bekleyenler = []
      if (!dugumler.length) {
        duzelt()
        return
      }
      for (const d of dugumler) {
        if (!d || d.nodeType !== 1) continue
        if (d.matches && d.matches('button[title]:not([aria-label])')) {
          const baslik = d.getAttribute('title')
          if (baslik) d.setAttribute('aria-label', baslik)
        }
        etiketle(d)
      }
    }, 300)
  }
  const observer = new MutationObserver((kayitlar) => {
    const eklenenler = []
    for (const kayit of kayitlar) kayit.addedNodes.forEach((n) => eklenenler.push(n))
    if (eklenenler.length) planla(eklenenler)
  })
  observer.observe(document.body, { childList: true, subtree: true })
})()

// Yeni surum hazir oldugunda gosterilen hafif sabit bildirim (bagimliliksiz).
let __guncellemeBannerVar = false
function guncellemeBildir() {
  if (__guncellemeBannerVar) return
  __guncellemeBannerVar = true
  const b = document.createElement('div')
  b.setAttribute('role', 'status')
  b.style.cssText = 'position:fixed;bottom:16px;right:16px;z-index:2147483000;display:flex;align-items:center;gap:10px;padding:10px 14px;border-radius:12px;background:var(--accent,#14b8a6);color:#fff;font:600 13px/1.2 system-ui,sans-serif;box-shadow:0 8px 24px rgba(0,0,0,.3);'
  const span = document.createElement('span')
  span.textContent = 'Yeni sürüm hazır'
  const btn = document.createElement('button')
  btn.textContent = 'Yenile'
  btn.style.cssText = 'background:#fff;color:#0f766e;border:none;padding:6px 12px;border-radius:8px;cursor:pointer;font-weight:700;'
  btn.onclick = () => window.location.reload()
  b.appendChild(span)
  b.appendChild(btn)
  document.body.appendChild(b)
}

// Legacy (surumsuz) service worker kayitlarini temizle: takili kalan eski SW
// beyaz ekrana yol acabiliyor. Surum degistiginde eski kayit kaldirilir.
(function () {
  if (!('serviceWorker' in navigator)) return
  const BEKLENEN = '/sw.js?v=' + __APP_VERSION__
  // Eski (surumsuz VEYA farkli surumlu) SW kaydini dusur. Farkli surumlu SW
  // takili kalirsa tarayici eski arayuzu onbellekten servis edebiliyor; bu
  // durumda kaydi kaldirip bir kez yenileriz (dongu korumali).
  const SURUM_ANAHTARI = 'raspel_sw_surum_temizligi'
  navigator.serviceWorker.getRegistrations().then((kayitlar) => {
    let eskiSurumVar = false
    kayitlar.forEach((r) => {
      const url = (r.active && r.active.scriptURL) || (r.installing && r.installing.scriptURL) || ''
      if (!url.includes('/sw.js')) return
      if (!url.includes('?v=') || !url.includes('?v=' + __APP_VERSION__)) {
        eskiSurumVar = true
        r.unregister().catch(() => {})
      }
    })
    if (eskiSurumVar && sessionStorage.getItem(SURUM_ANAHTARI) !== __APP_VERSION__) {
      sessionStorage.setItem(SURUM_ANAHTARI, __APP_VERSION__)
      window.location.reload()
    }
  }).catch(() => {})

  // Surumlu URL ile kaydet: surum degisince tarayici yeni SW'yi kesin yukler
  // (eski surumun onbelleginde takili kalma sorunu kalici olarak onlenir).
  // updateViaCache:'none' ile sw.js hicbir zaman HTTP onbelleginden gelmez.
  navigator.serviceWorker.register(BEKLENEN, { scope: '/', updateViaCache: 'none' })
    .then((reg) => {
      const kontrol = () => reg.update().then(() => {
        if (reg.waiting && navigator.serviceWorker.controller) guncellemeBildir()
      }).catch(() => {})
      kontrol()
      setInterval(kontrol, 60 * 60 * 1000)
    })
    .catch(() => {})

  // Yeni SW kontrolu devralinca bir kez yenile (taze icerik).
  let devralindi = false
  const ilkKontrolcuVar = !!navigator.serviceWorker.controller
  navigator.serviceWorker.addEventListener('controllerchange', () => {
    if (devralindi || !ilkKontrolcuVar) return
    devralindi = true
    window.location.reload()
  })
})()
