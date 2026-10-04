import { ref, onUnmounted } from 'vue'
import { stokAPI } from '../api'
import { unwrapList } from '../api/utils/unwrap.js'

// Sunucu taraflı stok araması.
//
// Neden ayrı composable? Onlarca view'da stok seçici `<Dropdown>`'u, liste
// `getAll({ size: 1000 })` ile dolduruluyordu. Bu hem her sayfa açılışında
// gereksiz ağ trafiği demek hem de 1000. stoktan sonrasının seçilememesi:
// kullanıcı aradığı ürünü listede bulamayınca "stok yok" sanıyor. Bazı
// ekranlarda tavan daha da yüksekti (`StokSayim.vue` size=5000).
//
// `useCariOnerileri` cari için aynı problemi çözüyor; bu onun stok karşılığı.

const VARSAYILAN_BOYUT = 50
const VARSAYILAN_GECIKME_MS = 250

export function useStokOnerileri({
  boyut = VARSAYILAN_BOYUT,
  gecikmeMs = VARSAYILAN_GECIKME_MS,
  minKacHarf = 0
} = {}) {
  const oneriler = ref([])
  const yukleniyor = ref(false)
  let zamanlayici = null
  let seq = 0

  const sorguyuYukle = async (sorgu) => {
    // Kısa sorgular sunucuya gitmez; `LIKE '%q%'` her tuşta katalog taramak
    // yerine minimum eşik beklemek hem hızlı hem anlamlı sonuç verir.
    if (minKacHarf > 0 && sorgu.length < minKacHarf) {
      seq++
      oneriler.value = []
      yukleniyor.value = false
      return
    }
    const benimSeq = ++seq
    yukleniyor.value = true
    try {
      const r = await stokAPI.ara(sorgu)
      // Ağ gecikmesi nedeniyle eski istek yeni sorgunun sonucunu ezmesin.
      if (benimSeq !== seq) return
      oneriler.value = unwrapList(r).slice(0, boyut)
    } catch {
      if (benimSeq === seq) oneriler.value = []
    } finally {
      if (benimSeq === seq) yukleniyor.value = false
    }
  }

  // PrimeVue `Dropdown`'un `@filter` olayı `{ filter }`, AutoComplete'ın
  // `@complete` olayı `{ query }` taşır; ikisi de desteklenir.
  const ara = (event) => {
    const q = (event?.filter ?? event?.query ?? event ?? '').toString().trim()
    if (zamanlayici) clearTimeout(zamanlayici)
    zamanlayici = setTimeout(() => sorguyuYukle(q), gecikmeMs)
  }

  // Debounce beklemeden programatik çağrı (dropdown ilk açılışta boş görünmesin).
  const hemenAra = (sorgu = '') => {
    if (zamanlayici) clearTimeout(zamanlayici)
    return sorguyuYukle((sorgu ?? '').toString().trim())
  }

  onUnmounted(() => {
    if (zamanlayici) clearTimeout(zamanlayici)
  })

  return { oneriler, yukleniyor, ara, hemenAra }
}