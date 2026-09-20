// Spike: ani yogunluk (ornek: ayni anda 100 musteri/kasa), ~4 dk
import { sleep } from 'k6'
import { girisYap, okumaKarisi, barkodAra } from './lib.js'

export const options = {
  stages: [
    { duration: '30s', target: 5 },
    { duration: '30s', target: 100 }, // ani sicrama
    { duration: '1m', target: 100 },
    { duration: '1m', target: 5 },
    { duration: '30s', target: 0 }
  ],
  thresholds: {
    http_req_failed: ['rate<0.02']
  }
}

export function setup() {
  return { token: girisYap() }
}

export default function (data) {
  okumaKarisi(data.token)
  if (Math.random() < 0.5) barkodAra(data.token)
  sleep(Math.random())
}
