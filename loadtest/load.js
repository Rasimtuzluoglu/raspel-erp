// Load: beklenen gunluk yuk (3 kasa + 2 depo ~ 20 es zamanli oturum), ~6 dk
import { sleep } from 'k6'
import { girisYap, okumaKarisi, barkodAra } from './lib.js'

export const options = {
  stages: [
    { duration: '1m', target: 10 },
    { duration: '2m', target: 20 },
    { duration: '2m', target: 20 },
    { duration: '1m', target: 0 }
  ],
  thresholds: {
    http_req_failed: ['rate<0.005'],
    http_req_duration: ['p(95)<1000', 'p(99)<2000']
  }
}

export function setup() {
  return { token: girisYap() }
}

export default function (data) {
  okumaKarisi(data.token)
  if (Math.random() < 0.4) barkodAra(data.token)
  sleep(Math.random() * 1.5)
}
