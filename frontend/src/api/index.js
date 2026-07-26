import axios from 'axios'
import axiosRetry from 'axios-retry'
import { reactive } from 'vue'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

export const networkStatus = reactive({
  online: navigator.onLine,
  showBanner: false
})

window.addEventListener('online', () => { networkStatus.online = true; networkStatus.showBanner = false })
window.addEventListener('offline', () => { networkStatus.online = false; networkStatus.showBanner = true })
window.addEventListener('focus', () => { if (navigator.onLine) networkStatus.showBanner = false })

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json'
  }
})

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

import { useAuthStore } from '../stores/authStore.js'

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (!error.response) {
      networkStatus.showBanner = true
      return Promise.reject(error)
    }
    const { status } = error.response
    if (status === 401 && !window.location.pathname.startsWith('/giris')) {
      try {
        const authStore = useAuthStore()
        authStore.cikisYap()
      } catch {}
      window.location.href = '/giris'
    }
    if (status === 403 && !window.location.pathname.startsWith('/giris') && !window.location.pathname.startsWith('/yetki-reddi')) {
      window.location.href = '/yetki-reddi'
    }
    return Promise.reject(error)
  }
)

/**
 * Cari Hesap API'si
 */
export const cariHesapAPI = {
  getAll() {
    return apiClient.get('/cari-hesaplar')
  },

  getById(id) {
    return apiClient.get(`/cari-hesaplar/${id}`)
  },

  search(query) {
    return apiClient.get(`/cari-hesaplar/search`, { params: { q: query } })
  },

  create(data) {
    return apiClient.post('/cari-hesaplar', data)
  },

  update(id, data) {
    return apiClient.put(`/cari-hesaplar/${id}`, data)
  },

  delete(id) {
    return apiClient.delete(`/cari-hesaplar/${id}`)
  }
}

/**
 * Hareket API'si
 */
export const hareketAPI = {
  getByCariHesap(cariHesapId) {
    return apiClient.get(`/hareketler/cari/${cariHesapId}`)
  },

  getSon(limit = 5) {
    return apiClient.get(`/hareketler/son/${limit}`)
  },

  getAll() {
    return apiClient.get('/hareketler')
  },

  filtrele(params) {
    return apiClient.get('/hareketler', { params })
  },

  create(data) {
    return apiClient.post('/hareketler', data)
  },

  update(id, data) {
    return apiClient.put(`/hareketler/${id}`, data)
  },

  delete(id) {
    return apiClient.delete(`/hareketler/${id}`)
  }
}

/**
 * Dashboard API'si
 */
export const dashboardAPI = {
  // Dashboard verilerini getir
  getData() {
    return apiClient.get('/dashboard')
  }
}

/**
 * Fatura API'si
 */
export const faturaAPI = {
  getAll() {
    return apiClient.get('/faturalar')
  },
  getById(id) {
    return apiClient.get(`/faturalar/${id}`)
  },
  create(data) {
    return apiClient.post('/faturalar', data)
  },
  update(id, data) {
    return apiClient.put(`/faturalar/${id}`, data)
  },
  updateDurum(id, durum) {
    return apiClient.put(`/faturalar/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/faturalar/${id}`)
  }
}

/**
 * Banka API'si
 */
export const bankaAPI = {
  getAll() {
    return apiClient.get('/bankalar')
  },
  getById(id) {
    return apiClient.get(`/bankalar/${id}`)
  },
  create(data) {
    return apiClient.post('/bankalar', data)
  },
  update(id, data) {
    return apiClient.put(`/bankalar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/bankalar/${id}`)
  }
}

/**
 * Kasa API'si
 */
export const kasaAPI = {
  getAll() { return apiClient.get('/kasalar') },
  getById(id) { return apiClient.get(`/kasalar/${id}`) },
  create(data) { return apiClient.post('/kasalar', data) },
  update(id, data) { return apiClient.put(`/kasalar/${id}`, data) },
  delete(id) { return apiClient.delete(`/kasalar/${id}`) },
  getHareketler(id) { return apiClient.get(`/kasalar/${id}/hareketler`) },
  addHareket(id, data) { return apiClient.post(`/kasalar/${id}/hareketler`, data) },
  deleteHareket(hareketId) { return apiClient.delete(`/kasalar/hareketler/${hareketId}`) }
}

/**
 * Kategori API'si
 */
export const kategoriAPI = {
  getAll() { return apiClient.get('/kategoriler') },
  getByTur(tur) { return apiClient.get(`/kategoriler/tur/${tur}`) },
  create(data) { return apiClient.post('/kategoriler', data) },
  delete(id) { return apiClient.delete(`/kategoriler/${id}`) }
}

