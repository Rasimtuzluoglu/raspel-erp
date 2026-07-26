import { defineStore } from 'pinia'
import { ref } from 'vue'
import { bankaAPI } from '../api/index.js'

export const useBankaStore = defineStore('banka', () => {
  const bankalar = ref([])
  const loading = ref(false)
  const error = ref(null)

  const getAllBankalar = async () => {
    loading.value = true
    error.value = null
    try {
      const response = await bankaAPI.getAll()
      bankalar.value = response.data?.content || response.data || []
      return bankalar.value
    } catch (err) {
      error.value = err.message
      throw err
    } finally {
      loading.value = false
    }
  }

  const addBanka = async (banka) => {
    try {
      const response = await bankaAPI.create(banka)
      bankalar.value.push(response.data)
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const updateBanka = async (id, banka) => {
    try {
      const response = await bankaAPI.update(id, banka)
      const index = bankalar.value.findIndex(b => b.id === id)
      if (index !== -1) bankalar.value[index] = response.data
      return response.data
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  const deleteBanka = async (id) => {
    try {
      await bankaAPI.delete(id)
      bankalar.value = bankalar.value.filter(b => b.id !== id)
    } catch (err) {
      error.value = err.message
      throw err
    }
  }

  return {
    bankalar, loading, error,
    getAllBankalar, addBanka, updateBanka, deleteBanka
  }
})
