import { ref, onUnmounted } from 'vue'
import { cariHesapAPI } from '../api'
import { unwrapList } from '../api/utils/unwrap.js'

// Sunucu taraflı cari araması.
//
// Neden ayrı bir composable? Birden fazla view cari seçici barındırıyor
// (fatura, cari hareketi, cari ekstre raporu, stok tedarikçisi, POS müşterisi).
// Hepsi `getAllCariHesaplar()` ile ilk açılışta tüm şirketi çekiyordu; istek
// parametresiz olduğu için backend'in `@PageableDefault(size = 50)` tavanına
// takılıyor ve 50. kayıttan sonraki hiçbir cari seçilemiyordu. Bu composable
// listeyi sayfa taşımadan tutar: her tuş vuruşunda sunucudan ilk N sonuç
// gelir, kullanıcı ne aradığını yazar.

const VARSAYILAN_BOYUT = 20
const VARSAYILAN_GECIKME_MS = 250

export function useCariOnerileri({
  boyut = VARSAYILAN_BOYUT,
  gecikmeMs = VARSAYILAN_GECIKME_MS,
  ekParams = {}
} = {}) {
  const oneriler = ref([])
  const yukleniyor = ref(false)
  let zamanlayici = null
  let seq = 0

  const sorguyuYukle = async (sorgu) => {
    const benimSeq = ++seq
    yukleniyor.value = true
    try {
      const params = { page: 0, size: boyut, ...ekParams }
      // Backend parametre adı `q` (CariHesapController.filtreli). `search`
      // göndermek sessizce filtreyi düşürüyordu: her arama filtresiz ilk sayfayı
      // getiriyor, yani kullanıcı yazdıkça liste değişmiyordu.
      if (sorgu) params.q = sorgu
      const r = await cariHesapAPI.filtreli(params)
      // Ağ gecikmesi nedeniyle eski istek yeni sorgunun sonucunu ezmesin.
      if (benimSeq !== seq) return
      oneriler.value = unwrapList(r)
    } catch {
      if (benimSeq === seq) oneriler.value = []
    } finally {
      if (benimSeq === seq) yukleniyor.value = false
    }
  }

  // AutoComplete/Dropdown `@complete` olayı için: { query } gelir.
  const ara = (event) => {
    const q = (event?.query ?? event ?? '').toString().trim()
    if (zamanlayici) clearTimeout(zamanlayici)
    zamanlayici = setTimeout(() => sorguyuYukle(q), gecikmeMs)
  }

  // Debounce beklemeden, programatik çağrı (açılışta öneri listesi, temizleme).
  const hemenAra = (sorgu = '') => {
    if (zamanlayici) clearTimeout(zamanlayici)
    return sorguyuYukle((sorgu ?? '').toString().trim())
  }

  onUnmounted(() => {
    if (zamanlayici) clearTimeout(zamanlayici)
  })

  return { oneriler, yukleniyor, ara, hemenAra }
}

// Seçicilerden bağımsız tekil cari çözümü. Derin bağlantıda
// (`route.query.cariHesapId`) veya bir kaydı düzenlerken cari liste tıklığıyla
// aranamayabilir; önce önbelleğe bakılır, bulunamazsa sunucudan tek kayıt
// çekilir. Bu olmadan `find` null döner ve form sessizce boş kalır.
export async function cariHesapCoz(id, onbellek = []) {
  if (id == null || id === '') return null
  const onbellekteki = onbellek.find((c) => c?.id === id)
  if (onbellekteki) return onbellekteki
  try {
    const r = await cariHesapAPI.getById(id)
    return r?.data ?? null
  } catch {
    return null
  }
}