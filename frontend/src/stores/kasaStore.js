import { defineStore } from 'pinia'
import { ref } from 'vue'
import { kasaAPI } from '../api/index.js'

export const useKasaStore = defineStore('kasa', () => {
  const kasalar = ref([])
  const loading = ref(false)

  const getAllKasalar = async () => {
    loading.value = true
    try { const r = await kasaAPI.getAll(); kasalar.value = r.data?.content || r.data || []; return kasalar.value }
    finally { loading.value = false }
  }

  const addKasa = async (data) => {
    const r = await kasaAPI.create(data)
    kasalar.value.push(r.data)
    return r.data
  }

  const updateKasa = async (id, data) => {
    const r = await kasaAPI.update(id, data)
    const i = kasalar.value.findIndex(k => k.id === id)
    if (i !== -1) kasalar.value[i] = r.data
    return r.data
  }

  const deleteKasa = async (id) => {
    await kasaAPI.delete(id)
    kasalar.value = kasalar.value.filter(k => k.id !== id)
  }

  return { kasalar, loading, getAllKasalar, addKasa, updateKasa, deleteKasa }
})
