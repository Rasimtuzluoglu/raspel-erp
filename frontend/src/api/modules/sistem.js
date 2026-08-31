import { apiClient } from '../client.js'

export const kullaniciAPI = {
  getAll() {
    return apiClient.get('/kullanicilar')
  },
  getById(id) {
    return apiClient.get(`/kullanicilar/${id}`)
  },
  create(data) {
    return apiClient.post('/kullanicilar', data)
  },
  update(id, data) {
    return apiClient.put(`/kullanicilar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/kullanicilar/${id}`)
  },
  giris(data) {
    return apiClient.post('/kullanicilar/giris', data)
  },
  girisSirket(data) {
    return apiClient.post('/kullanicilar/giris-sirket', data)
  },
  giris2fa(data) {
    return apiClient.post('/kullanicilar/giris-2fa', data)
  },
  sifreDegistir(data) {
    return apiClient.put('/kullanicilar/sifre-degistir', data)
  },
  ben() {
    return apiClient.get('/kullanicilar/ben')
  },
  beniGuncelle(data) {
    return apiClient.put('/kullanicilar/ben', data)
  },
  setup2fa() {
    return apiClient.post('/kullanicilar/setup-2fa')
  },
  enable2fa(data) {
    return apiClient.post('/kullanicilar/enable-2fa', data)
  },
  disable2fa(data) {
    return apiClient.post('/kullanicilar/disable-2fa', data)
  },
  aktifOturumlar() {
    return apiClient.get('/kullanicilar/aktif-oturumlar')
  },
  oturumIptal(jti) {
    return apiClient.delete(`/kullanicilar/oturum/${jti}`)
  },
  bildirimTercihleriGetir() {
    return apiClient.get('/kullanicilar/bildirim-tercihleri')
  },
  bildirimTercihleriGuncelle(tercihler) {
    return apiClient.put('/kullanicilar/bildirim-tercihleri', tercihler)
  }
}

export const kurulumAPI = {
  durum() {
    return apiClient.get('/kurulum/durum')
  },
  baslat(data) {
    return apiClient.post('/kurulum/baslat', data)
  }
}

export const sohbetAPI = {
  sonMesajlar() {
    return apiClient.get('/sohbet')
  },
  gonder(data) {
    return apiClient.post('/sohbet', data)
  },
  aiSorgu(soru) {
    return apiClient.post('/sohbet/ai-sorgu', { soru })
  }
}

export const ajandaAPI = {
  olaylar(params) {
    return apiClient.get('/ajanda', { params })
  }
}

export const bildirimAPI = {
  liste() {
    return apiClient.get('/bildirimler')
  },
  okunmamis() {
    return apiClient.get('/bildirimler/okunmamis')
  },
  okundu(id) {
    return apiClient.put(`/bildirimler/${id}/okundu`)
  },
  tumuOkundu() {
    return apiClient.put('/bildirimler/tumu-okundu')
  }
}

export const sistemDurumAPI = {
  durum() {
    return apiClient.get('/sistem/durum')
  },
  hataLog() {
    return apiClient.get('/sistem/hata-log')
  }
}

export const apiTokenAPI = {
  listele() {
    return apiClient.get('/api-tokenlar')
  },
  olustur(ad) {
    return apiClient.post('/api-tokenlar', { ad })
  },
  sil(id) {
    return apiClient.delete(`/api-tokenlar/${id}`)
  }
}

export const churnAPI = {
  analiz() {
    return apiClient.get('/churn')
  }
}

export const onayAyariAPI = {
  listele() {
    return apiClient.get('/onay-ayarlari')
  },
  kaydet(data) {
    return apiClient.post('/onay-ayarlari', data)
  }
}

export const sirketAPI = {
  getAll() {
    return apiClient.get('/sirketler')
  },
  getAktif() {
    return apiClient.get('/sirketler/aktif')
  },
  getById(id) {
    return apiClient.get(`/sirketler/${id}`)
  },
  create(data) {
    return apiClient.post('/sirketler', data)
  },
  update(id, data) {
    return apiClient.put(`/sirketler/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/sirketler/${id}`)
  },
  getKonsolideOzet(id) {
    return apiClient.get(`/sirketler/${id}/konsolide-ozet`)
  }
}

export const subeAPI = {
  getAll() {
    return apiClient.get('/subeler')
  },
  getAktif() {
    return apiClient.get('/subeler/aktif')
  },
  getById(id) {
    return apiClient.get(`/subeler/${id}`)
  },
  create(data) {
    return apiClient.post('/subeler', data)
  },
  update(id, data) {
    return apiClient.put(`/subeler/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/subeler/${id}`)
  }
}

export const donemAPI = {
  getAll() {
    return apiClient.get('/donemler')
  },
  getBySirket(sirketId) {
    return apiClient.get(`/donemler/sirket/${sirketId}`)
  },
  getAktif(sirketId) {
    return apiClient.get(`/donemler/sirket/${sirketId}/aktif`)
  },
  getById(id) {
    return apiClient.get(`/donemler/${id}`)
  },
  create(data) {
    return apiClient.post('/donemler', data)
  },
  update(id, data) {
    return apiClient.put(`/donemler/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/donemler/${id}`)
  }
}

export const auditLogAPI = {
  getAll(params) {
    return apiClient.get('/audit-log', { params })
  },
  getIslemTipleri() {
    return apiClient.get('/audit-log/islem-tipleri')
  },
  getEntityListesi() {
    return apiClient.get('/audit-log/entity-listesi')
  }
}

export const notAPI = {
  getAll(params) {
    return apiClient.get('/notlar', { params })
  },
  getById(id) {
    return apiClient.get(`/notlar/${id}`)
  },
  cariNotlari(cariHesapId) {
    return apiClient.get(`/notlar/cari/${cariHesapId}`)
  },
  create(data) {
    return apiClient.post('/notlar', data)
  },
  update(id, data) {
    return apiClient.put(`/notlar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/notlar/${id}`)
  }
}

export const projeAPI = {
  getAll(params) {
    return apiClient.get('/projeler', { params })
  },
  getById(id) {
    return apiClient.get(`/projeler/${id}`)
  },
  create(data) {
    return apiClient.post('/projeler', data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/projeler/${id}/durum`, { durum })
  },
  gorevEkle(projeId, data) {
    return apiClient.post(`/projeler/${projeId}/gorevler`, data)
  },
  gorevDurumGuncelle(gorevId, durum) {
    return apiClient.put(`/projeler/gorev/${gorevId}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/projeler/${id}`)
  }
}

export const anomaliAPI = {
  tara() {
    return apiClient.get('/anomaliler')
  },
  getIpWhitelist() {
    return apiClient.get('/anomaliler/ip-whitelist')
  },
  addIpWhitelist(data) {
    return apiClient.post('/anomaliler/ip-whitelist', data)
  },
  deleteIpWhitelist(id) {
    return apiClient.delete(`/anomaliler/ip-whitelist/${id}`)
  }
}

export const aiConfigAPI = {
  getConfig() {
    return apiClient.get('/ai-config')
  },
  saveConfig(data) {
    return apiClient.post('/ai-config', data)
  },
  testConnection() {
    return apiClient.post('/ai-config/test')
  },
  deleteConfig() {
    return apiClient.delete('/ai-config')
  }
}

