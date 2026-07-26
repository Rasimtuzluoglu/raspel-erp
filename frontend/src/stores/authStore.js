import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { kullaniciAPI } from '../api/index.js'

export const useAuthStore = defineStore('auth', () => {
  const kullanici = ref(null)
  const token = ref('')
  const companyName = ref('')
  const sirketId = ref(null)
  const sirketAdi = ref('')
  const loading = ref(false)

  const isLoggedIn = computed(() => !!kullanici.value)

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
      }
    } catch { localStorage.removeItem('raspel_erp_auth') }
  }

  const girisYap = async (username, password, sirketAdiParam, sirketIdParam) => {
    loading.value = true
    try {
      const loginData = { username, password, companyName: sirketAdiParam }
      if (sirketIdParam) loginData.sirketId = sirketIdParam
      const res = await kullaniciAPI.giris(loginData)
      kullanici.value = { id: res.data.id, username: res.data.username, displayName: res.data.displayName, avatarUrl: res.data.avatarUrl, companyName: res.data.companyName, role: res.data.role }
      token.value = res.data.token || ''
      companyName.value = sirketAdiParam || res.data.companyName || ''
      sirketId.value = res.data.sirketId || null
      sirketAdi.value = res.data.sirketAdi || ''
      localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: kullanici.value,
        token: token.value,
        companyName: companyName.value,
        sirketId: sirketId.value,
        sirketAdi: sirketAdi.value
      }))
      return res.data
    } catch (err) {
      cikisYap()
      throw err
    } finally {
      loading.value = false
    }
  }

  const cikisYap = () => {
    kullanici.value = null
    token.value = ''
    companyName.value = ''
    sirketId.value = null
    sirketAdi.value = ''
    localStorage.removeItem('raspel_erp_auth')
  }

  const kullanicilariGetir = async () => {
    const res = await kullaniciAPI.getAll()
    return res.data
  }

  const kullaniciGuncelle = async () => {
    if (!kullanici.value?.id) return
    try {
      const r = await kullaniciAPI.getById(kullanici.value.id)
      kullanici.value = r.data
      companyName.value = r.data.companyName || companyName.value
      localStorage.setItem('raspel_erp_auth', JSON.stringify({
        kullanici: kullanici.value,
        companyName: companyName.value,
        sirketId: sirketId.value,
        sirketAdi: sirketAdi.value
      }))
    } catch {}
  }

  init()

  return { kullanici, token, companyName, sirketId, sirketAdi, loading, isLoggedIn, girisYap, cikisYap, kullanicilariGetir, init }
})