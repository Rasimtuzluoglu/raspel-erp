# Çek/Senet Revizyonu Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Çek/senet yaşam döngüsünü (portföy/teminat/ciro/tahsil/protesto) izlenebilir, geri alınabilir ve raporlanabilir hale getirmek.

**Architecture:** V171 ile hareket bağları + durum logu eklenir; servis geçiş matrisi ve tahsil iptali mevcut `hareketIptal` üzerine kurulur. Frontend KPI + 3 sekme + takvim + drawer ile yenilenir.

**Tech Stack:** Java 21, Spring Boot 3.5, PostgreSQL/Flyway, JUnit 5/Mockito, Vue 3, PrimeVue 4, Vitest.

**Spec:** `docs/superpowers/specs/2026-10-28-cek-senet-revizyonu-design.md`

## Global Constraints

- Migration **V171**; mevcut V170 korunur.
- Durumlar: `PORTFOY, TEMINATTA, CIRO_EDILDI, TAHSIL_EDILDI, PROTESTO, IADE`; legacy `ODENDI → TAHSIL_EDILDI`.
- Tüm geçişler tenant + dönem kilidi + `cek_senet_durum_log`.
- Tahsil iptali yalnız ADMIN/MUHASEBE, gerekçe zorunlu, `TAHSIL_EDILDI → PORTFOY`.
- Ciro finansal hareket oluşturmaz (belge takibi).
- i18n tr/en simetrik; her faz sonunda tam kapılar.

## Review Focus

- Tahsil iptalinde cari/fatura/kasa-banka tüm etkilerinin dönmesi.
- Kasa/banka hareket id’lerinin kaybolmaması (ters kayıt için).
- Çift iptal / terminal protesto guard’ı.
- Legacy ODENDI kayıtlarının doğru eşlenmesi.
- Takvim 62 gün sınırı ve boş günler.

---

### Task 1: V171 + durum log + tahsil bağları
**Files:** migration V171, entity `CekSenet`/`CekSenetDurumLog`, repo, DTO, service.
- [ ] Test: tahsilde hareket/kasa/banka id’leri kaydedilir; log yazılır; legacy ODENDI eşlenir.
- [ ] Migration: alanlar + log tablosu + indeksler (idempotent).
- [ ] `durumGuncelle` tahsil dalını id’leri kaydedecek şekilde düzenle; kasa/banka `kaynakTip=CEK_SENET`.
- [ ] Pass: `mvn -B test -Dtest="CekSenetServiceTest,CekSenetControllerTest" -q`
- [ ] Commit: `feat(cek-senet): V171 durum logu ve tahsil baglantilari`

### Task 2: Tahsil iptali + ciro/teminat/protesto
**Files:** DTO’lar, `CekSenetService`, controller, testler.
- [ ] Testler: iptal bakiyeleri, ikinci iptal, dönem kilidi, geçiş matrisi, ciro hedefi, teminat iade, protesto.
- [ ] `POST /{id}/tahsil-iptal`, `/ciro`, `/ciro-iade`, `/iade-al`, `/teminat`, `/teminat-iade`, `/protesto`.
- [ ] `HareketService.hareketIptal` + kasa/banka ters kayıt; durum PORTFOY; log.
- [ ] Pass + commit: `feat(cek-senet): tahsil iptali, ciro, teminat ve protesto`

### Task 3: Filtre/özet/takvim/Excel
**Files:** repo JPQL, `CekSenetOzetDTO`, `CekSenetTakvimDTO`, controller, Excel.
- [ ] Testler: filtreler, özet sayıları, takvim aralık sınırı, Excel.
- [ ] `GET /filtreli`, `/ozet`, `/takvim?baslangic&bitis` (max 62 gün), `/exports/cek-senet`.
- [ ] Pass + commit: `feat(cek-senet): filtreli liste, ozet, takvim ve excel`

### Task 4: Frontend
**Files:** `api/modules/finans.js`, `dosya.js`, `views/CekSenet.vue`, `components/CekSenetDetayDrawer.vue`, i18n, testler.
- [ ] Testler: 5 KPI, 3 sekme, filtre/hafıza, takvim/aging, drawer, tahsil+iptal, ciro/teminat/protesto, Excel.
- [ ] PageHeader + KPI + TabView (Portföy/Vade Takvimi/Geçmiş); sunucu filtreli tablo; aylık takvim + aging kartları; drawer; aksiyon modalları; mobil/a11y.
- [ ] Pass: eslint + i18n + vitest ilgili + build.
- [ ] Commit: `feat(cek-senet): KPI, sekmeler, takvim, drawer ve aksiyon UI`

### Task 5: Kapanış
- [ ] `npm run lint && npm run i18n:check && npm run test && npm run build`
- [ ] `mvn -B clean verify`
- [ ] `docker compose build backend frontend && docker compose up -d backend frontend`
- [ ] V171 success; yeni uçlar 401; health; bundle.
- [ ] `git add -A && git commit -m "feat(cek-senet): revizyon (Faz 1-5)" && git push origin main`

## Self-Review
- Spec §2–§5 → Task 1–2; §6 → Task 3; §7 → Task 4; §9 → Task 5.
- Tipler: `tahsilatHareketId`, `kasaHareketId`, `bankaHareketiId`, `ciroCariId`, `gecmis[]`, `CekSenetTakvimDTO`.
- Review Focus testleri Task 1–3’e bağlandı.
