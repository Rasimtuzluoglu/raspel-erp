import { defineStore } from 'pinia'
import { ref } from 'vue'
import { unwrapList } from '../api/utils/unwrap.js'

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
 *     actions: { getAll: 'getAllKasalar', add: 'addKasa', update: 'updateKasa', remove: 'deleteKasa' }
 *   })
 *
 * Desteklenen ek secenekler:
 *   addPosition: 'push' | 'unshift'   -> yeni kaydin listeye eklenme yonu (varsayilan push)
 *   totalKey: 'toplamKayit'           -> getAll sonrasi data.totalElements bu state'e yazilir
 *   extraState: { toplamKayit: () => ref(0) }  -> ek state'ler
 *   extraActions: (ctx) => ({ ara, filtreli }) -> ek action'lar (liste/loading/error/durumlar erisimi)
 *   afterAdd / afterRemove            -> ek sayac/hook islemleri
 */
export function createCrudStore(name, api, opts = {}) {
  const stateKey = opts.stateKey || 'liste'
  const actionNames = {
    getAll: opts.actions?.getAll || 'getAll',
    add: opts.actions?.add || 'add',
    update: opts.actions?.update || 'update',
    remove: opts.actions?.remove || 'remove'
  }
  const addPosition = opts.addPosition || 'push'

  return defineStore(name, () => {
    const liste = ref([])
    const loading = ref(false)
    const error = ref(null)

    const durumlar = {}
    for (const [k, factory] of Object.entries(opts.extraState || {})) {
      durumlar[k] = factory()
    }

    const getAll = async (...args) => {
      loading.value = true
      error.value = null
      try {
        const r = await api.getAll(...args)
        liste.value = unwrapList(r)
        if (opts.totalKey && durumlar[opts.totalKey]) {
          durumlar[opts.totalKey].value = r.data?.totalElements ?? liste.value.length
        }
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
        if (addPosition === 'unshift') liste.value.unshift(r.data)
        else liste.value.push(r.data)
        if (opts.afterAdd) opts.afterAdd(r.data, { liste, durumlar })
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
        if (opts.afterRemove) opts.afterRemove(id, { liste, durumlar })
      } catch (err) {
        error.value = err.response?.data?.message || err.message
        throw err
      }
    }

    const expose = { ...durumlar }
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

    if (opts.extraActions) {
      Object.assign(
        expose,
        opts.extraActions({ liste, loading, error, durumlar, getAll, add, update, remove, api, unwrapList })
      )
    }

    return expose
  })
}
