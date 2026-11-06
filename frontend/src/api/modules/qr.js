import { apiClient } from '../client.js'

/**
 * Telefonla QR oturum onayı uclari.
 * Olusturma/poll/tamamla public; onay/red oturum ister.
 */
export const qrOturumAPI = {
  baslat(girisToken) {
    return apiClient.post('/kullanicilar/qr-oturum', { girisToken })
  },
  durum(token) {
    return apiClient.get(`/kullanicilar/qr-oturum/${token}`)
  },
  onayla(token) {
    return apiClient.post(`/kullanicilar/qr-oturum/${token}/onayla`)
  },
  reddet(token) {
    return apiClient.post(`/kullanicilar/qr-oturum/${token}/reddet`)
  },
  tamamla(girisToken, rememberMe) {
    return apiClient.post('/kullanicilar/giris-qr-tamamla', {
      girisToken,
      rememberMe: rememberMe === true
    })
  },
  gorselUrl(token) {
    const taban = apiClient.defaults.baseURL || '/api'
    return `${taban.replace(/\/$/, '')}/kullanicilar/qr-oturum/${token}/gorsel`
  }
}