/**
 * Rapor API'si
 */
export const raporAPI = {
  cariEkstre(params) { return apiClient.get('/raporlar/cari-ekstre', { params }) },
  gelirGider(params) { return apiClient.get('/raporlar/gelir-gider', { params }) },
  kdv(params) { return apiClient.get('/raporlar/kdv', { params }) },
  yaslandirma() { return apiClient.get('/raporlar/yaslandirma') }
}

/**
 * Kullanıcı API'si
 */
export const kullaniciAPI = {
  getAll() { return apiClient.get('/kullanicilar') },
  getById(id) { return apiClient.get(`/kullanicilar/${id}`) },
  create(data) { return apiClient.post('/kullanicilar', data) },
  update(id, data) { return apiClient.put(`/kullanicilar/${id}`, data) },
  delete(id) { return apiClient.delete(`/kullanicilar/${id}`) },
  giris(data) { return apiClient.post('/kullanicilar/giris', data) },
  sifreDegistir(data) { return apiClient.put('/kullanicilar/sifre-degistir', data) }
}

/**
 * Stok API'si
 */
export const stokAPI = {
  getAll() { return apiClient.get('/stoklar') },
  ara(q) { return apiClient.get('/stoklar/ara', { params: { q } }) },
  getById(id) { return apiClient.get(`/stoklar/${id}`) },
  create(data) { return apiClient.post('/stoklar', data) },
  update(id, data) { return apiClient.put(`/stoklar/${id}`, data) },
  delete(id) { return apiClient.delete(`/stoklar/${id}`) },
  getHareketler(id) { return apiClient.get(`/stoklar/${id}/hareketler`) },
  tumHareketler() { return apiClient.get('/stoklar/hareketler/tum') },
  addHareket(id, data) { return apiClient.post(`/stoklar/${id}/hareketler`, data) },
  deleteHareket(hareketId) { return apiClient.delete(`/stoklar/hareketler/${hareketId}`) }
}

/**
 * Şirket API'si
 */
export const sirketAPI = {
  getAll() { return apiClient.get('/sirketler') },
  getAktif() { return apiClient.get('/sirketler/aktif') },
  getById(id) { return apiClient.get(`/sirketler/${id}`) },
  create(data) { return apiClient.post('/sirketler', data) },
  update(id, data) { return apiClient.put(`/sirketler/${id}`, data) },
  delete(id) { return apiClient.delete(`/sirketler/${id}`) }
}

export const uploadAPI = {
  uploadSirketLogo(file) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/upload/sirket-logo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}

/**
 * Dönem API'si
 */
export const donemAPI = {
  getAll() { return apiClient.get('/donemler') },
  getBySirket(sirketId) { return apiClient.get(`/donemler/sirket/${sirketId}`) },
  getAktif(sirketId) { return apiClient.get(`/donemler/sirket/${sirketId}/aktif`) },
  getById(id) { return apiClient.get(`/donemler/${id}`) },
  create(data) { return apiClient.post('/donemler', data) },
  update(id, data) { return apiClient.put(`/donemler/${id}`, data) },
  delete(id) { return apiClient.delete(`/donemler/${id}`) }
}

/**
 * Satın Alma Talepleri API'si
 */
export const satinalmaTalepAPI = {
  getAll(params) { return apiClient.get('/satinalma-talepler', { params }) },
  getById(id) { return apiClient.get(`/satinalma-talepler/${id}`) },
  create(data) { return apiClient.post('/satinalma-talepler', data) },
  update(id, data) { return apiClient.put(`/satinalma-talepler/${id}`, data) },
  durumGuncelle(id, durum) { return apiClient.put(`/satinalma-talepler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/satinalma-talepler/${id}`) }
}

/**
 * Satın Alma Siparişleri API'si
 */
export const satinalmaSiparisAPI = {
  getAll(params) { return apiClient.get('/satinalma-siparisler', { params }) },
  getById(id) { return apiClient.get(`/satinalma-siparisler/${id}`) },
  create(data) { return apiClient.post('/satinalma-siparisler', data) },
  update(id, data) { return apiClient.put(`/satinalma-siparisler/${id}`, data) },
  durumGuncelle(id, durum) { return apiClient.put(`/satinalma-siparisler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/satinalma-siparisler/${id}`) }
}

/**
 * Personel API'si
 */
export const personelAPI = {
  getAll(params) { return apiClient.get('/personel', { params }) },
  getById(id) { return apiClient.get(`/personel/${id}`) },
  create(data) { return apiClient.post('/personel', data) },
  update(id, data) { return apiClient.put(`/personel/${id}`, data) },
  delete(id) { return apiClient.delete(`/personel/${id}`) }
}

