import { apiClient } from '../client.js'

// Idempotency anahtari üretir (tarayici destegi yoksa zaman damgasi + rastgele ile).
function idempotencyAnahtari() {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  } catch {
    /* yoksay */
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}

export const cariHesapAPI = {
  getAll(params) {
    return apiClient.get('/cari-hesaplar', { params })
  },
  filtreli(params) {
    return apiClient.get('/cari-hesaplar/filtreli', { params })
  },
  ozet() {
    return apiClient.get('/cari-hesaplar/ozet')
  },
  getById(id) {
    return apiClient.get(`/cari-hesaplar/${id}`)
  },
  search(query) {
    return apiClient.get('/cari-hesaplar/search', { params: { q: query } })
  },
  create(data) {
    return apiClient.post('/cari-hesaplar', data)
  },
  update(id, data) {
    return apiClient.put(`/cari-hesaplar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/cari-hesaplar/${id}`)
  },
  topluSil(ids) {
    return apiClient.delete('/cari-hesaplar/toplu-sil', { data: ids })
  },
  getFiyatlar(id) {
    return apiClient.get(`/cari-hesaplar/${id}/fiyatlar`)
  },
  fiyatKaydet(id, data) {
    return apiClient.post(`/cari-hesaplar/${id}/fiyatlar`, data)
  },
  fiyatSil(fiyatId) {
    return apiClient.delete(`/cari-hesaplar/fiyatlar/${fiyatId}`)
  },
  kart(id) {
    return apiClient.get(`/cari-hesaplar/${id}/kart`)
  }
}

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

export const faturaAPI = {
  getAll(params) {
    return apiClient.get('/faturalar', { params })
  },
  getById(id) {
    return apiClient.get(`/faturalar/${id}`)
  },
  getByNumara(faturaNumarasi) {
    return apiClient.get(`/faturalar/numara/${encodeURIComponent(faturaNumarasi)}`)
  },
  paraIzi(id) {
    return apiClient.get(`/faturalar/${id}/para-izi`)
  },
  create(data) {
    // Idempotency anahtari: ag tekrarinda/çift tıklamada mükerrer fatura oluşmasını engeller.
    return apiClient.post('/faturalar', data, {
      headers: { 'X-Idempotency-Key': idempotencyAnahtari() }
    })
  },
  update(id, data) {
    return apiClient.put(`/faturalar/${id}`, data)
  },
  updateDurum(id, durum) {
    return apiClient.put(`/faturalar/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/faturalar/${id}`)
  },
  gonderEmail(id) {
    return apiClient.post(`/faturalar/${id}/gonder-email`)
  },
  cariSonUrunler(cariId, limit = 10) {
    return apiClient.get(`/faturalar/cari/${cariId}/son-urunler`, { params: { limit } })
  },
  cariSonFatura(cariId) {
    return apiClient.get(`/faturalar/cari/${cariId}/son-fatura`)
  },
  cariUrunFiyatGecmisi(cariId, stokId) {
    return apiClient.get(`/faturalar/cari/${cariId}/stok/${stokId}/fiyat-gecmisi`)
  },
  cariFaturalari(cariId, params) {
    return apiClient.get(`/faturalar/cari/${cariId}`, { params })
  },
  stokFiyatGecmisi(stokId) {
    return apiClient.get(`/faturalar/stok/${stokId}/fiyat-gecmisi`)
  },
  gecmis(id) {
    return apiClient.get(`/faturalar/${id}/gecmis`)
  },
  yazdirmaKaydet(id, data) {
    return apiClient.post(`/faturalar/${id}/yazdirma`, data || {})
  }
}

export const eFaturaAPI = {
  getTumu(params) {
    return apiClient.get('/e-fatura', { params })
  },
  getById(id) {
    return apiClient.get(`/e-fatura/${id}`)
  },
  olustur(faturaId, senaryo = 'TEMELFATURA', tip = 'SATIS') {
    return apiClient.post(`/e-fatura/olustur/${faturaId}`, null, { params: { senaryo, tip } })
  },
  gibGonder(id) {
    return apiClient.post(`/e-fatura/${id}/gib-gonder`)
  },
  durumSorgula(id) {
    return apiClient.post(`/e-fatura/${id}/durum-sorgula`)
  },
  xmlIndir(id) {
    return apiClient.get(`/e-fatura/${id}/xml`, { responseType: 'blob' })
  }
}

