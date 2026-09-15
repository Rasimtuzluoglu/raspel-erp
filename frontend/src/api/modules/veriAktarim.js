import { apiClient } from '../client.js'

export const veriAktarimAPI = {
  sirketlerArasi(payload) {
    return apiClient.post('/veri-aktarim/sirketler-arasi', payload)
  }
}