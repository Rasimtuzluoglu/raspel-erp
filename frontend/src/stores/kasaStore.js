import { defineStore } from 'pinia'
import { ref } from 'vue'
import { kasaAPI } from '../api/index.js'

export const useKasaStore = defineStore('kasa', () => {
  const kasalar = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getAllKasalar = async () => {
    loading.value = true
    error.value = null
    try { const r = await kasaAPI.getAll(); kasalar.value = r.data?.content || r.data || []; return kasalar.value }
    catch (err) { error.value = err.message; throw err }
    finally { loading.value = false }
  }

  const addKasa = async (data) => {
    try {
      const r = await kasaAPI.create(data)
      kasalar.value.push(r.data)
      return r.data
    } catch (err) { error.value = err.message; throw err }
  }

  const updateKasa = async (id, data) => {
    try {
      const r = await kasaAPI.update(id, data)
      const i = kasalar.value.findIndex(k => k.id === id)
      if (i !== -1) kasalar.value[i] = r.data
      return r.data
    } catch (err) { error.value = err.message; throw err }
  }

  const deleteKasa = async (id) => {
    try {
      await kasaAPI.delete(id)
      kasalar.value = kasalar.value.filter(k => k.id !== id)
    } catch (err) { error.value = err.message; throw err }
  }

  return { kasalar, loading, error, getAllKasalar, addKasa, updateKasa, deleteKasa }
})

