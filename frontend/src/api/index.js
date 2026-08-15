export { apiClient, networkStatus } from './client.js'
export { default } from './client.js'

export {
  bankaAPI,
  bankaMutabakatAPI,
  kasaAPI,
  butceAPI,
  masrafAPI,
  cekSenetAPI,
  dovizAPI,
  muhasebeAPI
} from './modules/finans.js'

export {
  cariHesapAPI,
  hareketAPI,
  faturaAPI,
  eFaturaAPI,
  siparisAPI,
  irsaliyeAPI,
  iadeAPI,
  fiyatListesiAPI,
  crmAPI,
  satinalmaTalepAPI,
  satinalmaSiparisAPI
} from './modules/ticaret.js'

export {
  stokAPI,
  depoAPI,
  kategoriAPI,
  stokSeriAPI,
  stokSayimAPI
} from './modules/stok.js'

export {
  personelAPI,
  personelIzinAPI,
  puantajAPI,
  maasBordroAPI,
  vardiyaAPI
} from './modules/ik.js'

export {
  kullaniciAPI,
  kurulumAPI,
  sirketAPI,
  subeAPI,
  donemAPI,
  auditLogAPI,
  notAPI,
  projeAPI
} from './modules/sistem.js'

export {
  dashboardAPI,
  raporAPI
} from './modules/rapor.js'

export {
  uploadAPI,
  backupAPI,
  excelAPI,
  importAPI,
  pdfAPI,
  belgeAPI
} from './modules/dosya.js'
