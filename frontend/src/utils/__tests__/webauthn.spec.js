import { describe, it, expect, vi, afterEach } from 'vitest'
import {
  b64ToBuffer,
  bufferToB64,
  passkeyDestekleniyor,
  passkeyOlustur,
  passkeyIleGiris
} from '../webauthn.js'

const dizi = (deger) => new TextEncoder().encode(deger)

afterEach(() => {
  vi.unstubAllGlobals()
  delete window.PublicKeyCredential
  delete navigator.credentials
})

describe('webauthn yardımcıları', () => {
  it('base64url ↔ ArrayBuffer dönüşümü kayıpsızdır', () => {
    const ham = dizi('RasPel passkey')
    const kod = bufferToB64(ham)
    expect(kod).not.toContain('=')
    expect(kod).not.toContain('+')
    expect(Array.from(b64ToBuffer(kod))).toEqual(Array.from(ham))
  })

  it('boş değer güvenli davranır', () => {
    expect(Array.from(b64ToBuffer(''))).toEqual([])
  })

  it('passkey desteğini ortama göre bildirir', () => {
    expect(passkeyDestekleniyor()).toBe(false)
    window.PublicKeyCredential = function () {}
    navigator.credentials = { create: vi.fn(), get: vi.fn() }
    expect(passkeyDestekleniyor()).toBe(true)
  })

  it('oluşturma sonucunu sunucuya uygun JSON’a çevirir', async () => {
    window.PublicKeyCredential = function () {}
    const credential = {
      id: 'abc',
      rawId: dizi('abc').buffer,
      type: 'public-key',
      authenticatorAttachment: 'platform',
      getClientExtensionResults: () => ({ credProps: { rk: true } }),
      response: {
        attestationObject: dizi('att').buffer,
        clientDataJSON: dizi('client').buffer,
        getTransports: () => ['internal']
      }
    }
    navigator.credentials = { create: vi.fn().mockResolvedValue(credential) }

    const sonuc = await passkeyOlustur({
      challenge: 'Y2hhbGxlbmdl',
      user: { id: 'MQ', name: 'test', displayName: 'Test' },
      excludeCredentials: []
    })

    expect(sonuc.id).toBe('abc')
    expect(sonuc.rawId).toBe(bufferToB64(dizi('abc')))
    expect(sonuc.response.attestationObject).toBe(bufferToB64(dizi('att')))
    expect(sonuc.response.clientDataJSON).toBe(bufferToB64(dizi('client')))
    expect(sonuc.response.transports).toEqual(['internal'])
    expect(sonuc.clientExtensionResults).toEqual({ credProps: { rk: true } })
  })

  it('giriş sonucunu sunucuya uygun JSON’a çevirir', async () => {
    window.PublicKeyCredential = function () {}
    const credential = {
      id: 'abc',
      rawId: dizi('abc').buffer,
      type: 'public-key',
      getClientExtensionResults: () => ({}),
      response: {
        authenticatorData: dizi('auth').buffer,
        clientDataJSON: dizi('client').buffer,
        signature: dizi('sig').buffer,
        userHandle: dizi('1').buffer
      }
    }
    navigator.credentials = { get: vi.fn().mockResolvedValue(credential) }

    const sonuc = await passkeyIleGiris({ challenge: 'Y2hhbGxlbmdl', rpId: 'localhost' })

    expect(sonuc.response.authenticatorData).toBe(bufferToB64(dizi('auth')))
    expect(sonuc.response.signature).toBe(bufferToB64(dizi('sig')))
    expect(sonuc.response.userHandle).toBe(bufferToB64(dizi('1')))
  })

  it('kullanıcı iptalinde null döner', async () => {
    window.PublicKeyCredential = function () {}
    navigator.credentials = { get: vi.fn().mockResolvedValue(null) }
    expect(await passkeyIleGiris({ challenge: 'Y2hhbGxlbmdl' })).toBeNull()
  })
})
