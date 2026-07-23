import { defineStore } from 'pinia'
import { ref } from 'vue'
import { hareketAPI } from '../api/index.js'

export const useHareketStore = defineStore('hareket', () => {
  const hareketler = ref([])
  const sonHareketler = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getHareketlerByCariHesap = async (cariHesapId) => {
    loading.value = true
    error.value = null
    try {
      const response = await hareketAPI.getByCariHesap(cariHesapId)
      hareketler.value = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const getSonHareketler = async (limit = 5) => {
    loading.value = true
    error.value = null
    try {
      const response = await hareketAPI.getSon(limit)
      sonHareketler.value = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const getAllHareketler = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await hareketAPI.getAll()
      hareketler.value = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const addHareket = async (hareket) => {
    try {
      const response = await hareketAPI.create(hareket)
      hareketler.value.push(response.data)
      await getSonHareketler(5)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const updateHareket = async (id, hareket) => {
    try {
      const response = await hareketAPI.update(id, hareket)
      const index = hareketler.value.findIndex(h => h.id === id)
      if (index !== -1) hareketler.value[index] = response.data
      await getSonHareketler(5)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const deleteHareket = async (id) => {
    try {
      await hareketAPI.delete(id)
      hareketler.value = hareketler.value.filter(h => h.id !== id)
      sonHareketler.value = sonHareketler.value.filter(h => h.id !== id)
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  return {
    hareketler,
    sonHareketler,
    loading,
    error,
    getHareketlerByCariHesap,
    getSonHareketler,
    getAllHareketler,
    addHareket,
    updateHareket,
    deleteHareket
  }
})
