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
      const userRol = roller.find(r => r.ad === kullanici.value.role)
      if (userRol && userRol.yetkiler) {
        yetkiler.value = userRol.yetkiler.map(y => y.kod)
      }
    } catch {}
  }

  const init = () => {
    try {
      const stored = localStorage.getItem('raspel_erp_auth')
      if (stored) {
        const data = JSON.parse(stored)
        kullanici.value = data.kullanici
        token.value = data.token || ''
        companyName.value = data.companyName || ''
        sirketId.value = data.sirketId || null
        sirketAdi.value = data.sirketAdi || ''
        yetkiler.value = data.yetkiler || []
        if (kullanici.value && yetkiler.value.length === 0) {
          yetkileriYukle()
        }
      }
    } catch { localStorage.removeItem('raspel_erp_auth') }
  }

  const girisYap = async (username, password, sirketAdiParam, sirketIdParam) => {
    loading.value = true
    try {
      const loginData = { username, password, companyName: sirketAdiParam }
      if (sirketIdParam) loginData.sirketId = sirketIdParam
      const res = await kullaniciAPI.giris(loginData)
      if (res.data?.twoFactorGerekli) {
        return res.data
      }
      oturumKur(res.data, sirketAdiParam)
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const giris2fa = async (girisToken, code, sirketAdiParam, sirketIdParam) => {
    loading.value = true
    try {
      const loginData = { girisToken, code, companyName: sirketAdiParam }
      if (sirketIdParam) loginData.sirketId = sirketIdParam
      const res = await kullaniciAPI.giris2fa(loginData)
      oturumKur(res.data, sirketAdiParam)
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const oturumKur = (data, sirketAdiParam) => {
    kullanici.value = { id: data.id, username: data.username, displayName: data.displayName, avatarUrl: data.avatarUrl, companyName: data.companyName, role: data.role }
    token.value = data.token || ''
    companyName.value = sirketAdiParam || data.companyName || ''
    sirketId.value = data.sirketId || null
    sirketAdi.value = data.sirketAdi || ''

    localStorage.setItem('raspel_erp_auth', JSON.stringify({
      kullanici: kullanici.value,
      token: token.value,
      companyName: companyName.value,
      sirketId: sirketId.value,
      sirketAdi: sirketAdi.value,
      yetkiler: yetkiler.value
    }))
    yetkileriYukle()
  }

  const cikisYap = () => {
    kullanici.value = null
    token.value = ''
    companyName.value = ''
    sirketId.value = null
    sirketAdi.value = ''
    yetkiler.value = []
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
      localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: kullanici.value,
        token: token.value,
        companyName: companyName.value,
        sirketId: sirketId.value,
        sirketAdi: sirketAdi.value,
        yetkiler: yetkiler.value
      }))
    } catch {}
  }

  init()

  return { kullanici, token, companyName, sirketId, sirketAdi, yetkiler, loading, isLoggedIn, isAdmin, hasPermission, girisYap, giris2fa, cikisYap, kullanicilariGetir, kullaniciGuncelle, yetkileriYukle, init }
})