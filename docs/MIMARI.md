# RasPel ERP — Mimari Dokümanı

## Genel Bakış

- **Backend**: Java 21, Spring Boot 3.2, Spring Security (JWT), Spring Data JPA, Redis (cache), RabbitMQ (async), Flyway (DB migration)
- **Frontend**: Vue 3 (Composition API), Vite 6, PrimeVue 4, Pinia, Vue Router
- **Veritabanı**: PostgreSQL 16, çok şemalı (`sistem`, `cari`, `stok`, `fatura`, `finans`, ...)

## Katman Mimarisi

```
HTTP → JwtAuthFilter → Controller → Service → Repository → PostgreSQL
                          │            │
                          │            └─ CacheYardimci → Redis
                          └─ AuditAspect (AOP) → audit_log
```

- **Controller**: HTTP giriş noktası. `sirketId`'yi JWT'den `request.getAttribute("sirketId")` ile alır.
- **Service**: İş mantığı. `@Transactional`, `@Cacheable`/`@CacheEvict`, `TenantChecker` doğrulaması.
- **Repository**: JPA derived queries + `@Query`. Pessimistic lock için `findByIdForUpdate`.
- **DTO**: Request/response nesneleri. `entityToDTO` mapping'i servislerde.

## Multi-Tenant Modeli

Her kayıt `sirket_id` kolonu taşır. Tenant izolasyonu şu katmanlarda sağlanır:

1. **JWT claim**: Token `sirketId` claim'i taşır; `JwtAuthFilter` bunu request attribute'a koyar.
2. **Controller**: `sirketId`'yi JWT'den okur, client'tan asla kabul etmez.
3. **Service**: 
   - Okuma: repository tenant-filtreli sorgular (`findBySirketId...`)
   - Yazma: `tenantChecker.checkSirketId(dto.getSirketId(), ...)` DTO'daki değerin JWT ile eşleştiğini doğrular
   - Entity okuma: `tenantChecker.check(entity.getSirketId(), ...)` farklı tenanta erişimi engeller
4. **Giriş**: `girisSirket` — USER yalnızca atandığı firmada oturum açar; ADMIN tüm aktif firmaları seçebilir.

## Giriş Akışı

```
1. POST /api/kullanicilar/giris (username + password)
   → Kimlik doğrulanır, girisToken üretilir, kullanıcının firmaları döner
2. (2FA aktifse) POST /api/kullanicilar/giris-2fa → yeni girisToken + firmalar
3. POST /api/kullanicilar/giris-sirket (girisToken + sirketId)
   → Üyelik doğrulanır → JWT + Secure cookie döner
```

## Cache Stratejisi (Redis)

| Cache | TTL | Evict |
|-------|-----|-------|
| `dashboard` | 2 dk | stok/cari mutasyonlarında |
| `cariHesaplar` | 10 dk | bakiye güncellemelerinde |
| `faturalar` | 5 dk | fatura CRUD |
| `stoklar` | 10 dk | stok hareketlerinde (fatura/irsaliye/iade) |
| `lookup` | 30 dk | lookup verileri |

**Önemli**: Stok miktarını değiştiren servisler (FaturaService, IrsaliyeService, IadeService, StokService) `CacheYardimci.temizle("stoklar", "dashboard")` çağırmalıdır — aksi halde eski stok görünür.

## Eşzamanlılık & Locking

- Stok düşme/ekleme işlemleri `StokRepository.findByIdForUpdate(id)` (PESSIMISTIC_WRITE) kullanır → oversell engellenir.
- Kasa bakiye güncellemeleri `KasaRepository.findByIdForUpdate` kullanır.
- `@Lock` yalnızca repository metotlarında çalışır; service seviyesinde kullanmayın (etkisizdir).

## Audit Log

- `AuditAspect` (AOP) controller katmanındaki `olustur/guncelle/sil/durumGuncelle` çağrılarını `audit_log` tablosuna yazar.
- `sirketId` ve `kullaniciId` request attribute'lerinden alınır; hatalı işlemler de `HATA` olarak kaydedilir.
- Denetim görünümü tenant-filtreli sorgular kullanır.

## Güvenlik

- JWT stateless; `Secure` cookie (prod), `HttpOnly`, `SameSite=Strict`
- `JwtAuthFilter` her istekte kullanıcının `active` durumunu kontrol eder
- Giriş rate limit: IP başına 5 deneme / 60 sn
- Şifre politikası: 8-72 karakter, büyük/küçük harf, rakam, özel karakter
- Actuator health detayları `when-authorized`
- Security headers: HSTS, X-Frame-Options (deny)

## Mesajlaşma (RabbitMQ)

RabbitMQ bağlantısı mevcut ancak aktif kuyruk tüketicileri modül bazında genişletilebilir (bildirim, e-posta, rapor).

## Test Stratejisi

| Katman | Araç | Desen |
|--------|------|-------|
| Controller | JUnit + MockMvc | `@WebMvcTest` + `@Import(TestSecurityMocks.class)` |
| Service | JUnit + Mockito | `@ExtendWith(MockitoExtension.class)` |
| Repository | Testcontainers | Gerçek PostgreSQL + Flyway |
| Tenant | TenantCheckerTest | MockHttpServletRequest |
| Frontend | Vitest | Store/component/birim testleri |

CI: `mvn clean verify` (JaCoCo coverage) + `npm run lint` + `npm run test -- --coverage` + `npm run build` + Trivy + Gitleaks + `npm audit`.
