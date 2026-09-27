/**
 * İstemci tarafı görsel sıkıştırma. Yükleme öncesi büyük fotoğrafları (telefon kamerası
 * vb.) tarayıcıda küçültüp JPEG'e çevirir; böylece yükleme hızlı olur ve sunucuda az yer
 * kaplar. Şeffaf PNG'ler ve GIF'ler (animasyon bozulmasın) olduğu gibi bırakılır.
 * Canvas'ın çözemediği formatlarda (ör. HEIC) dosya değiştirilmeden döner; yükleme
 * sunucuda anlaşılır hata mesajıyla reddedilir.
 */

export const SIKISTIRMA_MAX_KENAR = 1600
export const SIKISTIRMA_KALITE = 0.82

export async function resimSikistir(file, {
  maxKenar = SIKISTIRMA_MAX_KENAR,
  kalite = SIKISTIRMA_KALITE
} = {}) {
  if (!file || !file.type || !file.type.startsWith('image/')) return file
  // Animasyonlu GIF ve şeffaf PNG'leri yeniden kodlamak görsel kayba yol açar.
  if (file.type === 'image/gif' || file.type === 'image/png') return file
  if (typeof createImageBitmap !== 'function') return file
  try {
    const bitmap = await createImageBitmap(file)
    const buyuk = Math.max(bitmap.width, bitmap.height)
    const oran = buyuk > maxKenar ? maxKenar / buyuk : 1
    // Zaten küçük ve makul boyutta ise dokunma.
    if (oran === 1 && file.size <= 1.5 * 1024 * 1024) {
      if (bitmap.close) bitmap.close()
      return file
    }
    const genislik = Math.max(1, Math.round(bitmap.width * oran))
    const yukseklik = Math.max(1, Math.round(bitmap.height * oran))
    const canvas = document.createElement('canvas')
    canvas.width = genislik
    canvas.height = yukseklik
    const ctx = canvas.getContext('2d')
    ctx.fillStyle = '#ffffff'
    ctx.fillRect(0, 0, genislik, yukseklik)
    ctx.drawImage(bitmap, 0, 0, genislik, yukseklik)
    if (bitmap.close) bitmap.close()
    const blob = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', kalite))
    if (!blob || blob.size >= file.size) return file
    const ad = (file.name || 'foto').replace(/\.[^.]+$/, '') + '.jpg'
    return new File([blob], ad, { type: 'image/jpeg', lastModified: Date.now() })
  } catch {
    // Canvas çözemedi (ör. HEIC): dosyayı olduğu gibi bırak; sunucu karar verir.
    return file
  }
}
