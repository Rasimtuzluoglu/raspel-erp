import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { cariHesapAPI } from '../api/index.js'

/**
 * Cari Hesap Store
 * Cari hesaplara ait state ve actionları yönetir.
 */
export const useCariHesapStore = defineStore('cariHesap', () => {
  const cariHesaplar = ref([])
  const loading = ref(false)
  const error = ref(null)

  /**
   * Tüm cari hesapları getir
   */
  const getAllCariHesaplar = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await cariHesapAPI.getAll()
      cariHesaplar.value = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      console.error('Cari hesaplar yüklenirken hata:', err)
      throw err
    } finally {
      loading.value = false
    }
  }

  /**
   * Yeni cari hesap ekle
   */
  const addCariHesap = async (cariHesap) => {
    try {
      const response = await cariHesapAPI.create(cariHesap)
      cariHesaplar.value.push(response.data)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  /**
   * Cari hesap güncelle
   */
  const updateCariHesap = async (id, cariHesap) => {
    try {
      const response = await cariHesapAPI.update(id, cariHesap)
      const index = cariHesaplar.value.findIndex(c => c.id === id)
      if (index !== -1) {
        cariHesaplar.value[index] = response.data
      }
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  /**
   * Cari hesap sil
   */
  const deleteCariHesap = async (id) => {
    try {
      await cariHesapAPI.delete(id)
      cariHesaplar.value = cariHesaplar.value.filter(c => c.id !== id)
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const ara = async (query) => {
    loading.value = true
    error.value = null
    try {
      const response = await cariHesapAPI.search(query)
      cariHesaplar.value = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  return {
    cariHesaplar,
    loading,
    error,
    getAllCariHesaplar,
    ara,
    addCariHesap,
    updateCariHesap,
    deleteCariHesap
  }
})
