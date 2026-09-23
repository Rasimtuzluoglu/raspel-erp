import { ref } from 'vue'
import { createCrudStore } from './createCrudStore.js'
import { faturaAPI } from '../api/index.js'

export const useFaturaStore = createCrudStore('fatura', faturaAPI, {
  stateKey: 'faturalar',
  addPosition: 'unshift',
  totalKey: 'toplamKayit',
  extraState: { toplamKayit: () => ref(0) },
  actions: { getAll: 'getAllFaturalar', add: 'addFatura', update: 'updateFatura', remove: 'deleteFatura' },
  extraActions: ({ liste, error, api }) => ({
    getFaturaById: async (id) => {
      try {
        const r = await api.getById(id)
        return r.data
      } catch (err) {
        error.value = err.message
        throw err
      }
    },
    updateDurum: async (id, durum) => {
      try {
        const r = await api.updateDurum(id, durum)
        const idx = liste.value.findIndex((f) => f.id === id)
        if (idx !== -1) liste.value[idx] = r.data
        return r.data
      } catch (err) {
        error.value = err.message
        throw err
      }
    }
  })
})
