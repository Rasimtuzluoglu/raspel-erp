/**
 * WebAuthn / passkey istemci yardimcilari.
 *
 * Sunucudan gelen base64url alanlari ArrayBuffer'a cevirir, tarayicidan donen
 * PublicKeyCredential'i JSON'a serilestirir. Parola veya token tasimaz.
 */

export const passkeyDestekleniyor = () => {
  try {
    return typeof window !== 'undefined'
      && typeof window.PublicKeyCredential === 'function'
      && !!navigator?.credentials
  } catch {
    return false
  }
}

export const b64ToBuffer = (deger) => {
  if (!deger) return new Uint8Array(0)
  const normal = String(deger).replace(/-/g, '+').replace(/_/g, '/')
  const dolgu = normal + '='.repeat((4 - (normal.length % 4)) % 4)
  const ikili = atob(dolgu)
  const bayt = new Uint8Array(ikili.length)
  for (let i = 0; i < ikili.length; i += 1) bayt[i] = ikili.charCodeAt(i)
  return bayt
}

export const bufferToB64 = (buffer) => {
  const bayt = new Uint8Array(buffer)
  let ikili = ''
  for (let i = 0; i < bayt.length; i += 1) ikili += String.fromCharCode(bayt[i])
  return btoa(ikili).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

const hazirlaOlusturmaSecenekleri = (publicKey) => ({
  ...publicKey,
  challenge: b64ToBuffer(publicKey.challenge),
  user: {
    ...publicKey.user,
    id: b64ToBuffer(publicKey.user?.id)
  },
  excludeCredentials: (publicKey.excludeCredentials || []).map((c) => ({
    ...c,
    id: b64ToBuffer(c.id)
  }))
})

const hazirlaIstekSecenekleri = (publicKey) => ({
  ...publicKey,
  challenge: b64ToBuffer(publicKey.challenge),
  allowCredentials: (publicKey.allowCredentials || []).map((c) => ({
    ...c,
    id: b64ToBuffer(c.id)
  }))
})

const credentialJson = (credential) => ({
  id: credential.id,
  rawId: bufferToB64(credential.rawId),
  type: credential.type,
  authenticatorAttachment: credential.authenticatorAttachment || undefined,
  clientExtensionResults: credential.getClientExtensionResults?.() || {},
  response: {
    attestationObject: credential.response?.attestationObject
      ? bufferToB64(credential.response.attestationObject)
      : undefined,
    clientDataJSON: bufferToB64(credential.response?.clientDataJSON),
    transports: credential.response?.getTransports?.() || []
  }
})

const assertionJson = (credential) => ({
  id: credential.id,
  rawId: bufferToB64(credential.rawId),
  type: credential.type,
  authenticatorAttachment: credential.authenticatorAttachment || undefined,
  clientExtensionResults: credential.getClientExtensionResults?.() || {},
  response: {
    authenticatorData: bufferToB64(credential.response?.authenticatorData),
    clientDataJSON: bufferToB64(credential.response?.clientDataJSON),
    signature: bufferToB64(credential.response?.signature),
    userHandle: credential.response?.userHandle
      ? bufferToB64(credential.response.userHandle)
      : null
  }
})

/** Tarayicida passkey olusturur; kullanici iptal ederse null doner. */
export const passkeyOlustur = async (publicKey) => {
  if (!passkeyDestekleniyor()) return null
  const credential = await navigator.credentials.create({
    publicKey: hazirlaOlusturmaSecenekleri(publicKey)
  })
  return credential ? credentialJson(credential) : null
}

/** Tarayicida passkey ile giris yapar; kullanici iptal ederse null doner. */
export const passkeyIleGiris = async (publicKey) => {
  if (!passkeyDestekleniyor()) return null
  const credential = await navigator.credentials.get({
    publicKey: hazirlaIstekSecenekleri(publicKey)
  })
  return credential ? assertionJson(credential) : null
}
