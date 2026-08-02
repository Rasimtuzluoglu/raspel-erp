import { defineStore } from 'pinia'
import { ref } from 'vue'
import { kategoriAPI } from '../api/index.js'

export const useKategoriStore = defineStore('kategori', () => {
  const kategoriler = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getAllKategoriler = async () => {
    loading.value = true
    error.value = null
    try { const r = await kategoriAPI.getAll(); kategoriler.value = r.data?.content || r.data || []; return kategoriler.value }
    catch (err) { error.value = err.message; throw err }
    finally { loading.value = false }
  }

  const addKategori = async (data) => {
    try {
      const r = await kategoriAPI.create(data)
      kategoriler.value.push(r.data)
      return r.data
    } catch (err) { error.value = err.message; throw err }
  }

  const deleteKategori = async (id) => {
    try {
      await kategoriAPI.delete(id)
      kategoriler.value = kategoriler.value.filter(k => k.id !== id)
    } catch (err) { error.value = err.message; throw err }
  }

  return { kategoriler, loading, error, getAllKategoriler, addKategori, deleteKategori }
})

