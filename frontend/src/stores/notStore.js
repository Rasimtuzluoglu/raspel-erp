import { createCrudStore } from './createCrudStore.js'
import { notAPI } from '../api/index.js'

export const useNotStore = createCrudStore('not', notAPI, {
  stateKey: 'notlar',
  addPosition: 'unshift',
  actions: { getAll: 'getAllNotlar', add: 'addNot', update: 'updateNot', remove: 'deleteNot' }
})
