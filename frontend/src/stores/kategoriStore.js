import { ref } from 'vue'
import { createCrudStore } from './createCrudStore.js'
import { kategoriAPI } from '../api/index.js'

/**
 * Kategori registry store'u.
 *
 * `getAllKategoriler` geriye uyumludur: sayfali Spring `Page` yanitini de
 * dogrudan dizi yanitini da kabul eder (createCrudStore -> unwrapList).
 * Agac/ozet/secenek/kullanim gibi yeni uclar `extraActions` ile eklenir.
 */
export const useKategoriStore = createCrudStore('kategori', kategoriAPI, {
  stateKey: 'kategoriler',
  actions: {
    getAll: 'getAllKategoriler',
    add: 'addKategori',
    update: 'updateKategori',
    remove: 'deleteKategori'
  },
  extraState: {
    agac: () => ref([]),
    ozet: () => ref({ toplam: 0, aktif: 0, pasif: 0, altKategori: 0 }),
    // kapsam -> KategoriDTO[]; Kasa ekrani ayni secenekleri tekrar tekrar cekmez.
    secenekOnbellek: () => ref({}),
    sistemKategoriler: () => ref([])
  },
  extraActions: ({ api, durumlar }) => {
    const onbellekTemizle = () => {
      durumlar.secenekOnbellek.value = {}
    }

    return {
      onbellekTemizle,

      async agacGetir(params = {}) {
        const r = await api.agac(params)
        const veri = Array.isArray(r.data) ? r.data : []
        durumlar.agac.value = veri
        return veri
      },

      async ozetGetir(params = {}) {
        const r = await api.ozet(params)
        const veri = r.data && !Array.isArray(r.data) ? r.data : {}
        durumlar.ozet.value = { toplam: 0, aktif: 0, pasif: 0, altKategori: 0, ...veri }
        return durumlar.ozet.value
      },

      /**
       * Aktif kategori secenekleri. Ayni kapsam icin ikinci cagri onbellekten
       * doner; `{ zorla: true }` ile tazelenebilir.
       */
      async seceneklerGetir(kapsam, { zorla = false } = {}) {
        if (!kapsam) return []
        const onbellek = durumlar.secenekOnbellek.value
        if (!zorla && Array.isArray(onbellek[kapsam])) return onbellek[kapsam]
        const r = await api.secenekler({ kapsam })
        const veri = Array.isArray(r.data) ? r.data : []
        durumlar.secenekOnbellek.value = { ...onbellek, [kapsam]: veri }
        return veri
      },

      async kullanimGetir(id) {
        const r = await api.kullanim(id)
        return r.data || null
      },

      async arsivle(id) {
        const r = await api.arsivle(id)
        onbellekTemizle()
        return r.data
      },

      async aktiflestir(id) {
        const r = await api.aktiflestir(id)
        onbellekTemizle()
        return r.data
      },

      async birlestir(payload) {
        const r = await api.birlestir(payload)
        onbellekTemizle()
        return r.data
      },

      async sistemKategorileriGetir() {
        const r = await api.sistemKategorileri()
        const veri = Array.isArray(r.data) ? r.data : []
        durumlar.sistemKategoriler.value = veri
        return veri
      }
    }
  }
})
