import { ref, computed } from 'vue'
import { createCrudStore } from './createCrudStore.js'
import { stokAPI } from '../api/index.js'

export const useStokStore = createCrudStore('stok', stokAPI, {
  stateKey: 'stoklar',
  totalKey: 'toplamKayit',
  extraState: { toplamKayit: () => ref(0) },
  afterAdd: (_data, { durumlar }) => durumlar.toplamKayit.value++,
  afterRemove: (_id, { durumlar }) => durumlar.toplamKayit.value--,
  actions: { getAll: 'getAll', add: 'addStok', update: 'updateStok', remove: 'deleteStok' },
  extraActions: ({ liste, durumlar, loading, error, api, unwrapList }) => ({
    ara: async (q) => {
      loading.value = true
      error.value = null
      try {
        const r = await api.ara(q)
        liste.value = unwrapList(r)
        return r.data
      } catch (err) {
        error.value = err.message
        throw err
      } finally {
        loading.value = false
      }
    },
    filtreli: async (params = {}) => {
      loading.value = true
      error.value = null
      try {
        const r = await api.filtreli(params)
        liste.value = unwrapList(r)
        durumlar.toplamKayit.value = r.data?.totalElements ?? liste.value.length
        return r.data
      } catch (err) {
        error.value = err.message
        throw err
      } finally {
        loading.value = false
      }
    },
    dusukStoklar: computed(() => liste.value.filter((s) => s.minMiktar && s.miktar <= s.minMiktar))
  })
})
