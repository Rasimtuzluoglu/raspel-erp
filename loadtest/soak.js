// Soak: kesintisiz kullanimda sizinti/kararlilik (varsayilan 30 dk, DURATION ile degisir)
// Kullanim: docker compose --profile loadtest run --rm -e DURATION=30m k6 run soak.js
import { sleep } from 'k6'
import { girisYap, okumaKarisi, barkodAra } from './lib.js'

export const options = {
  vus: 20,
  duration: __ENV.DURATION || '30m',
  thresholds: {
    http_req_failed: ['rate<0.005'],
    http_req_duration: ['p(95)<1200']
  }
}

export function setup() {
  return { token: girisYap() }
}

export default function (data) {
  okumaKarisi(data.token)
  if (Math.random() < 0.4) barkodAra(data.token)
  sleep(Math.random() * 0.5)
}
