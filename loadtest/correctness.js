// Correctness: es zamanlilik dogrulugu (tek VU, tek iterasyon)
//  1) Stok asiri satis (oversell) engeli
//  2) Fatura idempotency (ayni X-Idempotency-Key)
// Kullanim: docker compose --profile loadtest run --rm k6 run correctness.js
import http from 'k6/http'
import { check } from 'k6'
import { BASE, girisYap, jsonHeaders, ornekIdler } from './lib.js'

export const options = {
  vus: 1,
  iterations: 1
}

function satisGovde(cariId, stokId, adet) {
  return {
    tarih: new Date().toISOString().slice(0, 10),
    tur: 'SATIS',
    durum: 'KESILDI',
    cariHesapId: cariId,
    kalemler: [{
      aciklama: 'Dogruluk testi',
      adet: adet,
      birimFiyat: 10,
      kdvOrani: 20,
      stokId: stokId,
      tutar: adet * 10
    }]
  }
}

export function setup() {
  const token = girisYap()
  const h = jsonHeaders(token)
  const idler = ornekIdler(token)

  // Oversell icin miktari 10 olan ozel stok olustur.
  const stokGovde = {
    ad: 'Yuk Testi Oversell Stok',
    stokKodu: 'YUK-OVERSELL-' + Date.now(),
    barkod: String(Date.now()),
    birim: 'ADET',
    fiyat: 10,
    miktar: 10,
    kdvOrani: 20
  }
  const sr = http.post(`${BASE}/api/stoklar`, JSON.stringify(stokGovde), { headers: h })
  if (sr.status !== 201 && sr.status !== 200) throw new Error(`Stok olusturulamadi: ${sr.status} ${sr.body}`)
  const stokId = sr.json().id

  return { token, cariId: idler.cariId, stokId, normalStokId: idler.stokId }
}

export default function (data) {
  const h = jsonHeaders(data.token)

  // ---- 1) OVERSELL: stok 10 iken 30 es zamanli 1'er adet satis ----
  const istekler = []
  for (let i = 0; i < 30; i++) {
    istekler.push({
      method: 'POST',
      url: `${BASE}/api/faturalar`,
      body: JSON.stringify(satisGovde(data.cariId, data.stokId, 1)),
      params: { headers: h, tags: { endpoint: 'oversell' }, responseCallback: http.expectedStatuses(201, 400) }
    })
  }
  const sonuclar = http.batch(istekler)
  let basarili = 0
  sonuclar.forEach((r) => { if (r.status === 201) basarili++ })

  const stok = http.get(`${BASE}/api/stoklar/${data.stokId}`, { headers: h }).json()
  const kalan = Number(stok.miktar)
  check(kalan, { 'stok negatife dusmedi': (m) => m >= 0 })
  check(basarili, { 'asiri satis engellendi (<=10)': (n) => n <= 10 })
  console.log(`OVERSell -> basarili satis=${basarili}, kalan miktar=${kalan}`)

  // ---- 2) IDEMPOTENCY: ayni anahtar ile 10 es zamanli satis ----
  // (stogu bol olan normal urun kullanilir; oversell stogu tukenmis olabilir)
  const anahtar = 'yuk-idem-' + Date.now()
  const istekler2 = []
  for (let i = 0; i < 10; i++) {
    const basliklar = Object.assign({}, h, { 'X-Idempotency-Key': anahtar })
    istekler2.push({
      method: 'POST',
      url: `${BASE}/api/faturalar`,
      body: JSON.stringify(satisGovde(data.cariId, data.normalStokId, 1)),
      params: { headers: basliklar, tags: { endpoint: 'idempotency' }, responseCallback: http.expectedStatuses(200, 201, 409) }
    })
  }
  const son2 = http.batch(istekler2)
  const idler = {}
  son2.forEach((r) => {
    if (r.status === 200 || r.status === 201) {
      const j = r.json()
      if (j && j.id) idler[j.id] = true
    }
  })
  const farkliKayit = Object.keys(idler).length
  check(farkliKayit, { 'idempotency: tek kayit': (n) => n === 1 })
  console.log(`Idempotency -> olusan farkli fatura id sayisi=${farkliKayit}`)
}
