# AGENTS.md

## Project Overview

**RasPel ERP** is a full-stack enterprise resource planning system. Turkish-language-first with English i18n support.

## Quick Commands

```bash
# Backend (Java 21 + Spring Boot 3.2 + Maven)
cd backend
mvn -B compile -q          # Compile
mvn -B test -q             # Run 678 tests (H2 in-memory)
mvn -B clean verify        # Full build with tests
mvn spring-boot:run        # Run dev server on :8081

# Frontend (Vue 3 + Vite + PrimeVue 4)
cd frontend
npm ci                      # Install deps
npm run dev                 # Dev server :5173
npm run build               # Production build
npm run test                # Run 153 tests (Vitest)
npm run lint                # ESLint
npm run cypress:run         # E2E tests

# Full stack with Docker
docker-compose up -d        # Full production setup (9 containers)
docker-compose up -d postgres redis rabbitmq  # Dev minimum
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21, Spring Boot 3.2, Maven |
| Database | PostgreSQL 16 + Flyway migrations |
| Cache | Redis 7 |
| Queue | RabbitMQ 3 |
| Frontend | Vue 3, Vite 5, PrimeVue 4, Pinia |
| Auth | JWT + BCrypt + TOTP 2FA |
| Container | Docker Compose (9 services) |
| CI/CD | GitHub Actions |
| Tests | JUnit 5 (678) + Vitest (153) + Cypress |

## Project Structure

```
raspel-erp/
â”œâ”€â”€ backend/src/main/java/com/raspel/erp/
â”‚   â”œâ”€â”€ controller/          # REST controllers by domain
â”‚   â”‚   â”œâ”€â”€ envanter/        # Stok, StokSeri, StokSayim
â”‚   â”‚   â”œâ”€â”€ finans/          # Banka, Kasa, Cari, Hareket, CekSenet, Butce, Masraf, Doviz
â”‚   â”‚   â”œâ”€â”€ ik/              # Personel, Izin, Puantaj, Maas, Vardiya
â”‚   â”‚   â”œâ”€â”€ muhasebe/        # Muhasebe, Irsaliye
â”‚   â”‚   â”œâ”€â”€ sistem/          # Kullanici, Sirket, Donem, Yetki, Audit, Not, Proje, Dashboard, Yedek, Import, Excel, PDF, Belge, Anomali
â”‚   â”‚   â”œâ”€â”€ sube/            # Sube, Depo
â”‚   â”‚   â””â”€â”€ ticaret/         # Fatura, EFatura, Siparis, Satinalma, Iade, FiyatListesi, Crm
â”‚   â”œâ”€â”€ service/             # Business logic (mirrors controller structure)
â”‚   â”œâ”€â”€ repository/          # JPA repositories (mirrors service structure)
â”‚   â”œâ”€â”€ entity/              # JPA entities (mirrors repository structure)
â”‚   â”œâ”€â”€ dto/                 # Data transfer objects
â”‚   â”œâ”€â”€ config/security/     # Spring Security, JWT filter, rate limiter
â”‚   â”œâ”€â”€ aspect/              # AOP audit logging
â”‚   â”œâ”€â”€ exception/           # BusinessException, ResourceNotFoundException
â”‚   â””â”€â”€ util/                # TOTP utility
â”‚
â”œâ”€â”€ frontend/src/
â”‚   â”œâ”€â”€ api/
â”‚   â”‚   â”œâ”€â”€ client.js         # Axios instance + interceptors
â”‚   â”‚   â”œâ”€â”€ index.js          # Re-exports all API modules
â”‚   â”‚   â””â”€â”€ modules/          # Domain-based API modules
â”‚   â”‚       â”œâ”€â”€ finans.js, ticaret.js, stok.js, ik.js, sistem.js, rapor.js, dosya.js
â”‚   â”œâ”€â”€ stores/               # Pinia stores (auth, banka, cari, fatura, stok, etc.)
â”‚   â”œâ”€â”€ views/                # 57 views (lazy-loaded)
â”‚   â”œâ”€â”€ components/            # 25+ shared components
â”‚   â”œâ”€â”€ composables/           # 12 composables
â”‚   â”œâ”€â”€ router/               # Vue Router with auth guards
â”‚   â”œâ”€â”€ locales/              # i18n (tr.json, en.json)
â”‚   â””â”€â”€ assets/
â”‚
â”œâ”€â”€ config/                   # Infrastructure configs
â”‚   â”œâ”€â”€ traefik/              # Reverse proxy + SSL
â”‚   â”œâ”€â”€ prometheus/           # Metrics + alerts
â”‚   â””â”€â”€ grafana/              # Dashboards
â”‚
â”œâ”€â”€ scripts/                  # Operations (backup, restore, load test)
â””â”€â”€ docs/                     # API, usage, installation docs
```

## Backend Conventions

- **Package structure**: Domain-based sub-packages under controller/service/repository/entity/dto
- **Lombok**: `@RequiredArgsConstructor`, `@Slf4j`, `@Builder` throughout
- **Multi-tenant**: `HttpServletRequest.getAttribute("sirketId")` for tenant isolation
- **Auth**: `@PreAuthorize("hasAnyRole('ADMIN','USER')")` at controller level
- **Caching**: `@Cacheable`/`@CacheEvict` on services (Redis)
- **Security**: JWT stateless sessions, BCrypt passwords, TOTP 2FA, login rate limiting
- **API prefix**: All controllers use `/api/`
- **DB schemas**: sistem, cari, stok, fatura, siparis, satinalma, muhasebe, personel, proje, sube, finans, ticaret, envanter, ik

## Frontend Conventions

- **Composition API**: All components use `<script setup>`
- **Global components**: PrimeVue components registered in `main.js` (DataTable, Button, Dialog, etc.)
- **API access**: Import from `../api/index.js` (centralized Axios client)
- **State**: Pinia stores with `defineStore`
- **Routing**: Lazy-loaded views in `router/index.js`, auth guards check `authStore.isLoggedIn`
- **i18n**: `$t()` in templates, `useI18n()` in composables
- **Styling**: PrimeVue Lara theme + `app.css` for overrides, dark/light theme via `useTheme`

## Code Quality

- Backend: 678 tests (JUnit 5, H2, Mockito), must pass before commit
- Frontend: 153 tests (Vitest), zero ESLint warnings required
- CI runs on push/PR to main: backend (compile+test), frontend (lint+test+build), security (Trivy)

## Dev Setup (Minimal)

```bash
# Terminal 1: Infrastructure
docker-compose up -d postgres redis rabbitmq

# Terminal 2: Backend
cd backend
mvn spring-boot:run

# Terminal 3: Frontend
cd frontend
npm run dev
```

Access: Frontend http://localhost:5173, Backend API http://localhost:8081, Adminer http://localhost:8082

