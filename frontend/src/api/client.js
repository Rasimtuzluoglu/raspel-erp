import axios from 'axios'
import axiosRetry from 'axios-retry'
import { reactive } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const networkStatus = reactive({
  online: navigator.onLine,
  showBanner: false
})

window.addEventListener('online', () => {
  networkStatus.online = true
  networkStatus.showBanner = false
})
window.addEventListener('offline', () => {
  networkStatus.online = false
  networkStatus.showBanner = true
})
window.addEventListener('focus', () => {
  if (navigator.onLine) networkStatus.showBanner = false
})

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
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

apiClient.interceptors.response.use(
  (response) => {
    handleNProgress(false)
    return response
  },
  async (error) => {
    handleNProgress(false)
    if (!error.response) {
      networkStatus.showBanner = true
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

    // Global Toast Trigger
    if (status >= 400 && status !== 401) {
      const errorMsg = data?.message || data?.error || 'Bir hata oluştu.'
      window.dispatchEvent(new CustomEvent('api-error', { detail: { status, message: errorMsg } }))
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
    const kayitli = JSON.parse(localStorage.getItem('raspel_erp_auth') || '{}')
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
