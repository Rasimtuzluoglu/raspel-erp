import axios from 'axios'
import axiosRetry from 'axios-retry'
import { reactive } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const networkStatus = reactive({
  online: navigator.onLine,
  showBanner: false,
  // Tarayici "online" dese bile sunucuya ulasilamiyor olabilir (kapali port,
  // bozuk DNS, gateway). Bu bayrak ayri tutulur; online bayragini yalanla
  // cevrimdisi saymamak icin.
  sunucuyaUlasilamiyor: false
})

window.addEventListener('online', () => {
  networkStatus.online = true
  networkStatus.sunucuyaUlasilamiyor = false
  networkStatus.showBanner = false
})
window.addEventListener('offline', () => {
  networkStatus.online = false
  networkStatus.showBanner = true
})
window.addEventListener('focus', () => {
  if (navigator.onLine) networkStatus.showBanner = false
})

const VARSAYILAN_TIMEOUT_MS = 30000
// PDF/Excel/rapor üretimi sunucuda senkrondur (byte[] bellekte üretilir) ve veri
// hacmine göre dakikalar sürebilir. 30 sn'lik global timeout bu istekleri
// İSTEMCİDE iptal ediyordu: kullanıcı hata görüyor, sunucu ise işi
// tamamlamaya devam ediyor — kullanıcı tekrar denediğinde iş yükü ikiye
// katlanıyordu. Bu yüzden bu uçlara daha uzun süre tanınır.
const UZUN_ISLEM_TIMEOUT_MS = 180000
const UZUN_ISLEM_YOL_DESENI = /(^|\/)(exports?|raporlar?|rapor|belge|pdf|excel|backups)(\/|$)/

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: VARSAYILAN_TIMEOUT_MS,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

// Sunucu tarafi (PDF/fis) belge dili icin aktif arayuz dilini bildir.
apiClient.interceptors.request.use((config) => {
  try {
    const dil = (typeof localStorage !== 'undefined' && localStorage.getItem('lang') === 'en') ? 'en' : 'tr'
    config.headers = config.headers || {}
    config.headers['Accept-Language'] = dil
  } catch {
    /* yoksay */
  }
  // Uzun süren uçlar için süre uzatılır. Çağıran kendi timeout'unu verirse
  // (null ya da varsayılandan farklı) dokunulmaz.
  const uzunSureli = typeof config.url === 'string' && UZUN_ISLEM_YOL_DESENI.test(config.url)
  if (uzunSureli && (config.timeout == null || config.timeout === VARSAYILAN_TIMEOUT_MS)) {
    config.timeout = UZUN_ISLEM_TIMEOUT_MS
  }
  return config
})

import NProgress from 'nprogress'
import { useAuthStore } from '../stores/authStore.js'

let pendingRequests = 0
const handleNProgress = (isStart) => {
  if (isStart) {
    pendingRequests++
    NProgress.start()
  } else {
    pendingRequests = Math.max(0, pendingRequests - 1)
    if (pendingRequests === 0) {
      NProgress.done()
    }
  }
}

apiClient.interceptors.request.use(
  (config) => {
    handleNProgress(true)
    // JWT httpOnly cookie ile gönderilir (withCredentials: true).
    // Bellekteki token varsa ek güvenlik katmanı olarak Authorization header'ı da eklenir;
    // localStorage'dan token ASLA okunmaz (XSS ile çalınamaz).
    let token = ''
    try {
      const authStore = useAuthStore()
      token = authStore.token || ''
    } catch {
      /* empty */
    }
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    handleNProgress(false)
    return Promise.reject(error)
  }
)

let redirectKorumasi = false

// Ayni istek/URL icin kisa sure icinde tekrar eden bildirimleri bastirir.
// axios-retry ayni GET'i 2 kez denedigi icin hata toast'i defalarca tetiklenip
// arayuzu mesgul ediyordu.
const SON_HATA_YAYINI = new Map()
const HATA_YAYIN_ARALIGI_MS = 5000

const hataYayinlanabilir = (anahtar) => {
  const simdi = Date.now()
  const onceki = SON_HATA_YAYINI.get(anahtar)
  if (onceki != null && simdi - onceki < HATA_YAYIN_ARALIGI_MS) return false
  SON_HATA_YAYINI.set(anahtar, simdi)
  for (const [k, zaman] of SON_HATA_YAYINI) {
    if (simdi - zaman > 60000) SON_HATA_YAYINI.delete(k)
  }
  return true
}

// Sunucu mesaj uretemeyen hatalarda gosterilecek yerellestirilmis metinler.
// Eskiden bu durumda hicbir bildirim cikmiyordu: kullanici "Kaydet"e tiklayip
// ekranda hicbir sey olmadigini goruyordu.
const SUNUCUSUZ_HATA_ANAHTARI = {
  baglantiHatasi: 'common.baglantiHatasi',
  zamanAsimi: 'common.zamanAsimiHatasi',
  sunucuHatasi: 'common.sunucuHatasi'
}

/**
 * Tek noktadan hata bildirimi.
 * @param {number} status HTTP durumu (yoksa 0)
 * @param {string} url istek adresi (tekrar bastirmada ayirt etmek icin)
 * @param {string|null} sunucuMesaji backend'den gelen is metni (varsa onceliklidir)
 * @param {string|null} anahtar sunucu mesaji yoksa kullanilacak i18n anahtari
 */
