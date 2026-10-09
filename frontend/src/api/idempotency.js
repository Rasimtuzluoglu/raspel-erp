/**
 * Idempotency anahtarı üretir.
 *
 * Sunucu tarafında (IdempotencyService) aynı anahtarla gelen ikinci istek
 * reddedilir; böylece çift tıklama, ağ titremesi veya kullanıcının "Tekrar
 * Dene" demesi mükerrer finansal kayıt oluşturmaz.
 *
 * Kullanım kuralı: anahtar KULLANICI EYLEMİ başına üretilir. Bir form
 * açıldığında bir kez üretilir ve o formun tüm yeniden denemelerinde aynı
 * anahtar kullanılır; başarıdan sonra yeni bir anahtar üretilir. Çağrı başına
 * anahtar üretmek (her seferinde `idempotencyAnahtari()` çağırmak) korumayı
 * anlamsız kılar, çünkü iki istek farklı anahtar görür.
 */
export function idempotencyAnahtari() {
  try {
    if (typeof crypto !== 'undefined' && crypto.randomUUID) return crypto.randomUUID()
  } catch {
    /* yoksay */
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2)}`
}