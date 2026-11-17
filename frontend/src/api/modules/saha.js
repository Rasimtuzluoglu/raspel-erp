import { apiClient } from '../client.js'

/**
 * Saha portalı uçları. Tüm veriler oturum kullanıcısına göre daraltılır.
 */
export const sahaAPI = {
  /** Bugünkü teslimat/ziyaret/talep özeti. */
  ozet() {
    return apiClient.get('/saha/ozet')
  }
}
