// RasPel ERP - API Yük Testi
// Tek giriş (token), sonra eşzamanlı API istekleri — login rate-limiter'ı atlar
// Kullanım: node scripts/load-test.mjs [esZamanli] [tekrar]

const BASE = 'http://localhost:8081'
const ESZAMANLI = parseInt(process.argv[2] || '50', 10)
const TEKRAR = parseInt(process.argv[3] || '5', 10)

console.log(`=== RasPel ERP API Yük Testi: ${ESZAMANLI} eşzamanlı x ${TEKRAR} tekrar ===\n`)

// Tek giriş
const girisBas = performance.now()
const giris = await fetch(`${BASE}/api/kullanicilar/giris`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ username: 'admin', password: 'admin123', companyName: 'ABC Ön Muhasebe' })
})
if (!giris.ok) throw new Error(`Giriş başarısız: ${giris.status}`)
const { token } = await giris.json()
console.log(`Giriş: ${Math.round(performance.now() - girisBas)}ms (token alındı)\n`)

const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` }
const endpointler = [
  ['cari-listesi', '/api/cari-hesaplar?page=0&size=50'],
  ['stok-listesi', '/api/stoklar?page=0&size=50'],
  ['fatura-listesi', '/api/faturalar?page=0&size=50'],
  ['dashboard', '/api/dashboard'],
  ['not-listesi', '/api/notlar?page=0&size=50']
]

const sureler = {}
const hatalar = []
let toplamIstek = 0

const baslangic = performance.now()
for (let tur = 0; tur < TEKRAR; tur++) {
  for (const [ad, yol] of endpointler) {
    const bas = performance.now()
    const sonuclar = await Promise.all(
      Array.from({ length: ESZAMANLI }, async () => {
        const r = await fetch(`${BASE}${yol}`, { headers })
        if (!r.ok) hatalar.push(`${ad}: ${r.status}`)
      })
    )
    const ms = performance.now() - bas
    if (!sureler[ad]) sureler[ad] = []
    sureler[ad].push(ms)
    toplamIstek += ESZAMANLI
  }
}
const toplamMs = performance.now() - baslangic

console.log('=== Toplu İşlem Süreleri (ms) ===')
console.log('İşlem        | Ort | Min | Max')
for (const [ad] of endpointler) {
  const grup = sureler[ad]
  const ort = Math.round(grup.reduce((a, b) => a + b, 0) / grup.length)
  console.log(`${ad.padEnd(12)} | ${String(ort).padStart(3)} | ${String(Math.min(...grup)).padStart(3)} | ${String(Math.max(...grup)).padStart(3)}`)
}

console.log(`\n=== Özet ===`)
console.log(`Toplam istek: ${toplamIstek}, Hata: ${hatalar.length}`)
console.log(`Toplam süre: ${Math.round(toplamMs / 1000)}s, İstek/sn: ${Math.round(toplamIstek / (toplamMs / 1000))}`)
const tekistekOrt = Math.round(toplamMs / toplamIstek)
console.log(`Ortalama tek istek süresi: ${tekistekOrt}ms (${ESZAMANLI} eşzamanlıda)`)

if (hatalar.length) {
  console.log('\nHata örnekleri:')
  hatalar.slice(0, 5).forEach(h => console.log(`  - ${h}`))
}

const basariOrani = Math.round(((toplamIstek - hatalar.length) / toplamIstek) * 100)
console.log(`\n=== SONUÇ: Başarı oranı %${basariOrani} ${basariOrani >= 99.5 ? '- YÜK TESTİ BAŞARILI ✅' : '- İNCELEME GEREKİYOR ⚠️'} ===`)
process.exit(basariOrani >= 99.5 ? 0 : 1)
