# Güvenlik Politikası (SECURITY)

## Güvenlik Açığı Bildirimi

Güvenlik açığı bulduysanız lütfen **public issue açmayın**. Şu adresten iletin:

- E-posta: `security@raspel-erp.com` (varsa)
- Ya da GitHub Security Advisory: https://github.com/Rasimtuzluoglu/raspel-erp/security/advisories

Yanıt süresi: 72 saat içinde ilk geri bildirim hedeflenir.

## Sorumlu Açıklama

- Açığı özel olarak bildirin, yayınlamayın
- Düzeltme yayınlanana kadar bekleyin
- Açığı kötüye kullanmayın

## Üretim Ortamı Kontrol Listesi

### Zorunlu Ortam Değişkenleri (prod)

Aşağıdakiler **zorunludur**; tanımlanmazsa uygulama fail-fast yapar:

```bash
JWT_SECRET=            # en az 256-bit (32 byte) base64 değer
POSTGRES_PASSWORD=
REDIS_PASSWORD=
RABBITMQ_PASSWORD=
GRAFANA_PASSWORD=
```

### Güvenlik Ayarları (prod otomatik)

- JWT cookie: `Secure` + `HttpOnly` + `SameSite=Strict`
- HSTS aktif
- Actuator health detayları: `when-authorized` (gizli)
- `/api/sirketler/aktif` public değil
- Şifre politikası: min 8, max 72, büyük/küçük harf + rakam + özel karakter
- Giriş rate limit: IP başına 5 deneme / 60 sn
- CSV export: formula injection koruması (`=`, `+`, `-`, `@` önekleri)
- Container'lar non-root kullanıcıyla çalışır
- Ağ izolasyonu: frontend-net / backend-net / db-net

### Anahtar Döndürme

- `JWT_SECRET` değerini düzenli döndürün (öneri: 90 günde bir)
- Döndürme sırasında tüm oturumlar geçersiz olur (kullanıcılar yeniden giriş yapar)

## Kapsam Dışı

- Local dev ortamındaki varsayılan şifreler (`admin/admin123` dev seed) üretimde geçerli değildir; `APP_ADMIN_PASSWORD` ile değiştirilmelidir
