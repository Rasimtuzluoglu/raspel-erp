import { defineStore } from 'pinia'
import { ref } from 'vue'
import { notAPI } from '../api/index.js'

export const useNotStore = defineStore('not', () => {
  const notlar = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getAllNotlar = async (params) => {
    loading.value = true
    error.value = null
    try {
      const r = await notAPI.getAll(params)
      notlar.value = r.data?.content || r.data || []
      return notlar.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const addNot = async (data) => {
    const r = await notAPI.create(data)
    notlar.value.unshift(r.data)
    return r.data
  }

  const updateNot = async (id, data) => {
    const r = await notAPI.update(id, data)
    const idx = notlar.value.findIndex((n) => n.id === id)
    if (idx !== -1) notlar.value[idx] = r.data
    return r.data
  }

  const deleteNot = async (id) => {
    await notAPI.delete(id)
    notlar.value = notlar.value.filter((n) => n.id !== id)
  }

  return { notlar, loading, error, getAllNotlar, addNot, updateNot, deleteNot }
})