/**
 * Personel İzin API'si
 */
export const personelIzinAPI = {
  getAll() { return apiClient.get('/personel-izin') },
  getByPersonel(personelId) { return apiClient.get(`/personel-izin/personel/${personelId}`) },
  create(data) { return apiClient.post('/personel-izin', data) },
  durumGuncelle(id, durum, onaylayan) { return apiClient.put(`/personel-izin/${id}/durum`, { durum, onaylayan }) },
  delete(id) { return apiClient.delete(`/personel-izin/${id}`) }
}

/**
 * Sipariş / Teklif API'si
 */
export const siparisAPI = {
  getAll(params) { return apiClient.get('/siparisler', { params }) },
  getById(id) { return apiClient.get(`/siparisler/${id}`) },
  create(data) { return apiClient.post('/siparisler', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/siparisler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/siparisler/${id}`) }
}

/**
 * Çek/Senet API'si
 */
export const cekSenetAPI = {
  getAll(params) { return apiClient.get('/cek-senet', { params }) },
  getById(id) { return apiClient.get(`/cek-senet/${id}`) },
  create(data) { return apiClient.post('/cek-senet', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/cek-senet/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/cek-senet/${id}`) }
}

/**
 * İrsaliye API'si
 */
export const irsaliyeAPI = {
  getAll(params) { return apiClient.get('/irsaliyeler', { params }) },
  getById(id) { return apiClient.get(`/irsaliyeler/${id}`) },
  create(data) { return apiClient.post('/irsaliyeler', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/irsaliyeler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/irsaliyeler/${id}`) }
}

/**
 * Proje API'si
 */
export const projeAPI = {
  getAll(params) { return apiClient.get('/projeler', { params }) },
  getById(id) { return apiClient.get(`/projeler/${id}`) },
  create(data) { return apiClient.post('/projeler', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/projeler/${id}/durum`, { durum }) },
  gorevEkle(projeId, data) { return apiClient.post(`/projeler/${projeId}/gorevler`, data) },
  gorevDurumGuncelle(gorevId, durum) { return apiClient.put(`/projeler/gorev/${gorevId}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/projeler/${id}`) }
}

/**
 * Audit Log API'si
 */
export const auditLogAPI = {
  getAll(params) { return apiClient.get('/audit-log', { params }) },
  getIslemTipleri() { return apiClient.get('/audit-log/islem-tipleri') },
  getEntityListesi() { return apiClient.get('/audit-log/entity-listesi') }
}

/**
 * Sube (Branch) API'si
 */
export const subeAPI = {
  getAll() { return apiClient.get('/subeler') },
  getAktif() { return apiClient.get('/subeler/aktif') },
  getById(id) { return apiClient.get(`/subeler/${id}`) },
  create(data) { return apiClient.post('/subeler', data) },
  update(id, data) { return apiClient.put(`/subeler/${id}`, data) },
  delete(id) { return apiClient.delete(`/subeler/${id}`) }
}

/**
 * Depo (Warehouse) API'si
 */
export const depoAPI = {
  getAll() { return apiClient.get('/depolar') },
  getById(id) { return apiClient.get(`/depolar/${id}`) },
  create(data) { return apiClient.post('/depolar', data) },
  update(id, data) { return apiClient.put(`/depolar/${id}`, data) },
  delete(id) { return apiClient.delete(`/depolar/${id}`) },
  getStoklar(id) { return apiClient.get(`/depolar/${id}/stoklar`) },
  stokEkle(id, data) { return apiClient.post(`/depolar/${id}/stok-ekle`, data) },
  stokCikar(id, data) { return apiClient.post(`/depolar/${id}/stok-cikar`, data) },
  transfer(data) { return apiClient.post('/depolar/transfer', data) }
}

export const butceAPI = {
  getAll() { return apiClient.get('/butceler') },
  getById(id) { return apiClient.get(`/butceler/${id}`) },
  create(data) { return apiClient.post('/butceler', data) },
  update(id, data) { return apiClient.put(`/butceler/${id}`, data) },
  delete(id) { return apiClient.delete(`/butceler/${id}`) }
}

export const masrafAPI = {
  getAll() { return apiClient.get('/masraflar') },
  getById(id) { return apiClient.get(`/masraflar/${id}`) },
  create(data) { return apiClient.post('/masraflar', data) },
  update(id, data) { return apiClient.put(`/masraflar/${id}`, data) },
  delete(id) { return apiClient.delete(`/masraflar/${id}`) }
}

