import { createCrudStore } from './createCrudStore.js'
import { bankaAPI } from '../api/index.js'

export const useBankaStore = createCrudStore('banka', bankaAPI, {
  stateKey: 'bankalar',
  actions: { getAll: 'getAllBankalar', add: 'addBanka', update: 'updateBanka', remove: 'deleteBanka' }
})
