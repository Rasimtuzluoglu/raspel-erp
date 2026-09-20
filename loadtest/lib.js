// RasPel ERP - k6 ortak yardimcilar
import http from 'k6/http'
import { check } from 'k6'

export const BASE = __ENV.BASE_URL || 'http://raspel-backend:8081'

export function jsonHeaders(token) {
  const h = { 'Content-Type': 'application/json' }
  if (token) h.Authorization = `Bearer ${token}`
  return h
}

/** Tek giris + sirket secimi; JWT doner. (Login rate-limit'e takilmamak icin bir kez cagrilir.) */
export function girisYap() {
  const user = __ENV.TEST_USER || 'admin_test'
  const pass = __ENV.TEST_PASSWORD || 'Test1234!'
  const sirketId = Number(__ENV.TEST_SIRKET_ID || 5)

  const r = http.post(`${BASE}/api/kullanicilar/giris`,
    JSON.stringify({ username: user, password: pass }),
    { headers: { 'Content-Type': 'application/json' } })
  if (r.status !== 200) throw new Error(`Giris basarisiz: ${r.status} ${r.body}`)
  const j = r.json()
  if (j.token) return j.token

  const r2 = http.post(`${BASE}/api/kullanicilar/giris-sirket`,
    JSON.stringify({ girisToken: j.girisToken, sirketId }),
    { headers: { 'Content-Type': 'application/json' } })
  if (r2.status !== 200) throw new Error(`Sirket secimi basarisiz: ${r2.status} ${r2.body}`)
  return r2.json().token
}

/** Test sirketinde ornek cari + stok id'lerini bulur (yazma senaryolari icin). */
export function ornekIdler(token) {
  const h = jsonHeaders(token)
  let cariId = null
  let stokId = null
  const c = http.get(`${BASE}/api/cari-hesaplar?page=0&size=1`, { headers: h })
  if (c.status === 200) {
    const body = c.json()
    if (body && body.content && body.content.length) cariId = body.content[0].id
  }
  const s = http.get(`${BASE}/api/stoklar?page=0&size=1`, { headers: h })
  if (s.status === 200) {
    const body = s.json()
    if (body && body.content && body.content.length) stokId = body.content[0].id
  }
  return { cariId, stokId }
}

const OKUMA_UCLARI = [
  ['stok-listesi', '/api/stoklar?page=0&size=50'],
  ['cari-listesi', '/api/cari-hesaplar?page=0&size=50'],
  ['fatura-listesi', '/api/faturalar?page=0&size=50'],
  ['dashboard', '/api/dashboard'],
  ['stok-filtreli', '/api/stoklar/filtreli?page=0&size=50'],
  ['cari-filtreli', '/api/cari-hesaplar/filtreli?page=0&size=50']
]

/** Gercekci okuma karisimi (listeler + dashboard + arama). */
export function okumaKarisi(token) {
  const h = jsonHeaders(token)
  const secim = OKUMA_UCLARI[Math.floor(Math.random() * OKUMA_UCLARI.length)]
  const r = http.get(`${BASE}${secim[1]}`, { headers: h, tags: { endpoint: secim[0] } })
  check(r, { 'okuma 200': (x) => x.status === 200 })
  return r
}

/** Barkodla urun arama (POS hizli akisi). Barkod formati: 869 + 10 haneli siralama. */
export function barkodAra(token) {
  const no = Math.floor(Math.random() * 2000) + 1
  const barkod = '869' + String(no).padStart(10, '0')
  const r = http.get(`${BASE}/api/stoklar/barkod/${barkod}`, {
    headers: jsonHeaders(token),
    tags: { endpoint: 'stok-barkod' }
  })
  check(r, { 'barkod 200/404': (x) => x.status === 200 })
  return r
}

/** POS satisi: POST /api/faturalar (stok dusumu + cari bakiye + kasa). */
export function satisYap(token, cariId, stokId) {
  const kalemSayisi = Math.floor(Math.random() * 3) + 1
  const kalemler = []
  let toplam = 0
  for (let k = 0; k < kalemSayisi; k++) {
    const adet = Math.floor(Math.random() * 3) + 1
    const fiyat = Math.floor(Math.random() * 200) + 10
    toplam += adet * fiyat
    kalemler.push({
      aciklama: 'Yuk testi kalemi',
      adet: adet,
      birimFiyat: fiyat,
      kdvOrani: 20,
      stokId: stokId,
      tutar: adet * fiyat
    })
  }
  const govde = {
    tarih: new Date().toISOString().slice(0, 10),
    tur: 'SATIS',
    durum: 'KESILDI',
    cariHesapId: cariId,
    kalemler: kalemler
  }
  const r = http.post(`${BASE}/api/faturalar`, JSON.stringify(govde), {
    headers: jsonHeaders(token),
    tags: { endpoint: 'POST /api/faturalar' }
  })
  check(r, { 'satis 201': (x) => x.status === 201 })
  return r
}
