# RasPel ERP — API Kılavuzu

Swagger UI: `http://localhost:8081/swagger-ui.html` (production: `/swagger-ui.html`)

## Kimlik Doğrulama

Tüm endpoint'ler JWT Bearer token ister (giriş hariç).

```bash
# Giriş yap
curl -X POST http://localhost:8081/api/kullanicilar/giris \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123","companyName":"ABC Ön Muhasebe"}'

# Yanıttaki token'ı kullan
curl http://localhost:8081/api/cari-hesaplar \
  -H "Authorization: Bearer <token>"
```

> **Güvenlik:** Giriş başına IP başına 5 deneme / 60 saniye limiti vardır (429).

## Sayfalama

Liste endpoint'leri `page` ve `size` parametrelerini destekler:
```
/api/cari-hesaplar?page=0&size=50
```
Yanıt: `{ "content": [...], "totalElements": 123, "totalPages": 3, ... }`

## Ana Endpoint'ler

### Finans
| Metot | Yol | Açıklama |
|---|---|---|
| GET | `/api/cari-hesaplar` | Cari listesi (sayfalı) |
| POST | `/api/cari-hesaplar` | Cari oluştur |
| GET | `/api/cari-hesaplar/search?q=` | Cari ara |
| GET/PUT/DELETE | `/api/cari-hesaplar/{id}` | Detay / güncelle / sil |
| GET | `/api/faturalar` | Fatura listesi |
| POST | `/api/faturalar` | Fatura oluştur (no otomatik) |
| GET | `/api/bankalar`, `/api/kasalar` | Banka/kasa listeleri |
| GET | `/api/butceler`, `/api/masraflar` | Bütçe/masraf |

### Ticaret & Envanter
| Metot | Yol | Açıklama |
|---|---|---|
| POST | `/api/siparisler` | Sipariş oluştur (no otomatik) |
| GET | `/api/stoklar?page=&size=` | Stok listesi (sayfalı) |
| POST | `/api/stoklar` | Stok oluştur |
| GET | `/api/stoklar/ara?q=` | Stok ara |
| GET | `/api/depolar`, `/api/irsaliyeler`, `/api/iadeler` | Depo/irsaliye/iade |

### Rapor & Denetim
| Metot | Yol | Açıklama |
|---|---|---|
| GET | `/api/dashboard` | Dashboard özeti (2 dk cache) |
| GET | `/api/raporlar/cari-ekstre` | Cari ekstre |
| GET | `/api/raporlar/gelir-gider` | Gelir/gider raporu |
| GET | `/api/rapor/fatura/{id}` | Fatura PDF |
| GET | `/api/rapor/siparis/{id}` | Sipariş PDF |
| GET | `/api/audit-log` | Denetim logları (filtreli) |
| GET | `/api/anomaliler` | Anomali taraması |

### Dosya & Belge
| Metot | Yol | Açıklama |
|---|---|---|
| POST | `/api/import/stok` | CSV ile toplu stok (multipart) |
| POST | `/api/import/cari` | CSV ile toplu cari |
| POST | `/api/belgeler/yukle` | Kayda belge iliştir |
| GET | `/api/belgeler/kayit/{entityAdi}/{id}` | Kaydın belgeleri |
| POST | `/api/upload/sirket-logo` | Şirket logosu (PDF'lere gömülür) |

### Yedekleme
| Metot | Yol | Açıklama |
|---|---|---|
| POST | `/api/backups/manual?type=DAILY` | Manuel yedek |
| GET | `/api/backups` | Yedek listesi |
| GET | `/api/backups/download/{dosya}` | Yedek indir |
| DELETE | `/api/backups/{dosya}` | Yedek sil |

## Ortak Hata Formatı

```json
{ "timestamp": "2026-08-02T10:00:00", "message": "İnsan dostu mesaj", "status": 400 }
```

| HTTP | Anlam |
|---|---|
| 400 | Geçersiz istek / doğrulama hatası |
| 401 | Token yok / geçersiz |
| 403 | Yetki yok |
| 404 | Kaynak bulunamadı |
| 409 | Veri çakışması (mükerrer kayıt) |
| 429 | Rate limit aşıldı |

## WebSocket Bildirimleri

- Adres: `ws://localhost:8081/ws` (STOMP)
- Abonelik: `/topic/bildirimler/{sirketId}`
- Bildirim formatı: `{ tur, baslik, mesaj, tarih }`
