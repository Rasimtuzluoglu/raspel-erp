import { apiClient } from '../client.js'

export const uploadAPI = {
  uploadSirketLogo(file) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/upload/sirket-logo', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
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
  kasalar() { return apiClient.get('/exports/kasalar', { responseType: 'blob' }) },
  denetimLog(params) { return apiClient.get('/exports/denetim-log', { params, responseType: 'blob' }) }
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
  },
  alisFatura(file) {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post('/import/alis-fatura', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}

export const pdfAPI = {
  fatura(id) { return apiClient.get(`/rapor/fatura/${id}`, { responseType: 'blob' }) },
  siparis(id) { return apiClient.get(`/rapor/siparis/${id}`, { responseType: 'blob' }) },
  irsaliye(id) { return apiClient.get(`/rapor/irsaliye/${id}`, { responseType: 'blob' }) }
}

export const belgeAPI = {
  yukle(entityAdi, entityId, file) {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('entityAdi', entityAdi)
    formData.append('entityId', entityId)
    return apiClient.post('/belgeler/yukle', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
  },
  kayitBelgeleri(entityAdi, entityId) { return apiClient.get(`/belgeler/kayit/${entityAdi}/${entityId}`) },
  indir(filename) { return apiClient.get(`/belgeler/indir/${filename}`, { responseType: 'blob' }) },
  sil(id) { return apiClient.delete(`/belgeler/${id}`) }
}
