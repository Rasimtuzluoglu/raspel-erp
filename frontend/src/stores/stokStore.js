import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { stokAPI } from '../api/index.js'

export const useStokStore = defineStore('stok', () => {
  const stoklar = ref([])
  const loading = ref(false)

  const getAll = async () => {
    loading.value = true
    try { const r = await stokAPI.getAll(); stoklar.value = r.data.content || r.data; return r.data }
    finally { loading.value = false }
  }

  const ara = async (q) => {
    loading.value = true
    try { const r = await stokAPI.ara(q); stoklar.value = r.data.content || r.data; return r.data }
    finally { loading.value = false }
  }

  const addStok = async (data) => {
    const r = await stokAPI.create(data); stoklar.value.push(r.data); return r.data
  }

  const updateStok = async (id, data) => {
    const r = await stokAPI.update(id, data)
    const i = stoklar.value.findIndex(s => s.id === id)
    if (i !== -1) stoklar.value[i] = r.data
    return r.data
  }

  const deleteStok = async (id) => {
    await stokAPI.delete(id); stoklar.value = stoklar.value.filter(s => s.id !== id)
  }

  const dusukStoklar = computed(() => stoklar.value.filter(s => s.minMiktar && s.miktar <= s.minMiktar))

  return { stoklar, loading, getAll, ara, addStok, updateStok, deleteStok, dusukStoklar }
})
