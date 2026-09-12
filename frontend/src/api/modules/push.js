import { apiClient } from '../client.js'

export const pushAPI = {
  vapidKey() {
    return apiClient.get('/push/vapid-public-key')
  },
  abone(data) {
    return apiClient.post('/push/abone', data)
  },
  aboneSil(endpoint) {
    return apiClient.delete('/push/abone', { params: { endpoint } })
  },
  test() {
    return apiClient.post('/push/test')
  }
}
