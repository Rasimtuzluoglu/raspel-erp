import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kullaniciAPI, apiClient } from '../api/index.js'

export const useAuthStore = defineStore('auth', () => {
  const kullanici = ref(null)
  const token = ref('')
  const companyName = ref('')
  const sirketId = ref(null)
  const sirketAdi = ref('')
  const yetkiler = ref([])
  const loading = ref(false)
  const tokenExpiresAt = ref(null)

  const isLoggedIn = computed(() => !!kullanici.value)
  const isAdmin = computed(() => kullanici.value?.role === 'ADMIN')

  const hasPermission = (permissionCode) => {
    if (!kullanici.value) return false
    if (kullanici.value.role === 'ADMIN') return true
    if (!permissionCode) return true
    return yetkiler.value.includes(permissionCode)
  }

  const yetkileriYukle = async () => {
    if (!kullanici.value?.role) return
    if (kullanici.value.role === 'ADMIN') {
      yetkiler.value = ['*']
      return
    }
    try {
      const res = await apiClient.get('/yetkiler/roller')
      const roller = res.data || []
      const userRol = roller.find((r) => r.ad === kullanici.value.role)
      if (userRol && userRol.yetkiler) {
        yetkiler.value = userRol.yetkiler.map((y) => y.kod)
      }
    } catch {
      /* empty */
    }
  }

  /**
   * Oturum geri yükleme. JWT yalnızca httpOnly cookie'de saklanır; localStorage'da
   * yalnızca kullanıcı bilgisi ve oturum bitiş zamanı tutulur (XSS ile token
   * çalınamaz). Sayfa yenilendiğinde cookie otomatik gönderilir; burada yalnızca
   * kullanıcı bilgisi yenilenir. Cookie geçersizse /ben 401 döner ve oturum kapanır.
   */
  const init = async () => {
    try {
      const stored = localStorage.getItem('raspel_erp_auth')
      if (stored) {
        const data = JSON.parse(stored)
        kullanici.value = data.kullanici
        token.value = '' // token asla localStorage'dan geri yüklenmez (httpOnly cookie)
        companyName.value = data.companyName || ''
        sirketId.value = data.sirketId || null
        sirketAdi.value = data.sirketAdi || ''
        yetkiler.value = data.yetkiler || []
        tokenExpiresAt.value = data.tokenExpiresAt || null
        if (kullanici.value) {
          if (yetkiler.value.length === 0) yetkileriYukle()
          await kullaniciGuncelle()
        }
      }
    } catch {
      localStorage.removeItem('raspel_erp_auth')
    }
  }

  const girisYap = async (username, password) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.giris({ username, password })
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const girisSirket = async (girisToken, sirketId) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.girisSirket({ girisToken, sirketId })
      oturumKur(res.data)
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const giris2fa = async (girisToken, code) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.giris2fa({ girisToken, code })
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const oturumKur = (data) => {
    kullanici.value = {
      id: data.id,
      username: data.username,
      displayName: data.displayName,
      avatarUrl: data.avatarUrl,
      companyName: data.companyName,
      role: data.role
    }
    token.value = data.token || ''
    companyName.value = data.companyName || ''
    sirketId.value = data.sirketId || null
    sirketAdi.value = data.sirketAdi || ''
    tokenExpiresAt.value = data.tokenExpiresAt || null

    localStorage.setItem(
      'raspel_erp_auth',
      JSON.stringify({
        kullanici: kullanici.value,
        companyName: companyName.value,
        sirketId: sirketId.value,
        sirketAdi: sirketAdi.value,
        yetkiler: yetkiler.value,
        tokenExpiresAt: tokenExpiresAt.value
      })
    )
    yetkileriYukle()
  }

  const cikisYap = () => {
    kullanici.value = null
    token.value = ''
    companyName.value = ''
    sirketId.value = null
    sirketAdi.value = ''
    yetkiler.value = []
    tokenExpiresAt.value = null
    localStorage.removeItem('raspel_erp_auth')
  }

  const kullanicilariGetir = async () => {
    const res = await kullaniciAPI.getAll()
    return res.data
  }

  const kullaniciGuncelle = async () => {
    if (!kullanici.value?.id) return
    try {
      const r = await kullaniciAPI.ben()
      kullanici.value = r.data
      companyName.value = r.data.companyName || companyName.value
      await yetkileriYukle()
      localStorage.setItem(
        'raspel_erp_auth',
        JSON.stringify({
          kullanici: kullanici.value,
          companyName: companyName.value,
          sirketId: sirketId.value,
          sirketAdi: sirketAdi.value,
          yetkiler: yetkiler.value,
          tokenExpiresAt: tokenExpiresAt.value
        })
      )
    } catch (err) {
      // Oturum geçersizse (401/403) sessizce çıkış yap; geçici sunucu hatalarında (5xx) dokunma.
      if (err?.response?.status === 401 || err?.response?.status === 403) {
        cikisYap()
      }
    }
  }

  init()

  return {
    kullanici,
    token,
    companyName,
    sirketId,
    sirketAdi,
    yetkiler,
    tokenExpiresAt,
    loading,
    isLoggedIn,
    isAdmin,
    hasPermission,
    girisYap,
    girisSirket,
    giris2fa,
    cikisYap,
    kullanicilariGetir,
    kullaniciGuncelle,
    yetkileriYukle,
    init
  }
})
