// Stress: kirilma noktasini bulmak icin kademeli yuk (0 -> 200 VU), ~15 dk
import { sleep } from 'k6'
import { girisYap, okumaKarisi } from './lib.js'

export const options = {
  stages: [
    { duration: '2m', target: 50 },
    { duration: '3m', target: 100 },
    { duration: '3m', target: 150 },
    { duration: '3m', target: 200 },
    { duration: '2m', target: 100 },
    { duration: '2m', target: 0 }
  ],
  thresholds: {
    // Stress'te hata olabilir; kritik esik yalnizca asiri bozulmayi yakalar.
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000']
  }
}

export function setup() {
  return { token: girisYap() }
}

export default function (data) {
  okumaKarisi(data.token)
  sleep(Math.random())
}
