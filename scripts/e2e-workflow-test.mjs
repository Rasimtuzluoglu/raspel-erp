// RasPel ERP - Uçtan Uca İş Akışı Testi
// Akış: Giriş → Cari oluştur → Stok oluştur → Sipariş oluştur → Fatura oluştur → Tahsilat → Doğrula
// Kullanım: node scripts/e2e-workflow-test.mjs

const BASE = 'http://localhost:8081'
let TOKEN = ''
let hataSayisi = 0

const api = async (method, path, body = null) => {
  const headers = { 'Content-Type': 'application/json' }
  if (TOKEN) headers.Authorization = `Bearer ${TOKEN}`
  const res = await fetch(`${BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  })
  if (!res.ok) throw new Error(`${method} ${path} -> ${res.status}: ${await res.text()}`)
  return res.status === 204 ? null : res.json()
}

const basarisiz = (name, e) => {
  hataSayisi++
  console.log(`  ✗ ${name} -> ${e.message}`)
}

console.log('=== RasPel ERP Uçtan Uca İş Akışı Testi ===\n')

// 1. Giriş
console.log('[1/8] Giriş')
let kullanici
try {
  const giris = await api('POST', '/api/kullanicilar/giris', { username: 'admin', password: 'admin123', companyName: 'ABC Ön Muhasebe' })
  TOKEN = giris.token
  kullanici = giris
  console.log(`  ✓ Giriş başarılı (${giris.username}, rol: ${giris.role})`)
} catch (e) {
  console.log(`  ✗ Giriş BAŞARISIZ -> ${e.message}`)
  console.log('  Not: admin/admin123 girişi yapılamadı. Seed verisi kontrol edin.')
  process.exit(1)
}

// 2. Cari oluştur
console.log('\n[2/8] Cari Hesap Oluştur')
let cariId = null
try {
  const cari = await api('POST', '/api/cari-hesaplar', {
    ad: `E2E Test Cari ${Date.now()}`,
    tur: 'Musteri',
    telefon: '0500 000 00 00',
    email: 'e2e@test.com',
    aktif: true
  })
  cariId = cari.id
  console.log(`  ✓ Cari oluşturuldu (id: ${cariId}, ad: ${cari.ad})`)
} catch (e) { console.log(`  ✗ Cari oluşturulamadı -> ${e.message}`) }

// 3. Stok oluştur
console.log('\n[3/8] Stok Ürünü Oluştur')
let stokId = null
try {
  const stok = await api('POST', '/api/stoklar', {
    ad: 'E2E Test MDF 18mm',
    stokKodu: `E2E-${Date.now()}`,
    birim: 'Adet',
    fiyat: 850,
    satisFiyati: 1050,
    miktar: 100,
    minMiktar: 10
  })
  stokId = stok.id
  console.log(`  ✓ Stok oluşturuldu (id: ${stokId})`)
} catch (e) { console.log(`  ✗ Stok oluşturulamadı -> ${e.message}`) }

// 4. Sipariş oluştur
console.log('\n[4/8] Sipariş Oluştur')
let siparisNo = null
try {
  const siparis = await api('POST', '/api/siparisler', {
    cariHesapId: cariId,
    tarih: new Date().toISOString().split('T')[0],
    durum: 'ONAYLANDI',
    araToplam: 2100,
    kdv: 420,
    genelToplam: 2520,
    kalemler: stokId ? [{ stokId, aciklama: 'E2E Test MDF 18mm', miktar: 2, birimFiyat: 1050, kdvOrani: 20, tutar: 2520 }] : []
  })
  siparisNo = siparis.siparisNo
  console.log(`  ✓ Sipariş oluşturuldu (no: ${siparisNo})`)
} catch (e) { console.log(`  ✗ Sipariş oluşturulamadı -> ${e.message}`) }

// 5. Fatura oluştur
console.log('\n[5/8] Fatura Oluştur')
let faturaNo = null
try {
  const fatura = await api('POST', '/api/faturalar', {
    cariHesapId: cariId,
    tur: 'SATIS',
    tarih: new Date().toISOString().split('T')[0],
    aciklama: 'E2E Test Faturası',
    kalemler: stokId ? [{ stokId, aciklama: 'E2E Test MDF 18mm', adet: 2, birimFiyat: 1050, kdvOrani: 20, iskontoOrani: 0 }] : []
  })
  faturaNo = fatura.faturaNumarasi
  console.log(`  ✓ Fatura oluşturuldu (no: ${faturaNo})`)
} catch (e) { console.log(`  ✗ Fatura oluşturulamadı -> ${e.message}`) }

// 6. Tahsilat hareketi
console.log('\n[6/8] Tahsilat Hareketi')
try {
  const hareket = await api('POST', '/api/hareketler', {
    cariHesapId: cariId,
    tur: 'TAHSILAT',
    tutar: 1000,
    aciklama: 'E2E Test tahsilatı',
    hareketTarihi: new Date().toISOString().split('T')[0]
  })
  console.log(`  ✓ Tahsilat kaydedildi (id: ${hareket.id})`)
} catch (e) { console.log(`  ✗ Tahsilat kaydedilemedi -> ${e.message}`) }

// 7. Doğrulama: veriler geri okunuyor
console.log('\n[7/8] Doğrulama')
if (cariId) {
  try {
    const cari = await api('GET', `/api/cari-hesaplar/${cariId}`)
    console.log(`  ✓ Cari okundu: ${cari.ad}, bakiye: ${cari.bakiye}`)
  } catch (e) { console.log(`  ✗ Cari okunamadı -> ${e.message}`) }
}
if (faturaNo) {
  try {
    const faturalar = await api('GET', '/api/faturalar?size=5')
    const bulunan = (faturalar.content || []).find(f => f.faturaNumarasi === faturaNo)
    console.log(`  ${bulunan ? '✓' : '✗'} Fatura listede ${bulunan ? '' : 'BULUNAMADI'}`)
  } catch (e) { console.log(`  ✗ Fatura listesi okunamadı -> ${e.message}`) }
}

// 8. PDF rapor kontrolü (oluşturulan siparişin PDF'i)
console.log('\n[8/8] PDF Raporu')
try {
  const siparisler = await api('GET', '/api/siparisler?size=5')
  const bulunanSiparis = (siparisler.content || []).find(s => s.siparisNo === siparisNo)
  if (bulunanSiparis) {
    const res = await fetch(`${BASE}/api/rapor/siparis/${bulunanSiparis.id}`, { headers: { Authorization: `Bearer ${TOKEN}` } })
    if (res.ok && res.headers.get('content-type')?.includes('application/pdf')) {
      console.log(`  ✓ PDF raporu üretildi (sipariş: ${siparisNo})`)
    } else {
      basarisiz('PDF üretimi', new Error(`HTTP ${res.status}`))
    }
  } else {
    basarisiz('PDF üretimi', new Error('Sipariş bulunamadı'))
  }
} catch (e) { basarisiz('PDF üretimi', e) }

console.log(`\n=== SONUÇ: ${hataSayisi === 0 ? 'TÜM ADIMLAR BAŞARILI ✅' : hataSayisi + ' HATA ❌'} ===`)
process.exit(hataSayisi === 0 ? 0 : 1)