export const siparisAPI = {
  getAll(params) {
    return apiClient.get('/siparisler', { params })
  },
  getById(id) {
    return apiClient.get(`/siparisler/${id}`)
  },
  create(data) {
    return apiClient.post('/siparisler', data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/siparisler/${id}/durum`, { durum })
  },
  isEmriOlustur(id, data) {
    return apiClient.post(`/siparisler/${id}/is-emri`, data)
  },
  soforAta(id, driverId) {
    return apiClient.post(`/siparisler/${id}/sofor-ata`, { driverId })
  },
  delete(id) {
    return apiClient.delete(`/siparisler/${id}`)
  }
}

export const irsaliyeAPI = {
  getAll(params) {
    return apiClient.get('/irsaliyeler', { params })
  },
  getById(id) {
    return apiClient.get(`/irsaliyeler/${id}`)
  },
  create(data) {
    return apiClient.post('/irsaliyeler', data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/irsaliyeler/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/irsaliyeler/${id}`)
  }
}

export const siparisTakipAPI = {
  zincir() {
    return apiClient.get('/siparis-takip')
  }
}

export const iadeAPI = {
  getAll() {
    return apiClient.get('/iadeler')
  },
  getById(id) {
    return apiClient.get(`/iadeler/${id}`)
  },
  create(data) {
    return apiClient.post('/iadeler', data)
  },
  update(id, data) {
    return apiClient.put(`/iadeler/${id}`, data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/iadeler/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/iadeler/${id}`)
  }
}

export const fiyatListesiAPI = {
  getAll() {
    return apiClient.get('/fiyat-listesi')
  },
  getById(id) {
    return apiClient.get(`/fiyat-listesi/${id}`)
  },
  create(data) {
    return apiClient.post('/fiyat-listesi', data)
  },
  update(id, data) {
    return apiClient.put(`/fiyat-listesi/${id}`, data)
  },
  delete(id) {
      return apiClient.delete(`/fiyat-listesi/${id}`)
    }
  }

  export const iskontoKuraliAPI = {
    getAll(params) {
      return apiClient.get('/iskonto-kurallari', { params })
    },
    getById(id) {
      return apiClient.get(`/iskonto-kurallari/${id}`)
    },
    create(data) {
      return apiClient.post('/iskonto-kurallari', data)
    },
    update(id, data) {
      return apiClient.put(`/iskonto-kurallari/${id}`, data)
    },
    delete(id) {
      return apiClient.delete(`/iskonto-kurallari/${id}`)
    },
    hesapla(params) {
      return apiClient.get('/iskonto-kurallari/hesapla', { params })
    }
  }

export const crmAPI = {
  getFirsatlar(params) {
    return apiClient.get('/crm/firsatlar', { params })
  },
  firsatOlustur(data) {
    return apiClient.post('/crm/firsatlar', data)
  },
  firsatGuncelle(id, data) {
    return apiClient.put(`/crm/firsatlar/${id}`, data)
  },
  firsatSil(id) {
      return apiClient.delete(`/crm/firsatlar/${id}`)
    },
    // Lead
    getLeadler(params) {
      return apiClient.get('/crm/leadler', { params })
    },
    leadOlustur(data) {
      return apiClient.post('/crm/leadler', data)
    },
    leadGuncelle(id, data) {
      return apiClient.put(`/crm/leadler/${id}`, data)
    },
    leadDonustur(id, cariHesapId) {
      return apiClient.put(`/crm/leadler/${id}/donustur`, null, { params: { cariHesapId } })
    },
    leadSil(id) {
      return apiClient.delete(`/crm/leadler/${id}`)
    },
    // Aktivite
    getAktiviteler(params) {
      return apiClient.get('/crm/aktiviteler', { params })
    },
    aktiviteOlustur(data) {
      return apiClient.post('/crm/aktiviteler', data)
    },
    aktiviteTamamla(id, tamamlandi = true) {
      return apiClient.put(`/crm/aktiviteler/${id}/tamamla`, null, { params: { tamamlandi } })
    },
    aktiviteSil(id) {
      return apiClient.delete(`/crm/aktiviteler/${id}`)
    },
    // Kampanya
    getKampanyalar(params) {
      return apiClient.get('/crm/kampanyalar', { params })
    },
    kampanyaOlustur(data) {
      return apiClient.post('/crm/kampanyalar', data)
    },
    kampanyaGuncelle(id, data) {
      return apiClient.put(`/crm/kampanyalar/${id}`, data)
    },
    kampanyaSil(id) {
      return apiClient.delete(`/crm/kampanyalar/${id}`)
    }
}

export const satinalmaTalepAPI = {
  getAll(params) {
    return apiClient.get('/satinalma-talepler', { params })
  },
  getById(id) {
    return apiClient.get(`/satinalma-talepler/${id}`)
  },
  create(data) {
    return apiClient.post('/satinalma-talepler', data)
  },
  update(id, data) {
    return apiClient.put(`/satinalma-talepler/${id}`, data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/satinalma-talepler/${id}/durum`, { durum })
  },
  delete(id) {
    return apiClient.delete(`/satinalma-talepler/${id}`)
  }
}

