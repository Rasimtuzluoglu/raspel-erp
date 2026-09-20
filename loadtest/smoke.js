// Smoke: hizli dogru akis kontrolu (1 VU, 30 sn)
import { sleep } from 'k6'
import { girisYap, okumaKarisi, barkodAra } from './lib.js'

export const options = {
  vus: 1,
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<800']
  }
}

export function setup() {
  return { token: girisYap() }
}

export default function (data) {
  okumaKarisi(data.token)
  if (Math.random() < 0.3) barkodAra(data.token)
  sleep(1)
}
