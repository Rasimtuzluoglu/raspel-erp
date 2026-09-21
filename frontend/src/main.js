import { createApp, defineAsyncComponent } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import Lara from '@primevue/themes/lara'
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
      // Surum atlarken sema degismis olabilir; kalici is verilerini temizle.
      localStorage.removeItem('raspel_offline_satis_kuyrugu')
      localStorage.removeItem('raspel_kayitli_sepet')
      Object.keys(localStorage)
        .filter((k) => k.startsWith('raspel_taslak_'))
        .forEach((k) => localStorage.removeItem(k))
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
    preset: Lara,
    options: { darkModeSelector: false }
  },
  locale: pvTr
})
app.use(ToastService)
app.use(ConfirmationService)
app.use(i18n)

app.component('AppDataTable', AppDataTable)
app.component('PageHeader', PageHeader)
app.component('EmptyState', EmptyState)
app.component('SkeletonLoader', SkeletonLoader)
app.component('GecmisZamanCizelgesi', GecmisZamanCizelgesi)
app.component('FaturaGecmisDialog', FaturaGecmisDialog)
app.component('SatirEylemleri', SatirEylemleri)

app.directive('permission', permissionDirective)
app.directive('tablo-etiket', tabloEtiketDirective)

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

// Legacy (surumsuz) service worker kayitlarini temizle: takili kalan eski SW
// beyaz ekrana yol acabiliyor. Surum degistiginde eski kayit kaldirilir.
;(function () {
  if (!('serviceWorker' in navigator)) return
  const BEKLENEN = '/sw.js?v=' + __APP_VERSION__
  navigator.serviceWorker.getRegistrations().then((kayitlar) => {
    kayitlar.forEach((r) => {
      const url = (r.active && r.active.scriptURL) || (r.installing && r.installing.scriptURL) || ''
      // Surum sorgusu olmayan eski sw.js kayitlarini kaldir.
      if (url.endsWith('/sw.js') && !url.includes('?v=')) {
        r.unregister().catch(() => {})
      }
    })
  }).catch(() => {})

  // Surumlu URL ile kaydet: surum degisince tarayici yeni SW'yi kesin yukler
  // (eski surumun onbelleginde takili kalma sorunu kalici olarak onlenir).
  navigator.serviceWorker.register(BEKLENEN, { scope: '/' })
    .then((reg) => {
      reg.update().catch(() => {})
      setInterval(() => reg.update().catch(() => {}), 60 * 60 * 1000)
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
