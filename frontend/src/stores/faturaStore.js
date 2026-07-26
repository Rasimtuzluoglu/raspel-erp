import { defineStore } from 'pinia'
import { ref } from 'vue'
import { faturaAPI } from '../api/index.js'

export const useFaturaStore = defineStore('fatura', () => {
  const faturalar = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getAllFaturalar = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await faturaAPI.getAll()
      faturalar.value = response.data?.content || response.data || []
      return faturalar.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const getFaturaById = async (id) => {
    try {
      const response = await faturaAPI.getById(id)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const addFatura = async (fatura) => {
    try {
      const response = await faturaAPI.create(fatura)
      faturalar.value.unshift(response.data)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const updateDurum = async (id, durum) => {
    try {
      const response = await faturaAPI.updateDurum(id, durum)
      const index = faturalar.value.findIndex(f => f.id === id)
      if (index !== -1) faturalar.value[index] = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const updateFatura = async (id, data) => {
    try {
      const response = await faturaAPI.update(id, data)
      const index = faturalar.value.findIndex(f => f.id === id)
      if (index !== -1) faturalar.value[index] = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const deleteFatura = async (id) => {
    try {
      await faturaAPI.delete(id)
      faturalar.value = faturalar.value.filter(f => f.id !== id)
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  return {
    faturalar, loading, error,
    getAllFaturalar, getFaturaById, addFatura, updateFatura, updateDurum, deleteFatura
  }
})
