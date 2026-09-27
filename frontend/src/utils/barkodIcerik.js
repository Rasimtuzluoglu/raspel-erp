// Etiket QR/barkod icerigi icin tek kaynak; backend EtiketIcerikUtil ile ayni
// kural: barkod -> stok kodu -> STK{id}. CODE128 ASCII sinirlidir; Turkce
// karakterler translitere edilir. Onizleme ve PDF ayni degeri kodlar.
const ESLEME = {
  İ: 'I', ı: 'i', Ş: 'S', ş: 's', Ğ: 'G', ğ: 'g',
  Ü: 'U', ü: 'u', Ö: 'O', ö: 'o', Ç: 'C', ç: 'c'
}

export const asciiSadelestir = (ham) => {
  if (ham == null) return ''
  return String(ham)
    .split('')
    .map((c) => ESLEME[c] ?? c)
    .filter((c) => c >= ' ' && c <= '~')
    .join('')
    .trim()
}

/** Insan okur etiket kodu (etiket metinleri ve QR icerigi). */
export const etiketGosterim = (stok) => {
  if (!stok) return ''
  const barkod = String(stok.barkod ?? '').trim()
  if (barkod) return barkod
  const stokKodu = String(stok.stokKodu ?? '').trim()
  if (stokKodu) return stokKodu
  return stok.id != null ? `STK${stok.id}` : ''
}

/** CODE128 icerigi: ASCII'ye sadelestirilmis etiket kodu. */
export const barkodIcerik = (stok) => {
  const sade = asciiSadelestir(etiketGosterim(stok))
  if (sade) return sade
  return stok?.id != null ? `STK${stok.id}` : 'STK'
}
