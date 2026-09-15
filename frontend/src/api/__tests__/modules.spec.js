import { vi, describe, it, expect } from 'vitest'

vi.mock('../client.js', () => ({
  apiClient: {
    get: vi.fn(() => Promise.resolve({ data: {} })),
    post: vi.fn(() => Promise.resolve({ data: {} })),
    put: vi.fn(() => Promise.resolve({ data: {} })),
    patch: vi.fn(() => Promise.resolve({ data: {} })),
    delete: vi.fn(() => Promise.resolve({ data: {} }))
  }
}))

import * as finans from '../modules/finans.js'
import * as ticaret from '../modules/ticaret.js'
import * as stok from '../modules/stok.js'
import * as ik from '../modules/ik.js'
import * as sistem from '../modules/sistem.js'
import * as rapor from '../modules/rapor.js'
import * as dosya from '../modules/dosya.js'
import * as push from '../modules/push.js'
import * as veriAktarim from '../modules/veriAktarim.js'

const moduller = { finans, ticaret, stok, ik, sistem, rapor, dosya, push, veriAktarim }

// API modulleri ince http sarmalayicilaridir; her metot dummy argumanlarla
// cagrilir ve tanimli (Promise) donmesi beklenir.
describe('API modulleri', () => {
  for (const [modName, mod] of Object.entries(moduller)) {
    for (const [apiName, api] of Object.entries(mod)) {
      describe(`${modName}/${apiName}`, () => {
        for (const [fnName, fn] of Object.entries(api)) {
          if (typeof fn !== 'function') continue
          it(`${fnName} cagrilabilir`, () => {
            const sonuc = fn(1, { a: 1 }, 'x', 2, 3)
            expect(sonuc).toBeDefined()
          })
        }
      })
    }
  }
})
