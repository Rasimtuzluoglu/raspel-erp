/**
 * Saha teslimat listesi sıralaması.
 *
 * Kullanıcı kartları elle sıralar; sıra `localStorage`'da id dizisi olarak
 * saklanır. Listede olmayan (yeni atanmış) teslimatlar sona eklenir ve
 * aralarındaki mevcut sıra korunur.
 */
export function sirala(liste, siraIdleri = []) {
  if (!Array.isArray(liste)) return []
  const sira = Array.isArray(siraIdleri) ? siraIdleri : []

  const konum = new Map()
  sira.forEach((id, indeks) => {
    const anahtar = String(id)
    if (!konum.has(anahtar)) konum.set(anahtar, indeks)
  })

  const bilinmeyen = Number.MAX_SAFE_INTEGER
  return liste
    .map((oge, indeks) => ({
      oge,
      indeks,
      konum: konum.has(String(oge?.id)) ? konum.get(String(oge.id)) : bilinmeyen
    }))
    .sort((a, b) => (a.konum - b.konum) || (a.indeks - b.indeks))
    .map((x) => x.oge)
}

/** Listeyi id dizisine çevirir (localStorage'a yazmak için). */
export function siraIdleri(liste) {
  if (!Array.isArray(liste)) return []
  return liste.map((oge) => oge?.id).filter((id) => id !== undefined && id !== null)
}

/** Kaydı bir basamak yukarı/aşağı taşır; sınırda listeyi aynen döndürür. */
export function tasi(liste, id, yon) {
  if (!Array.isArray(liste)) return []
  const indeks = liste.findIndex((oge) => String(oge?.id) === String(id))
  const hedef = indeks + (yon === 'yukari' ? -1 : 1)
  if (indeks < 0 || hedef < 0 || hedef >= liste.length) return [...liste]

  const kopya = [...liste]
  const [kayit] = kopya.splice(indeks, 1)
  kopya.splice(hedef, 0, kayit)
  return kopya
}
