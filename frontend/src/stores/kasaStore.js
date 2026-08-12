import { createCrudStore } from './createCrudStore.js'
import { kasaAPI } from '../api/index.js'

export const useKasaStore = createCrudStore('kasa', kasaAPI, {
  stateKey: 'kasalar',
  actions: { getAll: 'getAllKasalar', add: 'addKasa', update: 'updateKasa', remove: 'deleteKasa' }
})