export const satinalmaSiparisAPI = {
  getAll(params) {
    return apiClient.get('/satinalma-siparisler', { params })
  },
  getById(id) {
    return apiClient.get(`/satinalma-siparisler/${id}`)
  },
  create(data) {
    return apiClient.post('/satinalma-siparisler', data)
  },
  update(id, data) {
    return apiClient.put(`/satinalma-siparisler/${id}`, data)
  },
  durumGuncelle(id, durum) {
    return apiClient.put(`/satinalma-siparisler/${id}/durum`, { durum })
  },
  faturayaCevir(id) {
    return apiClient.post(`/satinalma-siparisler/${id}/faturaya-cevir`)
  },
  delete(id) {
    return apiClient.delete(`/satinalma-siparisler/${id}`)
  }
}

export const teklifAPI = {
  getAll(params) {
    return apiClient.get('/teklifler', { params })
  },
  getById(id) {
    return apiClient.get(`/teklifler/${id}`)
  },
  create(data) {
    return apiClient.post('/teklifler', data)
  },
  update(id, data) {
    return apiClient.put(`/teklifler/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/teklifler/${id}`)
  },
  durumGuncelle(id, durum) {
    return apiClient.patch(`/teklifler/${id}/durum`, { durum })
  },
  revizyonOlustur(id) {
    return apiClient.post(`/teklifler/${id}/revizyon`)
  },
  sipariseDonustur(id) {
    return apiClient.post(`/teklifler/${id}/siparise-donustur`)
  },
  faturayaDonustur(id) {
    return apiClient.post(`/teklifler/${id}/faturaya-donustur`)
  }
}

export const tekrarlayanFaturaAPI = {
  getAll() {
    return apiClient.get('/tekrarlayan-faturalar')
  },
  getById(id) {
    return apiClient.get(`/tekrarlayan-faturalar/${id}`)
  },
  create(data) {
    return apiClient.post('/tekrarlayan-faturalar', data)
  },
  update(id, data) {
    return apiClient.put(`/tekrarlayan-faturalar/${id}`, data)
  },
  delete(id) {
    return apiClient.delete(`/tekrarlayan-faturalar/${id}`)
  },
  uret(id) {
    return apiClient.post(`/tekrarlayan-faturalar/${id}/uret`)
  }
}

export const teslimatAPI = {
  suruculer() {
    return apiClient.get('/drivers')
  },
  byDriver() {
    return apiClient.get('/deliveries/by-driver')
  },
  teslimatlar(driverId) {
    return apiClient.get('/deliveries', { params: { driverId } })
  },
  olustur(data) {
    return apiClient.post('/deliveries', data)
  },
  durumGuncelle(id, durum) {
    return apiClient.patch(`/deliveries/${id}/status`, { durum })
  },
  fotoYukle(id, file) {
    const form = new FormData()
    form.append('file', file)
    return apiClient.post(`/deliveries/${id}/foto`, form)
  },
  gecmis(id) {
    return apiClient.get(`/deliveries/${id}/gecmis`)
  },
  getById(id) {
    return apiClient.get(`/deliveries/${id}`)
  },
  teslimEt(id, data, imzaFile) {
    const form = new FormData()
    if (data?.teslimAlanAd != null) form.append('teslimAlanAd', data.teslimAlanAd)
    if (data?.teslimNotu != null) form.append('teslimNotu', data.teslimNotu)
    if (data?.teslimKonum != null) form.append('teslimKonum', data.teslimKonum)
    if (imzaFile) form.append('file', imzaFile)
    return apiClient.post(`/deliveries/${id}/teslim`, form)
  },
  teslimEtSiparis(siparisId, data, imzaFile) {
    const form = new FormData()
    if (data?.teslimAlanAd != null) form.append('teslimAlanAd', data.teslimAlanAd)
    if (data?.teslimNotu != null) form.append('teslimNotu', data.teslimNotu)
    if (data?.teslimKonum != null) form.append('teslimKonum', data.teslimKonum)
    if (imzaFile) form.append('file', imzaFile)
    return apiClient.post(`/deliveries/siparis/${siparisId}/teslim`, form)
  },
  fis(id) {
    return apiClient.get(`/deliveries/${id}/fis`, { responseType: 'blob' })
  }
}

