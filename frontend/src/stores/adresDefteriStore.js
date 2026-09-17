import { createCrudStore } from './createCrudStore.js'
import { adresDefteriAPI } from '../api/index.js'

export const useAdresDefteriStore = createCrudStore('adresDefteri', adresDefteriAPI, {
  stateKey: 'kisiler',
  actions: { getAll: 'getAllKisiler', add: 'addKisi', update: 'updateKisi', remove: 'deleteKisi' }
})
