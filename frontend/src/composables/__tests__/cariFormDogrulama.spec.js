import { describe, it, expect } from 'vitest'
import {
  epostaGecerli,
  telefonGecerli,
  vergiNumarasiGecerli,
  cariFormDogrula
} from '../cariFormDogrulama.js'

const t = (anahtar) => anahtar

describe('cariFormDogrulama', () => {
  it('e-posta biçimini doğrular', () => {
    expect(epostaGecerli('')).toBe(true)
    expect(epostaGecerli('a@b.com')).toBe(true)
    expect(epostaGecerli('a@b')).toBe(false)
    expect(epostaGecerli('a b@c.com')).toBe(false)
  })

  it('telefonu 10-11 rakamla sınırlar', () => {
    expect(telefonGecerli('')).toBe(true)
    expect(telefonGecerli('0532 111 22 33')).toBe(true)
    expect(telefonGecerli('053211122')).toBe(false)
    expect(telefonGecerli('053211122334')).toBe(false)
  })

  it('vergi numarasını 10-11 rakamla sınırlar', () => {
    expect(vergiNumarasiGecerli('')).toBe(true)
    expect(vergiNumarasiGecerli('1234567890')).toBe(true)
    expect(vergiNumarasiGecerli('123 456 789 01')).toBe(true)
    expect(vergiNumarasiGecerli('12345')).toBe(false)
    expect(vergiNumarasiGecerli('123456789012')).toBe(false)
  })

  it('zorunlu alan ve biçim hatalarını birlikte döndürür', () => {
    const hatalar = cariFormDogrula({
      form: { ad: ' ', email: 'gecersiz', telefon: '123', vergiNumarasi: '1', iban: 'TR1' },
      ibanGecerli: false,
      t
    })

    expect(Object.keys(hatalar).sort()).toEqual(['ad', 'email', 'iban', 'telefon', 'vergiNumarasi'])
  })

  it('geçerli formda hata döndürmez', () => {
    const hatalar = cariFormDogrula({
      form: {
        ad: 'ACME',
        email: 'a@b.com',
        telefon: '0532 111 22 33',
        vergiNumarasi: '1234567890',
        iban: 'TR330006100519786457841326'
      },
      ibanGecerli: true,
      t
    })
    expect(hatalar).toEqual({})
  })
})
