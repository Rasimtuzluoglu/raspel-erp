import { defineStore } from 'pinia'
import { ref } from 'vue'
import { kategoriAPI } from '../api/index.js'

export const useKategoriStore = defineStore('kategori', () => {
  const kategoriler = ref([])
  const loading = ref(false)

  const getAllKategoriler = async () => {
    loading.value = true
    try { const r = await kategoriAPI.getAll(); kategoriler.value = r.data; return r.data }
    finally { loading.value = false }
  }

  const addKategori = async (data) => {
    const r = await kategoriAPI.create(data)
    kategoriler.value.push(r.data)
    return r.data
  }

  const deleteKategori = async (id) => {
    await kategoriAPI.delete(id)
    kategoriler.value = kategoriler.value.filter(k => k.id !== id)
  }

  return { kategoriler, loading, getAllKategoriler, addKategori, deleteKategori }
})
