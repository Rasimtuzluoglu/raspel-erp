# Katkı Rehberi (CONTRIBUTING)

RasPel ERP'e katkıda bulunmak için teşekkürler! Aşağıdaki kurallara uyarak süreci hızlandırabilirsiniz.

## Geliştirme Ortamı

```bash
# Altyapı
docker-compose up -d postgres redis rabbitmq

# Backend (port 8081)
cd backend && mvn spring-boot:run

# Frontend (port 5173)
cd frontend && npm ci && npm run dev
```

## Kod Standardları

### Backend (Java 21 / Spring Boot 3.2)

- Paket yapısı: `controller/service/repository/entity/dto` altında domain bazlı paketler
- Lombok kullanın: `@RequiredArgsConstructor`, `@Slf4j`, `@Builder`
- Tenant izolasyonu: `sirketId`'yi **asla** request body/query'den almayın; `request.getAttribute("sirketId")` kullanın
- Yazma işlemlerinde `tenantChecker.checkSirketId(dto.getSirketId(), ...)` çağırın
- Stok/kasa mutasyonlarında `findByIdForUpdate` (pessimistic lock) kullanın
- Cache'i değiştiren servislerde `CacheYardimci.temizle(...)` çağırın
- Test: `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`
- Derleme: `mvn -B clean verify` (510+ test geçmeli)

### Frontend (Vue 3 / Composition API)

- Tüm bileşenler `<script setup>` kullanır
- API: `src/api/index.js` üzerinden import edin
- State: Pinia store'ları; CRUD store'ları için `createCrudStore` factory'ini kullanın
- i18n: Yeni metinlerde `$t()` kullanmaya çalışın (varsayılan Türkçe)
- Lint: `npm run lint` — sıfır hata olmalı
- Test: `npm run test` (Vitest)
- Format: `npm run format` (Prettier)

## Branch & PR Süreci

1. `main`'den yeni branch açın: `feature/adi`
2. Değişiklikleri yapın, testleri yazın/güncelleyin
3. CI'da tüm kontroller geçmeli: backend test + frontend lint/test/build + güvenlik taraması
4. PR açın, kısa ve açıklayıcı Türkçe/İngilizce açıklama ekleyin

## Commit Mesajları

Örnek: `fix: fatura kesildiginde stok cache'i temizlenmiyordu`

## Test Yazma

- Backend controller testleri için `@WebMvcTest` + `@Import(TestSecurityMocks.class)` desenini kopyalayın
- Service testleri için saf Mockito kullanın (Spring context gerekmez)
- Tenant izolasyonu testi: `TenantCheckerTest` desenini takip edin
- Repository entegrasyon testleri için `PostgresEntegrasyonTest` (Testcontainers) desenini kopyalayın
