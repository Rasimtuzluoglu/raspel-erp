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
    let token = ''
    try {
      const authStore = useAuthStore()
      token = authStore.token || ''
    } catch {
      /* empty */
    }
    if (!token) {
      try {
        const kayitli = JSON.parse(localStorage.getItem('raspel_erp_auth') || '{}')
        token = kayitli.token || ''
      } catch {
        /* empty */
      }
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
  (error) => {
    handleNProgress(false)
    if (!error.response) {
      networkStatus.showBanner = true
      return Promise.reject(error)
    }
    const { status, data } = error.response

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
    if (
      status === 403 &&
      !window.location.pathname.startsWith('/giris') &&
      !window.location.pathname.startsWith('/yetki-reddi')
    ) {
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
      } else {
        import('../router/index.js').then(({ default: router }) => {
          router.push('/yetki-reddi')
        })
      }
    }
    return Promise.reject(error)
  }
)

const tokenSuresiDolduMu = () => {
  try {
    const kayitli = JSON.parse(localStorage.getItem('raspel_erp_auth') || '{}')
    const token = kayitli.token || ''
    if (!token) return true
    const payload = token.split('.')[1]
    const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')))
    if (!decoded.exp) return false
    return decoded.exp * 1000 < Date.now()
  } catch {
    return false
  }
}

axiosRetry(apiClient, {
  retries: 2,
  retryDelay: (retryCount) => retryCount * 1000,
  retryCondition: (error) => {
    return !error.response || error.response.status >= 500
  },
  onRetry: (retryCount, error) => {
    console.warn(`API retry (${retryCount}/2):`, error.config?.url)
  }
})

export { apiClient }
export default apiClient