export const fiyatListesiAPI = {
  getAll() { return apiClient.get('/fiyat-listesi') },
  getById(id) { return apiClient.get(`/fiyat-listesi/${id}`) },
  create(data) { return apiClient.post('/fiyat-listesi', data) },
  update(id, data) { return apiClient.put(`/fiyat-listesi/${id}`, data) },
  delete(id) { return apiClient.delete(`/fiyat-listesi/${id}`) }
}

export const iadeAPI = {
  getAll() { return apiClient.get('/iadeler') },
  getById(id) { return apiClient.get(`/iadeler/${id}`) },
  create(data) { return apiClient.post('/iadeler', data) },
  update(id, data) { return apiClient.put(`/iadeler/${id}`, data) },
  durumGuncelle(id, durum) { return apiClient.put(`/iadeler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/iadeler/${id}`) }
}

export const stokSeriAPI = {
  getAll() { return apiClient.get('/stok-seri') },
  getByStok(stokId) { return apiClient.get(`/stok-seri/stok/${stokId}`) },
  create(data) { return apiClient.post('/stok-seri', data) },
  update(id, data) { return apiClient.put(`/stok-seri/${id}`, data) },
  delete(id) { return apiClient.delete(`/stok-seri/${id}`) }
}

export const stokSayimAPI = {
  getAll() { return apiClient.get('/stok-sayim') },
  getById(id) { return apiClient.get(`/stok-sayim/${id}`) },
  create(data) { return apiClient.post('/stok-sayim', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/stok-sayim/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/stok-sayim/${id}`) }
}

export const maasBordroAPI = {
  getAll() { return apiClient.get('/maas-bordro') },
  getById(id) { return apiClient.get(`/maas-bordro/${id}`) },
  create(data) { return apiClient.post('/maas-bordro', data) },
  update(id, data) { return apiClient.put(`/maas-bordro/${id}`, data) },
  delete(id) { return apiClient.delete(`/maas-bordro/${id}`) }
}

export const vardiyaAPI = {
  getAll() { return apiClient.get('/vardiyalar') },
  getByPersonel(personelId) { return apiClient.get(`/vardiyalar/personel/${personelId}`) },
  create(data) { return apiClient.post('/vardiyalar', data) },
  update(id, data) { return apiClient.put(`/vardiyalar/${id}`, data) },
  delete(id) { return apiClient.delete(`/vardiyalar/${id}`) }
}

export const puantajAPI = {
  getByPersonel(personelId, baslangic, bitis) { return apiClient.get(`/personel-puantaj/personel/${personelId}`, { params: { baslangic, bitis } }) },
  create(data) { return apiClient.post('/personel-puantaj', data) },
  update(id, data) { return apiClient.put(`/personel-puantaj/${id}`, data) },
  delete(id) { return apiClient.delete(`/personel-puantaj/${id}`) }
}

export const backupAPI = {
  manual(type) { return apiClient.post(`/backups/manual?type=${type || 'DAILY'}`) },
  list() { return apiClient.get('/backups') },
  download(filename) { return apiClient.get(`/backups/download/${filename}`, { responseType: 'blob' }) },
  delete(filename) { return apiClient.delete(`/backups/${filename}`) },
  getSchedule() { return apiClient.get('/backups/schedule') }
}

export const excelAPI = {
  cariHesaplar() { return apiClient.get('/exports/cari-hesaplar', { responseType: 'blob' }) },
  faturalar() { return apiClient.get('/exports/faturalar', { responseType: 'blob' }) },
  hareketler() { return apiClient.get('/exports/hareketler', { responseType: 'blob' }) },
  stoklar() { return apiClient.get('/exports/stoklar', { responseType: 'blob' }) },
  personel() { return apiClient.get('/exports/personel', { responseType: 'blob' }) },
  bankalar() { return apiClient.get('/exports/bankalar', { responseType: 'blob' }) },
  kasalar() { return apiClient.get('/exports/kasalar', { responseType: 'blob' }) }
}

export const importAPI = {
  stok(file) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/import/stok', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  cari(file) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/import/cari', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}

export const pdfAPI = {
  fatura(id) { return apiClient.get(`/rapor/fatura/${id}`, { responseType: 'blob' }) },
  siparis(id) { return apiClient.get(`/rapor/siparis/${id}`, { responseType: 'blob' }) },
  irsaliye(id) { return apiClient.get(`/rapor/irsaliye/${id}`, { responseType: 'blob' }) }
}

export const notAPI = {
  getAll(params) { return apiClient.get('/notlar', { params }) },
  getById(id) { return apiClient.get(`/notlar/${id}`) },
  create(data) { return apiClient.post('/notlar', data) },
  update(id, data) { return apiClient.put(`/notlar/${id}`, data) },
  delete(id) { return apiClient.delete(`/notlar/${id}`) }
}

export default apiClient
