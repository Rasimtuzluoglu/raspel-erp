// Write flow: POS satis zinciri (POST /api/faturalar) yazma yuku, ~5 dk
// Kullanim: docker compose --profile loadtest run --rm k6 run write-flow.js
import { sleep, check } from 'k6'
import { girisYap, ornekIdler, satisYap } from './lib.js'

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '3m', target: 15 },
    { duration: '1m', target: 0 }
  ],
  thresholds: {
    'http_req_failed{endpoint:POST /api/faturalar}': ['rate<0.01'],
    'http_req_duration{endpoint:POST /api/faturalar}': ['p(95)<1500']
  }
}

export function setup() {
  const token = girisYap()
  const idler = ornekIdler(token)
  if (!idler.cariId || !idler.stokId) {
    throw new Error('Test sirketinde cari/stok bulunamadi (veri uretildi mi?)')
  }
  return { token, cariId: idler.cariId, stokId: idler.stokId }
}

export default function (data) {
  const r = satisYap(data.token, data.cariId, data.stokId)
  check(r, { 'satis basarili': (x) => x.status === 201 })
  sleep(Math.random() * 2)
}
