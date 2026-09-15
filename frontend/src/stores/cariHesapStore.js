import { ref } from 'vue'
import { createCrudStore } from './createCrudStore.js'
import { cariHesapAPI } from '../api/index.js'

/**
 * Cari Hesap Store
 * Cari hesaplara ait state ve actionları yönetir.
 */
export const useCariHesapStore = createCrudStore('cariHesap', cariHesapAPI, {
  stateKey: 'cariHesaplar',
  totalKey: 'toplamKayit',
  extraState: { toplamKayit: () => ref(0) },
  actions: { getAll: 'getAllCariHesaplar', add: 'addCariHesap', update: 'updateCariHesap', remove: 'deleteCariHesap' },
  extraActions: ({ liste, durumlar, loading, error, api, unwrapList }) => ({
    ara: async (query) => {
      loading.value = true
      error.value = null
      try {
        const r = await api.search(query)
        liste.value = unwrapList(r)
        return liste.value
      } catch (err) {
        error.value = err.message
        throw err
      } finally {
        loading.value = false
      }
    },
    filtreliCari: async (params = {}) => {
      loading.value = true
      error.value = null
      try {
        const r = await api.filtreli(params)
        liste.value = unwrapList(r)
        durumlar.toplamKayit.value = r.data?.totalElements ?? liste.value.length
        return liste.value
      } catch (err) {
        error.value = err.message
        throw err
      } finally {
        loading.value = false
      }
    }
  })
})