const hataYayinla = (status, url, sunucuMesaji, anahtar) => {
  const ayirtEdici = sunucuMesaji || anahtar || ''
  const dedupeAnahtari = `${status}:${url || ''}:${ayirtEdici}`
  if (!hataYayinlanabilir(dedupeAnahtari)) return
  const detail = sunucuMesaji ? { status, message: sunucuMesaji } : { status, anahtar }
  window.dispatchEvent(new CustomEvent('api-error', { detail }))
}

const zamanAsimiMi = (error) => error.code === 'ECONNABORTED' || error.code === 'ETIMEDOUT'

apiClient.interceptors.response.use(
  (response) => {
    handleNProgress(false)
    // Basarili bir istek sunucunun erisilebilir oldugunun kanitidir.
    if (networkStatus.sunucuyaUlasilamiyor) {
      networkStatus.sunucuyaUlasilamiyor = false
      if (networkStatus.online) networkStatus.showBanner = false
    }
    // Başarılı bir istek geldiğinde koruma bayrağını sıfırla; aksi halde ilk 401/403
    // sonrası (SPA yeniden yüklenmeden tekrar giriş yapılsa bile) sonraki oturum
    // kaybında yönlendirme bir daha çalışmaz ve kullanıcı sessizce takılı kalır.
    redirectKorumasi = false
    return response
  },
  async (error) => {
    handleNProgress(false)
    if (!error.response) {
      // Sunucuya hic ulasilamadi: DNS, baglanti reddi, CORS veya zaman asimi.
      // Zaman asiminda sunucu cok yavas olabilir; bu yuzden cevrimdisi banner'i
      // gosterilmez, yalnizca bilgilendirilir.
      const zamanAsimi = zamanAsimiMi(error)
      if (!zamanAsimi) {
        networkStatus.showBanner = true
        networkStatus.sunucuyaUlasilamiyor = true
      }
      hataYayinla(0, error.config?.url, null,
        zamanAsimi ? SUNUCUSUZ_HATA_ANAHTARI.zamanAsimi : SUNUCUSUZ_HATA_ANAHTARI.baglantiHatasi)
      return Promise.reject(error)
    }
    let { status, data } = error.response
    // responseType: 'blob' isteklerinde hata gövdesi Blob olur; JSON'a çevir.
    if (typeof Blob !== 'undefined' && data instanceof Blob) {
      try {
        const text = await data.text()
        data = JSON.parse(text)
        error.response.data = data
      } catch {
        data = null
      }
    }

    // Global Toast Trigger.
    // Oncelik sirasi: sunucunun is metni > durum koduna karsilik gelen sabit
    // mesaj. Eski davranista yalnizca sunucu mesaji varsa bildirim cikiyordu;
    // Traefik/nginx'in 502/503/504 HTML govdesi veya bos govdeli 500'de ekranda
    // hicbir sey gorunmuyordu.
    if (status >= 400 && status !== 401) {
      const errorMsg = data?.message || data?.error
      if (errorMsg) {
        hataYayinla(status, error.config?.url, errorMsg, null)
      } else if (status >= 500) {
        hataYayinla(status, error.config?.url, null, SUNUCUSUZ_HATA_ANAHTARI.sunucuHatasi)
      }
    }

    if (status === 401 && !window.location.pathname.startsWith('/giris')) {
      if (redirectKorumasi) return Promise.reject(error)
      redirectKorumasi = true
      try {
        const authStore = useAuthStore()
        authStore.cikisYap()
      } catch {
        /* empty */
      }

      import('../router/index.js').then(({ default: router }) => {
        router.push({ name: 'Giris', query: { redirect: router.currentRoute.value.fullPath } })
      })
      return Promise.reject(error)
    }
    if (status === 403 && !window.location.pathname.startsWith('/giris')) {
      // Yalnızca token süresi dolmuşsa oturumu kapat ve girişe yönlendir.
      // Aksi halde (rol kaynaklı 403) route erişimi router guard'ı tarafından yönetilir;
      // arka plan isteklerinin kullanıcıyı yetki-reddi sayfasına düşürmesini engelliyoruz.
      const tokenSuresiDolmus = tokenSuresiDolduMu()
      if (tokenSuresiDolmus) {
        if (redirectKorumasi) return Promise.reject(error)
        redirectKorumasi = true
        try {
          const authStore = useAuthStore()
          authStore.cikisYap()
        } catch {
          /* empty */
        }
        import('../router/index.js').then(({ default: router }) => {
          router.push({ name: 'Giris', query: { redirect: router.currentRoute.value.fullPath } })
        })
      }
    }
    return Promise.reject(error)
  }
)

const tokenSuresiDolduMu = () => {
  try {
    // "Beni hatırla" kapalıysa kayıt sessionStorage'da tutulur; ikisini de kontrol et.
    const ham = localStorage.getItem('raspel_erp_auth') || sessionStorage.getItem('raspel_erp_auth')
    const kayitli = JSON.parse(ham || '{}')
    const bitis = kayitli.tokenExpiresAt
    if (!bitis) return false
    return bitis < Date.now()
  } catch {
    return false
  }
}

axiosRetry(apiClient, {
  retries: 2,
  retryDelay: (retryCount) => retryCount * 1000,
  retryCondition: (error) => {
    // Yalnızca idempotent istekler tekrar denenir. POST/PATCH tekrarı finansal
    // belgelerin (fatura/tahsilat/sipariş) mükerrer oluşmasına yol açabilir.
    const method = (error.config?.method || 'get').toLowerCase()
    const idempotent = ['get', 'head', 'options', 'put', 'delete'].includes(method)
    return idempotent && (!error.response || error.response.status >= 500)
  },
  onRetry: (retryCount, error) => {
    console.warn(`API retry (${retryCount}/2):`, error.config?.url)
  }
})

export { apiClient }
export default apiClient
