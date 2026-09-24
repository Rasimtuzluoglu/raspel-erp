import { apiClient } from '../client.js'

export const dashboardAPI = {
  getData() {
    return apiClient.get('/dashboard')
  }
}

export const raporAPI = {
  cariEkstre(params) {
    return apiClient.get('/raporlar/cari-ekstre', { params })
  },
  gelirGider(params) {
    return apiClient.get('/raporlar/gelir-gider', { params })
  },
  kdv(params) {
    return apiClient.get('/raporlar/kdv', { params })
  },
  yaslandirma() {
    return apiClient.get('/raporlar/yaslandirma')
  },
  kdvBeyanname(donem) {
    return apiClient.get('/raporlar/kdv-beyanname', { params: { donem } })
  },
  baBs(params) {
    return apiClient.get('/raporlar/ba-bs', { params })
  },
  cariKarlilik(params) {
    return apiClient.get('/raporlar/cari-karlilik', { params })
  },
  tedarikciUrunler() {
    return apiClient.get('/raporlar/tedarikci-urunler')
  },
  urunKarlilik() {
    return apiClient.get('/raporlar/urun-karlilik')
  },
  nakitAkisiProjeksiyonu(gun = 30) {
    return apiClient.get('/raporlar/nakit-akisi-projeksiyonu', { params: { gun } })
  },
  butceGerceklesen(params) {
    return apiClient.get('/raporlar/butce-gerceklesen', { params })
  },
  butceGerceklesenPdf(params) {
    return apiClient.get('/raporlar/butce-gerceklesen/pdf', { params, responseType: 'blob' })
  },
  cariEkstrePdf(params) {
    return apiClient.get('/raporlar/cari-ekstre/pdf', { params, responseType: 'blob' })
  },
  gelirGiderPdf(params) {
    return apiClient.get('/raporlar/gelir-gider/pdf', { params, responseType: 'blob' })
  },
  cariKarlilikPdf(params) {
    return apiClient.get('/raporlar/cari-karlilik/pdf', { params, responseType: 'blob' })
  },
  pivot(params) {
    return apiClient.get('/raporlar/pivot', { params })
  },
  karlilikAnalizi(params) {
    return apiClient.get('/raporlar/karlilik-analizi', { params })
  },
  stokKar360(params) {
    return apiClient.get('/raporlar/stok-kar-360', { params })
  },
  calisanPerformans360() {
    return apiClient.get('/raporlar/calisan-performans-360')
  },
  musteriSegment() {
    return apiClient.get('/raporlar/musteri-segment')
  },
  faturaGecmis(params) {
    return apiClient.get('/raporlar/fatura-gecmis', { params })
  },
  faturaGecmisPdf(params) {
    return apiClient.get('/raporlar/fatura-gecmis/pdf', { params, responseType: 'blob' })
  },
  faturaGecmisExcel(params) {
    return apiClient.get('/raporlar/fatura-gecmis/excel', { params, responseType: 'blob' })
  },
  epostaGonderPdf(pdfBlob, alici, baslik, dosyaAdi) {
    const fd = new FormData()
    const parca = pdfBlob instanceof Blob ? pdfBlob : new Blob([pdfBlob != null ? pdfBlob : ''])
    const ad = typeof dosyaAdi === 'string' && dosyaAdi ? dosyaAdi : 'rapor.pdf'
    fd.append('dosya', parca, ad)
    fd.append('alici', alici != null ? String(alici) : '')
    if (typeof baslik === 'string' && baslik) fd.append('baslik', baslik)
    if (typeof dosyaAdi === 'string' && dosyaAdi) fd.append('dosyaAdi', dosyaAdi)
    return apiClient.post('/raporlar/eposta', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
  }
}

export const yoneticiKokpitAPI = {
  getVeriler(params) {
    return apiClient.get('/yonetici-kokpit', { params })
  },
  hedefKaydet(data) {
    return apiClient.post('/yonetici-kokpit/hedef', data)
  },
  karlilikAnalizi(params) {
    return apiClient.get('/yonetici-kokpit/karlilik-analizi', { params })
  }
}

