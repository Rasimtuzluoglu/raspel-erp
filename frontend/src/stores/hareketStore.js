import { createCrudStore } from './createCrudStore.js'
import { hareketAPI } from '../api/index.js'

export const useHareketStore = createCrudStore('hareket', hareketAPI, {
  stateKey: 'hareketler',
  actions: { getAll: 'getAllHareketler', add: 'addHareket', update: 'updateHareket', remove: 'deleteHareket' },
  extraActions: ({ liste, loading, error, api }) => ({
    getHareketlerByCariHesap: async (cariHesapId) => {
      loading.value = true
      error.value = null
      try {
        const r = await api.getByCariHesap(cariHesapId)
        liste.value = r.data
        return r.data
      } catch (err) {
        error.value = err.message
        throw err
      } finally {
        loading.value = false
      }
    }
  })
})
