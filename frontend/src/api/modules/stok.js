import { apiClient } from '../client.js'

export const stokAPI = {
  getAll() {
    return apiClient.get('/stoklar')
  },
  ara(q) {
    return apiClient.get('/stoklar/ara', { params: { q } })
  },
  filtreli(params) {
    return apiClient.get('/stoklar/filtreli', { params })
  },
  enCokSatanlar(limit = 12) {
    return apiClient.get('/stoklar/en-cok-satanlar', { params: { limit } })
  },
  kritik() {
    return apiClient.get('/stoklar/kritik')
  },
  talepTahmini() {
    return apiClient.get('/stoklar/talep-tahmini')
  },
  getById(id) {
    return apiClient.get(`/stoklar/${id}`)
  },
  create(data) {
    return apiClient.post('/stoklar', data)
  },
  update(id, data) {
    return apiClient.put(`/stoklar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/stoklar/${id}`)
  },
  getHareketler(id) {
    return apiClient.get(`/stoklar/${id}/hareketler`)
  },
  getFiyatlar(id) {
    return apiClient.get(`/stoklar/${id}/fiyatlar`)
  },
  fiyatEkle(id, data) {
    return apiClient.post(`/stoklar/${id}/fiyatlar`, data)
  },
  fiyatGuncelle(fiyatId, data) {
    return apiClient.put(`/stoklar/fiyatlar/${fiyatId}`, data)
  },
  fiyatSil(fiyatId) {
    return apiClient.delete(`/stoklar/fiyatlar/${fiyatId}`)
  },
  tumHareketler() {
    return apiClient.get('/stoklar/hareketler/tum')
  },
  addHareket(id, data) {
    return apiClient.post(`/stoklar/${id}/hareketler`, data)
  },
  deleteHareket(hareketId) {
    return apiClient.delete(`/stoklar/hareketler/${hareketId}`)
  },
  topluFiyatGuncelle(data) {
    return apiClient.post('/stoklar/toplu-fiyat-guncelle', data)
  }
}

export const depoAPI = {
  getAll() {
    return apiClient.get('/depolar')
  },
  getById(id) {
    return apiClient.get(`/depolar/${id}`)
  },
  create(data) {
    return apiClient.post('/depolar', data)
  },
  update(id, data) {
    return apiClient.put(`/depolar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/depolar/${id}`)
  },
  getStoklar(id) {
    return apiClient.get(`/depolar/${id}/stoklar`)
  },
  stokEkle(id, data) {
    return apiClient.post(`/depolar/${id}/stok-ekle`, data)
  },
  stokCikar(id, data) {
    return apiClient.post(`/depolar/${id}/stok-cikar`, data)
  },
  transfer(data) {
    return apiClient.post('/depolar/transfer', data)
  }
}

export const depoTransferAPI = {
  getAll() {
    return apiClient.get('/depo-transferler')
  },
  bekleyenler() {
    return apiClient.get('/depo-transferler/bekleyenler')
  },
  talepOlustur(data) {
    return apiClient.post('/depo-transferler', data)
  },
  onayla(id) {
    return apiClient.post(`/depo-transferler/${id}/onayla`)
  },
  reddet(id) {
    return apiClient.post(`/depo-transferler/${id}/reddet`)
  }
}

export const kategoriAPI = {
  getAll() {
    return apiClient.get('/kategoriler')
  },
  getByTur(tur) {
    return apiClient.get(`/kategoriler/tur/${tur}`)
  },
  create(data) {
    return apiClient.post('/kategoriler', data)
  },
  delete(id) {
    return apiClient.delete(`/kategoriler/${id}`)
  }
}

export const stokSeriAPI = {
  getAll() {
    return apiClient.get('/stok-seri')
  },
  getByStok(stokId) {
    return apiClient.get(`/stok-seri/stok/${stokId}`)
  },
  create(data) {
    return apiClient.post('/stok-seri', data)
  },
  update(id, data) {
    return apiClient.put(`/stok-seri/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/stok-seri/${id}`)
  }
}

export const stokSayimAPI = {
  getAll() {
    return apiClient.get('/stok-sayim')
  },
  getById(id) {
    return apiClient.get(`/stok-sayim/${id}`)
  },
  create(data) {
    return apiClient.post('/stok-sayim', data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/stok-sayim/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/stok-sayim/${id}`)
  }
}
