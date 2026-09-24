/**
 * İstemci tarafı dosya doğrulama yardımcıları. Güvenlik sunucu tarafında uygulanır;
 * bunlar kullanıcı deneyimini iyileştirmek ve gereksiz istekleri önlemek için
 * savunma derinliği sağlar.
 */

export const MAKS_DOSYA_BOYUTU = 5 * 1024 * 1024 // 5 MB

export const IZINLI_RESIM_MIME = ['image/jpeg', 'image/png', 'image/webp', 'image/gif']

/**
 * Resim dosyasını doğrular. Geçerliyse null, aksi halde kullanıcıya gösterilecek
 * hata mesajı (anahtar) döndürür.
 * @returns {{ key: string, params?: object } | null}
 */
export function resimDogrula(file, { maksBoyut = MAKS_DOSYA_BOYUTU } = {}) {
  if (!file) return { key: 'dosya.dosyaSecilmedi' }
  if (file.size > maksBoyut) return { key: 'dosya.boyutAsildi', params: { mb: Math.round(maksBoyut / 1024 / 1024) } }
  if (file.type && !IZINLI_RESIM_MIME.includes(file.type.toLowerCase())) {
    return { key: 'dosya.gecersizResimTipi' }
  }
  return null
}

/** Genel dosya boyutu doğrulaması. */
export function boyutDogrula(file, { maksBoyut = MAKS_DOSYA_BOYUTU } = {}) {
  if (!file) return { key: 'dosya.dosyaSecilmedi' }
  if (file.size > maksBoyut) return { key: 'dosya.boyutAsildi', params: { mb: Math.round(maksBoyut / 1024 / 1024) } }
  return null
}
