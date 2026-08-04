import { apiClient } from '../client.js'

export const cariHesapAPI = {
  getAll(params) { return apiClient.get('/cari-hesaplar', { params }) },
  getById(id) { return apiClient.get(`/cari-hesaplar/${id}`) },
  search(query) { return apiClient.get('/cari-hesaplar/search', { params: { q: query } }) },
  create(data) { return apiClient.post('/cari-hesaplar', data) },
  update(id, data) { return apiClient.put(`/cari-hesaplar/${id}`, data) },
  delete(id) { return apiClient.delete(`/cari-hesaplar/${id}`) }
}

export const hareketAPI = {
  getByCariHesap(cariHesapId) { return apiClient.get(`/hareketler/cari/${cariHesapId}`) },
  getSon(limit = 5) { return apiClient.get(`/hareketler/son/${limit}`) },
  getAll() { return apiClient.get('/hareketler') },
  filtrele(params) { return apiClient.get('/hareketler', { params }) },
  create(data) { return apiClient.post('/hareketler', data) },
  update(id, data) { return apiClient.put(`/hareketler/${id}`, data) },
  delete(id) { return apiClient.delete(`/hareketler/${id}`) }
}

export const faturaAPI = {
  getAll() { return apiClient.get('/faturalar') },
  getById(id) { return apiClient.get(`/faturalar/${id}`) },
  create(data) { return apiClient.post('/faturalar', data) },
  update(id, data) { return apiClient.put(`/faturalar/${id}`, data) },
  updateDurum(id, durum) { return apiClient.put(`/faturalar/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/faturalar/${id}`) },
  gonderEmail(id) { return apiClient.post(`/faturalar/${id}/gonder-email`) }
}

export const eFaturaAPI = {
  getTumu(params) { return apiClient.get('/e-fatura', { params }) },
  getById(id) { return apiClient.get(`/e-fatura/${id}`) },
  olustur(faturaId, senaryo = 'TEMELFATURA', tip = 'SATIS') { return apiClient.post(`/e-fatura/olustur/${faturaId}`, null, { params: { senaryo, tip } }) },
  gibGonder(id) { return apiClient.post(`/e-fatura/${id}/gib-gonder`) },
  xmlIndir(id) { return apiClient.get(`/e-fatura/${id}/xml`, { responseType: 'blob' }) }
}

export const siparisAPI = {
  getAll(params) { return apiClient.get('/siparisler', { params }) },
  getById(id) { return apiClient.get(`/siparisler/${id}`) },
  create(data) { return apiClient.post('/siparisler', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/siparisler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/siparisler/${id}`) }
}

export const irsaliyeAPI = {
  getAll(params) { return apiClient.get('/irsaliyeler', { params }) },
  getById(id) { return apiClient.get(`/irsaliyeler/${id}`) },
  create(data) { return apiClient.post('/irsaliyeler', data) },
  durumGuncelle(id, durum) { return apiClient.put(`/irsaliyeler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/irsaliyeler/${id}`) }
}

export const iadeAPI = {
  getAll() { return apiClient.get('/iadeler') },
  getById(id) { return apiClient.get(`/iadeler/${id}`) },
  create(data) { return apiClient.post('/iadeler', data) },
  update(id, data) { return apiClient.put(`/iadeler/${id}`, data) },
  durumGuncelle(id, durum) { return apiClient.put(`/iadeler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/iadeler/${id}`) }
}

export const fiyatListesiAPI = {
  getAll() { return apiClient.get('/fiyat-listesi') },
  getById(id) { return apiClient.get(`/fiyat-listesi/${id}`) },
  create(data) { return apiClient.post('/fiyat-listesi', data) },
  update(id, data) { return apiClient.put(`/fiyat-listesi/${id}`, data) },
  delete(id) { return apiClient.delete(`/fiyat-listesi/${id}`) }
}

export const crmAPI = {
  getFirsatlar(params) { return apiClient.get('/crm/firsatlar', { params }) },
  getFirsat(id) { return apiClient.get(`/crm/firsatlar/${id}`) },
  firsatOlustur(data) { return apiClient.post('/crm/firsatlar', data) },
  firsatGuncelle(id, data) { return apiClient.put(`/crm/firsatlar/${id}`, data) },
  firsatSil(id) { return apiClient.delete(`/crm/firsatlar/${id}`) }
}

export const satinalmaTalepAPI = {
  getAll(params) { return apiClient.get('/satinalma-talepler', { params }) },
  getById(id) { return apiClient.get(`/satinalma-talepler/${id}`) },
  create(data) { return apiClient.post('/satinalma-talepler', data) },
  update(id, data) { return apiClient.put(`/satinalma-talepler/${id}`, data) },
  durumGuncelle(id, durum) { return apiClient.put(`/satinalma-talepler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/satinalma-talepler/${id}`) }
}

export const satinalmaSiparisAPI = {
  getAll(params) { return apiClient.get('/satinalma-siparisler', { params }) },
  getById(id) { return apiClient.get(`/satinalma-siparisler/${id}`) },
  create(data) { return apiClient.post('/satinalma-siparisler', data) },
  update(id, data) { return apiClient.put(`/satinalma-siparisler/${id}`, data) },
  durumGuncelle(id, durum) { return apiClient.put(`/satinalma-siparisler/${id}/durum`, { durum }) },
  delete(id) { return apiClient.delete(`/satinalma-siparisler/${id}`) }
}
