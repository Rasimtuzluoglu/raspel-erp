import { createCrudStore } from './createCrudStore.js'
import { kategoriAPI } from '../api/index.js'

export const useKategoriStore = createCrudStore('kategori', kategoriAPI, {
  stateKey: 'kategoriler',
  actions: { getAll: 'getAllKategoriler', add: 'addKategori', update: 'updateKategori', remove: 'deleteKategori' }
})
