import { apiClient } from '../client.js'

/**
 * Passkey / WebAuthn uclari.
 * Kayit ve yonetim oturum ister; giris uclari public'tir.
 */
export const passkeyAPI = {
  kayitSecenekleri(password) {
    return apiClient.post('/kullanicilar/passkey/kayit/secenekler', { password })
  },
  kaydet(data) {
    return apiClient.post('/kullanicilar/passkey/kayit', data)
  },
  listele() {
    return apiClient.get('/kullanicilar/passkeyler')
  },
  sil(id) {
    return apiClient.delete(`/kullanicilar/passkeyler/${id}`)
  },
  girisSecenekleri() {
    return apiClient.post('/kullanicilar/passkey/giris/secenekler')
  },
  giris(data) {
    return apiClient.post('/kullanicilar/passkey/giris', data)
  },
  adminSil(kullaniciId) {
    return apiClient.delete(`/kullanicilar/${kullaniciId}/passkeyler`)
  }
}
