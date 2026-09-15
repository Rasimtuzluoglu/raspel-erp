/**
 * Axios yanitlarindan veriyi tek noktadan cikarir.
 *
 * Spring Data `Page<T>` yanitlari `data.content` altinda listeyi tasir; bazi
 * endpoint'ler ise dogrudan dizi/nesne dondurur. View/store'larda tekrar eden
 * `r.data?.content || r.data || []` deseninin yerini alir.
 *
 * @param {unknown} response Axios yaniti (veya benzeri `{ data }` nesnesi)
 * @param {unknown} fallback Veri yoksa dondurulecek deger
 */
export function unwrap(response, fallback) {
  const data = response?.data
  const deger = data?.content || data
  return deger == null ? fallback : deger
}

/** Liste beklenen yerler icin: her zaman bir dizi dondurur. */
export function unwrapList(response) {
  const deger = unwrap(response)
  return Array.isArray(deger) ? deger : []
}
