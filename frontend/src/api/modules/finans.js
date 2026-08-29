import { apiClient } from '../client.js'

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

export const bankaMutabakatAPI = {
  listele(bankaId) {
    return apiClient.get(`/bankalar/${bankaId}/mutabakat`)
  },
  yukle(bankaId, dosya) {
    const formData = new FormData()
    formData.append('dosya', dosya)
    return apiClient.post(`/bankalar/${bankaId}/mutabakat/yukle`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },
  otomatikEslestir(bankaId) {
    return apiClient.post(`/bankalar/${bankaId}/mutabakat/otomatik-eslestir`)
  },
  eslestir(bankaId, hareketId, faturaId) {
    return apiClient.post(`/bankalar/${bankaId}/mutabakat/${hareketId}/eslestir/${faturaId}`)
  },
  eslestirmeyiKaldir(bankaId, hareketId) {
    return apiClient.post(`/bankalar/${bankaId}/mutabakat/${hareketId}/eslestirmeyi-kaldir`)
  },
  sil(bankaId) {
    return apiClient.delete(`/bankalar/${bankaId}/mutabakat`)
  }
}

export const kasaAPI = {
  getAll() {
    return apiClient.get('/kasalar')
  },
  getById(id) {
    return apiClient.get(`/kasalar/${id}`)
  },
  create(data) {
    return apiClient.post('/kasalar', data)
  },
  update(id, data) {
    return apiClient.put(`/kasalar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/kasalar/${id}`)
  },
  getHareketler(id) {
    return apiClient.get(`/kasalar/${id}/hareketler`)
  },
  addHareket(id, data) {
    return apiClient.post(`/kasalar/${id}/hareketler`, data)
  },
  deleteHareket(hareketId) {
    return apiClient.delete(`/kasalar/hareketler/${hareketId}`)
  }
}

export const butceAPI = {
  getAll() {
    return apiClient.get('/butceler')
  },
  getById(id) {
    return apiClient.get(`/butceler/${id}`)
  },
  create(data) {
    return apiClient.post('/butceler', data)
  },
  update(id, data) {
    return apiClient.put(`/butceler/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/butceler/${id}`)
  }
}

export const masrafAPI = {
  getAll() {
    return apiClient.get('/masraflar')
  },
  getById(id) {
    return apiClient.get(`/masraflar/${id}`)
  },
  create(data) {
    return apiClient.post('/masraflar', data)
  },
  update(id, data) {
    return apiClient.put(`/masraflar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/masraflar/${id}`)
  }
}

export const cekSenetAPI = {
  getAll(params) {
    return apiClient.get('/cek-senet', { params })
  },
  getById(id) {
    return apiClient.get(`/cek-senet/${id}`)
  },
  create(data) {
    return apiClient.post('/cek-senet', data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/cek-senet/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/cek-senet/${id}`)
  }
}

export const dovizAPI = {
  getKurlar() {
    return apiClient.get('/doviz/kurlar')
  },
  guncelle() {
    return apiClient.post('/doviz/guncelle')
  },
  cevir(tutar, kaynak = 'TRY', hedef = 'USD') {
    return apiClient.get('/doviz/cevir', { params: { tutar, kaynak, hedef } })
  }
}

export const tahsilatAPI = {
  ozet() {
    return apiClient.get('/tahsilat')
  },
  hatirlat(cariId) {
    return apiClient.post(`/tahsilat/${cariId}/hatirlat`)
  }
}

export const muhasebeAPI = {
  getHesapPlani() {
    return apiClient.get('/muhasebe/hesap-plani')
  },
  hesapOlustur(data) {
    return apiClient.post('/muhasebe/hesap-plani', data)
  },
  hesapGuncelle(id, data) {
    return apiClient.put(`/muhasebe/hesap-plani/${id}`, data)
  },
  hesapSil(id) {
    return apiClient.delete(`/muhasebe/hesap-plani/${id}`)
  },
  getFisler(params) {
    return apiClient.get('/muhasebe/fisler', { params })
  },
  getFis(id) {
    return apiClient.get(`/muhasebe/fisler/${id}`)
  },
  fisOlustur(data) {
    return apiClient.post('/muhasebe/fisler', data)
  },
  fisIptal(id) {
    return apiClient.post(`/muhasebe/fisler/${id}/iptal`)
  },
  fisSil(id) {
    return apiClient.delete(`/muhasebe/fisler/${id}`)
  },
  getMizan(params) {
    return apiClient.get('/muhasebe/mizan', { params })
  },
  getDefteriKebir(params) {
    return apiClient.get('/muhasebe/defteri-kebir', { params })
  },
  getBilanco() {
    return apiClient.get('/muhasebe/bilanco')
  },
  getKarZarar(params) {
    return apiClient.get('/muhasebe/kar-zarar', { params })
  }
}
