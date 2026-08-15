import { apiClient } from '../client.js'

export const dashboardAPI = {
  getData() { return apiClient.get('/dashboard') }
}

export const raporAPI = {
  cariEkstre(params) { return apiClient.get('/raporlar/cari-ekstre', { params }) },
  gelirGider(params) { return apiClient.get('/raporlar/gelir-gider', { params }) },
  kdv(params) { return apiClient.get('/raporlar/kdv', { params }) },
  yaslandirma() { return apiClient.get('/raporlar/yaslandirma') },
  kdvBeyanname(donem) { return apiClient.get('/raporlar/kdv-beyanname', { params: { donem } }) },
  baBs(params) { return apiClient.get('/raporlar/ba-bs', { params }) },
  cariKarlilik(params) { return apiClient.get('/raporlar/cari-karlilik', { params }) },
  tedarikciUrunler() { return apiClient.get('/raporlar/tedarikci-urunler') },
  urunKarlilik() { return apiClient.get('/raporlar/urun-karlilik') }
}
