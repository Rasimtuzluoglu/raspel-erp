import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { stokAPI } from '../api/index.js'

export const useStokStore = defineStore('stok', () => {
  const stoklar = ref([])
  const loading = ref(false)
  const error = ref(null)
  const toplamKayit = ref(0)

  const getAll = async (params = {}) => {
    loading.value = true
    error.value = null
    try {
      const r = await stokAPI.getAll(params)
      const icerik = r.data.content || r.data
      stoklar.value = Array.isArray(icerik) ? icerik : []
      toplamKayit.value = r.data.totalElements ?? stoklar.value.length
      return stoklar.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const ara = async (q) => {
    loading.value = true
    error.value = null
    try {
      const r = await stokAPI.ara(q)
      stoklar.value = r.data.content || r.data
      return r.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const filtreli = async (params = {}) => {
    loading.value = true
    error.value = null
    try {
      const r = await stokAPI.filtreli(params)
      const icerik = r.data.content || r.data
      stoklar.value = Array.isArray(icerik) ? icerik : []
      toplamKayit.value = r.data.totalElements ?? stoklar.value.length
      return r.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const addStok = async (data) => {
    try {
      const r = await stokAPI.create(data)
      stoklar.value.push(r.data)
      toplamKayit.value++
      return r.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const updateStok = async (id, data) => {
    try {
      const r = await stokAPI.update(id, data)
      const i = stoklar.value.findIndex((s) => s.id === id)
      if (i !== -1) stoklar.value[i] = r.data
      return r.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const deleteStok = async (id) => {
    try {
      await stokAPI.delete(id)
      stoklar.value = stoklar.value.filter((s) => s.id !== id)
      toplamKayit.value--
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const dusukStoklar = computed(() => stoklar.value.filter((s) => s.minMiktar && s.miktar <= s.minMiktar))

  return { stoklar, loading, error, toplamKayit, getAll, ara, filtreli, addStok, updateStok, deleteStok, dusukStoklar }
})
