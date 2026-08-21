import { apiClient } from '../client.js'

export const personelAPI = {
  getAll(params) {
    return apiClient.get('/personel', { params })
  },
  getById(id) {
    return apiClient.get(`/personel/${id}`)
  },
  create(data) {
    return apiClient.post('/personel', data)
  },
  update(id, data) {
    return apiClient.put(`/personel/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/personel/${id}`)
  }
}

export const personelIzinAPI = {
  getAll() {
    return apiClient.get('/personel-izin')
  },
  getByPersonel(personelId) {
    return apiClient.get(`/personel-izin/personel/${personelId}`)
  },
  create(data) {
    return apiClient.post('/personel-izin', data)
  },
  durumGuncelle(id, durum, onaylayan) {
    return apiClient.put(`/personel-izin/${id}/durum`, { durum, onaylayan })
  },
  delete(id) {
    return apiClient.delete(`/personel-izin/${id}`)
  }
}

export const puantajAPI = {
  getByPersonel(personelId, baslangic, bitis) {
    return apiClient.get(`/personel-puantaj/personel/${personelId}`, { params: { baslangic, bitis } })
  },
  create(data) {
    return apiClient.post('/personel-puantaj', data)
  },
  update(id, data) {
    return apiClient.put(`/personel-puantaj/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/personel-puantaj/${id}`)
  }
}

export const maasBordroAPI = {
  getAll() {
    return apiClient.get('/maas-bordro')
  },
  getById(id) {
    return apiClient.get(`/maas-bordro/${id}`)
  },
  create(data) {
    return apiClient.post('/maas-bordro', data)
  },
  update(id, data) {
    return apiClient.put(`/maas-bordro/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/maas-bordro/${id}`)
  }
}

export const vardiyaAPI = {
  getAll() {
    return apiClient.get('/vardiyalar')
  },
  getByPersonel(personelId) {
    return apiClient.get(`/vardiyalar/personel/${personelId}`)
  },
  create(data) {
    return apiClient.post('/vardiyalar', data)
  },
  update(id, data) {
    return apiClient.put(`/vardiyalar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/vardiyalar/${id}`)
  }
}

export const personelMasrafTalepAPI = {
  getAll(params) {
    return apiClient.get('/personel-masraf-talepler', { params })
  },
  getBekleyenler() {
    return apiClient.get('/personel-masraf-talepler/bekleyenler')
  },
  getKullaniciTalepleri() {
    return apiClient.get('/personel-masraf-talepler/kullanici-talepleri')
  },
  create(data) {
    return apiClient.post('/personel-masraf-talepler', data)
  },
  onayla(id, onayNotu = '') {
    return apiClient.patch(`/personel-masraf-talepler/${id}/onayla`, { onayNotu })
  },
  reddet(id, onayNotu = '') {
    return apiClient.patch(`/personel-masraf-talepler/${id}/reddet`, { onayNotu })
  },
  delete(id) {
    return apiClient.delete(`/personel-masraf-talepler/${id}`)
  }
}

