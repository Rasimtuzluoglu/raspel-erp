import { defineStore } from 'pinia'
import { ref } from 'vue'
import { depoAPI } from '../api/index.js'

/**
 * Depolar operasyon listesi: filtre + sayfalama durumu.
 * Sunucu tarafli sayfalama; istemci yalniz gecerli sayfayi tutar.
 */
export const useDepoStore = defineStore('depo', () => {
  const filtre = ref({ q: '', subeId: null, aktif: null, kritik: false })
  const sayfa = ref(0)
  const boyut = ref(20)
  const liste = ref([])
  const toplam = ref(0)
  const yukleniyor = ref(false)
  const hata = ref('')
  let aramaZamanlayici = null

  const yukle = async () => {
    yukleniyor.value = true
    hata.value = ''
    try {
      const r = await depoAPI.sayfali({
        q: filtre.value.q || undefined,
        subeId: filtre.value.subeId || undefined,
        aktif: filtre.value.aktif ?? undefined,
        kritik: filtre.value.kritik || undefined,
        page: sayfa.value,
        size: boyut.value
      })
      liste.value = r.data?.content || []
      toplam.value = Number(r.data?.totalElements || 0)
    } catch (e) {
      liste.value = []
      toplam.value = 0
      hata.value = e?.response?.data?.message || 'Depolar yüklenemedi'
    } finally {
      yukleniyor.value = false
    }
  }

  const filtreDegistir = (partial, gecikmeli = false) => {
    filtre.value = { ...filtre.value, ...partial }
    sayfa.value = 0
    if (aramaZamanlayici) clearTimeout(aramaZamanlayici)
    if (gecikmeli) {
      aramaZamanlayici = setTimeout(() => yukle(), 300)
    } else {
      yukle()
    }
  }

  const sayfaDegistir = (n) => {
    sayfa.value = Math.max(0, n)
    yukle()
  }

  const boyutDegistir = (n) => {
    boyut.value = n
    sayfa.value = 0
    yukle()
  }

  const sifirla = () => {
    filtre.value = { q: '', subeId: null, aktif: null, kritik: false }
    sayfa.value = 0
    return yukle()
  }

  return {
    filtre, sayfa, boyut, liste, toplam, yukleniyor, hata,
    yukle, filtreDegistir, sayfaDegistir, boyutDegistir, sifirla
  }
})
