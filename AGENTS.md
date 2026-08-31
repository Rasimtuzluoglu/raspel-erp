# AGENTS.md

## Project Overview

**RasPel ERP** is a full-stack enterprise resource planning system. Turkish-language-first with English i18n support.

## Quick Commands

```bash
# Backend (Java 21 + Spring Boot 3.2 + Maven)
cd backend
mvn -B compile -q          # Compile
mvn -B test -q             # Run 821 tests (H2 in-memory)
mvn -B clean verify        # Full build with tests
mvn spring-boot:run        # Run dev server on :8081

# Frontend (Vue 3 + Vite + PrimeVue 4)
cd frontend
npm ci                      # Install deps
npm run dev                 # Dev server :5173
npm run build               # Production build
npm run test                # Run 158 tests (Vitest)
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
| Tests | JUnit 5 (821) + Vitest (158) + Cypress |

## Project Structure

```
raspel-erp/
├── backend/src/main/java/com/raspel/erp/
│   ├── controller/          # REST controllers by domain
│   │   ├── envanter/        # Stok, StokSeri, StokSayim
│   │   ├── finans/          # Banka, Kasa, Cari, Hareket, CekSenet, Butce, Masraf, Doviz
│   │   ├── ik/              # Personel, Izin, Puantaj, Maas, Vardiya
│   │   ├── muhasebe/        # Muhasebe, Irsaliye
│   │   ├── sistem/          # Kullanici, Sirket, Donem, Yetki, Audit, Not, Proje, Dashboard, Yedek, Import, Excel, PDF, Belge, Anomali
│   │   ├── sube/            # Sube, Depo
│   │   └── ticaret/         # Fatura, EFatura, Siparis, Satinalma, Iade, FiyatListesi, Crm
│   ├── service/             # Business logic (mirrors controller structure)
│   ├── repository/          # JPA repositories (mirrors service structure)
│   ├── entity/              # JPA entities (mirrors repository structure)
│   ├── dto/                 # Data transfer objects
│   ├── config/security/     # Spring Security, JWT filter, rate limiter
│   ├── aspect/              # AOP audit logging
│   ├── exception/           # BusinessException, ResourceNotFoundException
│   └── util/                # TOTP utility
│
├── frontend/src/
│   ├── api/
│   │   ├── client.js         # Axios instance + interceptors
│   │   ├── index.js          # Re-exports all API modules
│   │   └── modules/          # Domain-based API modules
│   │       ├── finans.js, ticaret.js, stok.js, ik.js, sistem.js, rapor.js, dosya.js
│   ├── stores/               # Pinia stores (auth, banka, cari, fatura, stok, etc.)
│   ├── views/                # 57 views (lazy-loaded)
│   ├── components/            # 25+ shared components
│   ├── composables/           # 12 composables
│   ├── router/               # Vue Router with auth guards
│   ├── locales/              # i18n (tr.json, en.json)
│   └── assets/
│
├── config/                   # Infrastructure configs
│   ├── traefik/              # Reverse proxy + SSL
│   ├── prometheus/           # Metrics + alerts
│   └── grafana/              # Dashboards
│
├── scripts/                  # Operations (backup, restore, load test)
└── docs/                     # API, usage, installation docs
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

- Backend: 821 tests (JUnit 5, H2, Mockito), must pass before commit
- Frontend: 158 tests (Vitest), zero ESLint warnings required
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

