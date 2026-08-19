import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * Standart CRUD Pinia store'u ureten fabrika.
 * Liste + loading + error + getAll/add/update/delete akisini merkezilesirir.
 *
 * Kullanim:
 *   export const useFaturaStore = createCrudStore('fatura', faturaAPI)
 *
 * Mevcut view'lari kirmamak icin state/action adlari ozellestirilebilir:
 *   createCrudStore('kasa', kasaAPI, {
 *     stateKey: 'kasalar',
 *     prefix: 'Kasa',
 *     actions: { getAll: 'getAllKasalar', add: 'addKasa', update: 'updateKasa', remove: 'deleteKasa' }
 *   })
 */
export function createCrudStore(name, api, opts = {}) {
  const stateKey = opts.stateKey || 'liste'
  const actionNames = {
    getAll: opts.actions?.getAll || 'getAll',
    add: opts.actions?.add || 'add',
    update: opts.actions?.update || 'update',
    remove: opts.actions?.remove || 'remove'
  }

  return defineStore(name, () => {
    const liste = ref([])
    const loading = ref(false)
    const error = ref(null)

    const getAll = async () => {
      loading.value = true
      error.value = null
      try {
        const r = await api.getAll()
        liste.value = r.data?.content || r.data || []
        return liste.value
      } catch (err) {
        error.value = err.response?.data?.message || err.message
        throw err
      } finally {
        loading.value = false
      }
    }

    const add = async (data) => {
      try {
        const r = await api.create(data)
        liste.value.push(r.data)
        return r.data
      } catch (err) {
        error.value = err.response?.data?.message || err.message
        throw err
      }
    }

    const update = async (id, data) => {
      try {
        const r = await api.update(id, data)
        const idx = liste.value.findIndex((x) => x.id === id)
        if (idx !== -1) liste.value[idx] = r.data
        return r.data
      } catch (err) {
        error.value = err.response?.data?.message || err.message
        throw err
      }
    }

    const remove = async (id) => {
      try {
        await api.delete(id)
        liste.value = liste.value.filter((x) => x.id !== id)
      } catch (err) {
        error.value = err.response?.data?.message || err.message
        throw err
      }
    }

    const expose = {}
    expose[stateKey] = liste
    expose.loading = loading
    expose.error = error
    expose[actionNames.getAll] = getAll
    expose[actionNames.add] = add
    expose[actionNames.update] = update
    expose[actionNames.remove] = remove
    // Jenerik adlar da her zaman erisilebilir olsun
    if (!expose.getAll) expose.getAll = getAll
    if (!expose.add) expose.add = add
    if (!expose.update) expose.update = update
    if (!expose.remove) expose.remove = remove

    return expose
  })
}
