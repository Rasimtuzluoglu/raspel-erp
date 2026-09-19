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
  const isSaha = computed(() => kullanici.value?.sahaKullanici === true)
  const isDriver = computed(() => kullanici.value?.role === 'DRIVER')

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

  const AUTH_ANAHTAR = 'raspel_erp_auth'
  const authDeposu = (hatirla) => (hatirla ? localStorage : sessionStorage)
  const authOku = () => localStorage.getItem(AUTH_ANAHTAR) || sessionStorage.getItem(AUTH_ANAHTAR)
  const authTemizle = () => {
    localStorage.removeItem(AUTH_ANAHTAR)
    sessionStorage.removeItem(AUTH_ANAHTAR)
  }
  const authKaydet = (hatirla) => {
    const veri = JSON.stringify({
      kullanici: kullanici.value,
      companyName: companyName.value,
      sirketId: sirketId.value,
      sirketAdi: sirketAdi.value,
      yetkiler: yetkiler.value,
      tokenExpiresAt: tokenExpiresAt.value
    })
    authDeposu(hatirla).setItem(AUTH_ANAHTAR, veri)
    // Diğer depodaki eski kaydı temizle
    ;(hatirla ? sessionStorage : localStorage).removeItem(AUTH_ANAHTAR)
  }

  /**
   * Oturum geri yükleme. JWT yalnızca httpOnly cookie'de saklanır; tarayıcı
   * deposunda yalnızca kullanıcı bilgisi tutulur. "Beni hatırla" seçilmediyse
   * kayıt sessionStorage'da tutulur ve sekme kapanınca cookie ile birlikte kaybolur.
   */
  const init = async () => {
    try {
      const stored = authOku()
      if (stored) {
        const data = JSON.parse(stored)
        kullanici.value = data.kullanici
        token.value = '' // token asla depodan geri yüklenmez (httpOnly cookie)
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
      authTemizle()
    }
  }

  const girisYap = async (username, password, rememberMe) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.giris({ username, password, rememberMe: rememberMe === true })
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const girisSirket = async (girisToken, sirketId, rememberMe) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.girisSirket({ girisToken, sirketId, rememberMe: rememberMe === true })
      oturumKur(res.data, rememberMe === true)
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  // Oturum açıkken şirket değiştir
  const sirketDegistir = async (sirketId) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.sirketDegistir(sirketId)
      oturumKur(res.data)
      localStorage.setItem('raspel_erp_son_sirket', sirketId)
      return res.data
    } finally {
      loading.value = false
    }
  }

  // Oturum açmış kullanıcının erişebileceği şirketler
  const sirketlerim = async () => {
    try {
      const res = await kullaniciAPI.sirketlerim()
      return res.data || []
    } catch {
      return []
    }
  }

  const giris2fa = async (girisToken, code, rememberMe) => {
    loading.value = true
    try {
      const res = await kullaniciAPI.giris2fa({ girisToken, code, rememberMe: rememberMe === true })
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const oturumKur = (data, rememberMe) => {
    kullanici.value = {
      id: data.id,
      username: data.username,
      displayName: data.displayName,
      avatarUrl: data.avatarUrl,
      companyName: data.companyName,
      role: data.role,
      sahaKullanici: data.sahaKullanici === true,
      personelId: data.personelId ?? null
    }
    token.value = data.token || ''
    companyName.value = data.companyName || ''
    sirketId.value = data.sirketId || null
    sirketAdi.value = data.sirketAdi || ''
    tokenExpiresAt.value = data.tokenExpiresAt || null

    authKaydet(rememberMe === true)
    yetkileriYukle()
  }

  const cikisYap = () => {
    // Sunucu tarafında cookie'yi temizle ve oturumu iptal et (hata olsa da yerel temizlik yapılır).
    try {
      kullaniciAPI.cikis().catch(() => {})
    } catch {
      /* yoksay */
    }
    kullanici.value = null
    token.value = ''
    companyName.value = ''
    sirketId.value = null
    sirketAdi.value = ''
    yetkiler.value = []
    tokenExpiresAt.value = null
    authTemizle()
    hassasYerelVerileriTemizle()
  }

  /** Çıkışta cihazda kalan hassas iş verilerini ve servis çalışan önbelleğini temizler. */
  const hassasYerelVerileriTemizle = () => {
    try {
      localStorage.removeItem('raspel_offline_satis_kuyrugu')
      localStorage.removeItem('raspel_kayitli_sepet')
      Object.keys(localStorage)
        .filter((k) => k.startsWith('raspel_taslak_'))
        .forEach((k) => localStorage.removeItem(k))
    } catch {
      /* yoksay */
    }
    try {
      if (window.caches && caches.keys) {
        caches.keys().then((anahtarlar) => Promise.all(anahtarlar.map((k) => caches.delete(k)))).catch(() => {})
      }
      if (navigator.serviceWorker && navigator.serviceWorker.controller) {
        navigator.serviceWorker.controller.postMessage({ type: 'CLEAR_CACHES' })
      }
    } catch {
      /* yoksay */
    }
  }

  const kullaniciGuncelle = async () => {
    if (!kullanici.value?.id) return
    try {
      const r = await kullaniciAPI.ben()
      const oncekiPersonelId = kullanici.value?.personelId ?? null
      kullanici.value = { ...r.data, personelId: r.data?.personelId ?? oncekiPersonelId }
      companyName.value = r.data.companyName || companyName.value
      await yetkileriYukle()
      authKaydet(localStorage.getItem(AUTH_ANAHTAR) != null)
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
    isSaha,
    isDriver,
    hasPermission,
    girisYap,
    girisSirket,
    giris2fa,
    sirketDegistir,
    sirketlerim,
    cikisYap,
    kullaniciGuncelle,
    yetkileriYukle,
    init
  }
})
