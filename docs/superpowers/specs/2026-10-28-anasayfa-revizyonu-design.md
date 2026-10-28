# Anasayfa (Dashboard) Revizyonu — Tasarım Spesifikasyonu

**Tarih:** 2026-10-28
**Kapsam:** Premium komuta merkezi, bento düzen, görev merkezi, içgörüler, parçalı veri yükleme, kişiselleştirme
**Birincil kullanıcı:** Yönetici / muhasebe / operasyon kullanıcısı
**Durum:** Tasarım onaylandı

## 1. Amaç

Mevcut anasayfa tek dev endpoint'ten tüm veriyi bekleyen, 11 grafik ve çok sayıda kartın eşit ağırlıkta olduğu yoğun bir ekran. Yeni anasayfa:

- İlk boyada karar verilebilir KPI'lar ve **bugün yapılacaklar** gösterir.
- Veriyi **parçalı ve progresif** yükler; grafikler/akış sonradan gelir.
- **Rol bazlı varsayılan** düzen sunar, kullanıcı sürükle-bırak ile kişiselleştirir.
- Grafiklerden ilgili modüle **drill-down** sağlar.
- Canlı işlem akışı ve kural tabanlı içgörülerle "ne yapmalıyım?" sorusunu yanıtlar.

## 2. Bilgi mimarisi

| Bölge | İçerik | Varsayılan |
|---|---|---|
| Hero | Karşılama, kullanıcı/şirket, tarih-saat, canlı rozet, bugün şeridi (tahsilat/ödeme/sipariş/teslimat), döviz ticker, Ctrl+K, yenile, düzenle | Her rol |
| KPI (bento) | Likidite (sparkline), Net cari (alacak/borç), Aylık ciro (hedef %), Aylık net kâr (marj) | Her rol |
| Görev merkezi | Onay bekleyenler, vadesi geçen, kritik stok, bekleyen teslimat, vadesi yaklaşan | Her rol (rol filtreli) |
| İçgörüler | 3 kural tabanlı içgörü (INFO/WARN/KRITIK) + bağlantı | Her rol |
| Hedefler | Ciro hedefi halkası, kâr hedefi çubuğu, 30/60/90 nakit projeksiyonu | ADMIN, MUHASEBE |
| Grafikler (ana) | 30 gün nakit akışı, 12 ay gelir/gider, kategori satışları, en çok satan ürünler | Her rol |
| Canlı akış | Onay/tahsilat/ödeme/fatura/sipariş/stok olayları zaman çizelgesi | Her rol |
| Tüm analizler | Yaşlandırma, en borçlu/alacaklı, kasa-banka, pareto, ciro hedef | ADMIN, MUHASEBE (kapalı) |
| Vade & kritik stok | Vadesi geçen/yaklaşan listeleri, kritik stok kartları | ADMIN, MUHASEBE |
| Hızlı işlemler | 8 kutucuk + Ctrl+K komut paleti | Her rol |

**Rol varsayılanları**
- `ADMIN`: tüm bölümler; sıra: hero → KPI → görevler → içgörüler → hedefler → nakit akışı → gelir/gider → akış → kategori → ürünler → vade → kritik stok → hızlı işlemler.
- `MUHASEBE`: finans odaklı; görevler (onay+vade), hedefler, nakit akışı, gelir/gider, vade öne.
- `USER`: görevler, canlı akış, hızlı işlemler, kritik stok, en çok satanlar; şirket geneli kâr hedefi gizli.

## 3. Backend

### 3.1 V174 — kullanıcı tercihi
```
sistem.dashboard_tercih (
  id BIGSERIAL PK,
  kullanici_id BIGINT NOT NULL UNIQUE,
  sirket_id BIGINT NOT NULL,
  duzen TEXT NOT NULL,             -- JSON
  guncelleme_tarihi TIMESTAMP NOT NULL DEFAULT NOW()
)
+ index (sirket_id)
```
- `duzen` en fazla 20 KB, geçerli JSON object; backend `ObjectMapper.readTree` ile doğrular.
- Tenant: `sirket_id` eşleşmeli.

### 3.2 Uçlar (`/api/dashboard`, ADMIN/USER)

| Uç | Dönüş | Açıklama |
|---|---|---|
| `GET /ozet` | `DashboardOzetDTO` | Hero, bugün, 4 KPI, stok özeti, görev sayaçları |
| `GET /gorevler` | `DashboardGorevDTO[]` | Rol filtreli, öncelik sıralı görevler |
| `GET /grafikler` | `DashboardGrafikDTO` | Ana + genişletilmiş grafik verileri |
| `GET /akis?limit=20` | `DashboardAkisDTO[]` | Birleşik olay akışı |
| `GET /icgoruler` | `DashboardIcgoruDTO[]` | Kural tabanlı 3 içgörü |
| `GET /tercihler` | `DashboardTercihDTO` | Kullanıcı düzeni (yoksa `duzen=null`) |
| `PUT /tercihler` | `DashboardTercihDTO` | Kaydet (upsert) |
| `GET /` | `DashboardDTO` | **Geriye uyum** için mevcut payload korunur |

**DashboardOzetDTO**
```
hero: {kullaniciAd, sirketAd, canli}
bugun: {tahsilat, odeme, siparis, teslimat}
kpi: {
  likidite: {deger, kasa, banka, trendYuzde, sparkline[]},
  netCari: {deger, alacak, borc},
  ciro: {deger, hedef, yuzde},
  kar: {deger, hedef, marj}
}
stok: {cesit, deger, kritikAdet}
gorevSayilari: {onayBekleyen, vadesiGecen, kritikStok, bekleyenTeslimat, vadesiYaklasan}
```

**DashboardGorevDTO**
```
{tur, baslik, aciklama, adet, tutar, oncelik(0-100), route, ikon, renk}
```
Türler: `ONAY_IZIN, ONAY_MASRAF, ONAY_SATINALMA, ONAY_SIPARIS, VADESI_GECEN, VADESI_YAKLASAN, KRITIK_STOK, BEKLEYEN_TESLIMAT`.

**DashboardGrafikDTO**
```
aylikGelirGider[], gunlukNakitAkisi[], enCokSatanlar[], kategoriSatislari[],
alacakYaslandirma[], enCokBorcCariler[], enCokAlacakCariler[],
kasaBanka{kasa,banka}, hedef{ciro,gerceklesen,kar,hedefKar}
```

**DashboardAkisDTO**
```
{tur, baslik, aciklama, tutar, tarih, route, ikon, renk}
```
Kaynaklar: onay kararları (`sistem.onay_karar`), hareketler, faturalar, siparişler, stok hareketleri; her kaynaktan son N, birleştirilip tarih desc, limit.

**DashboardIcgoruDTO**: `{seviye(INFO/WARN/KRITIK), baslik, mesaj, route, ikon}`

**DashboardTercihDTO**: `{duzen(Map), guncellemeTarihi}`

### 3.3 Rol ve yetki
- Rol: `SecurityContextHolder` authority'lerinden (`ROLE_ADMIN`, `ROLE_MUHASEBE`, `ROLE_USER`).
- Görevler: onay görevleri yalnız ADMIN/MUHASEBE; USER için vade/kritik stok/teslimat.
- Tercih: yalnız kendi kaydı (kullaniciId).
- Tenant: `sirketId` request attribute.

### 3.4 Cache
- `@Cacheable("dashboard", key=...)` uçlar için; TTL mevcut Redis yapılandırmasına bırakılır.
- Tercih cache'lenmez.
- Mevcut `/api/dashboard` cache anahtarı korunur.

## 4. Frontend

### 4.1 Bileşenler
- `views/Dashboard.vue` — kabuk, veri orkestrasyonu, düzen modu.
- `components/dashboard/DashboardHero.vue`
- `components/dashboard/DashboardKpiGrid.vue`
- `components/dashboard/DashboardGorevMerkezi.vue`
- `components/dashboard/DashboardIcgoruKarti.vue`
- `components/dashboard/DashboardHedefler.vue`
- `components/dashboard/DashboardGrafikKarti.vue` — başlık, alt bağlantı, skeleton, boş durum, drill-down.
- `components/dashboard/DashboardAkis.vue`
- `components/dashboard/DashboardHizliIslemler.vue`
- `components/dashboard/DashboardWidget.vue` — sürükleme tutamacı, boyut, gizleme, klavye sıralama.
- `components/dashboard/DashboardDuzenPaneli.vue` — widget listesi, boyut, sıfırla.

### 4.2 Görsel dil
- Hero: yumuşak gradyan mesh + cam efekt; koyu/açık temada okunur.
- KPI: bento kart, büyük rakam, delta rozeti, sparkline; hover'da hafif yükselme.
- Bölüm başlıkları: ikon + başlık + sağda aksiyon/bağlantı.
- Grafikler: mevcut `useChartTema` paleti; yuvarlatılmış bar, gradyan dolgu.
- Animasyonlar: `prefers-reduced-motion` ile kapanır; 150-250 ms geçişler.
- Skeleton: bölüm bazlı, layout shift yok.

### 4.3 Kişiselleştirme
- Düzen anahtarı: `{versiyon:1, sira:[{key, boyut:1|2, gorunur}]}`.
- “Düzenle” modunda: sürükle-bırak (native HTML5 `draggable`), yukarı/aşağı butonları (klavye), boyut 1x/2x, gizle.
- Kaydet: `PUT /dashboard/tercihler`; başarısızsa localStorage fallback (`raspel_dashboard_duzen`).
- “Rol varsayılanına sıfırla”.

### 4.4 Drill-down rotaları
| Grafik | Rota |
|---|---|
| Nakit akışı | `/kasa` (hareketler) |
| Gelir/gider | `/raporlar` |
| Kategori satışları | `/raporlar` |
| En çok satan ürünler | `/stoklar` |
| Yaşlandırma | `/tahsilat` |
| En borçlu/alacaklı | `/cari-hesaplar` |
| Vade kartları | `/tahsilat` |
| Kritik stok | `/stoklar?kritik=1` |

### 4.5 Komut paleti
- Mevcut `QuickSearch` (Ctrl+K, App.vue) yeniden kullanılır.
- Dashboard’daki “Ara / komut” butonu `window.dispatchEvent(new CustomEvent('raspel:hizli-ara'))` yayar; `App.vue` dinleyip açar.

### 4.6 A11y / mobil
- 12 kolon desktop → 6 tablet → 1 mobil; KPI 2’li/1’li.
- Grafik kartları klavye ile odaklanabilir, `aria-label` ve “detaya git” bağlantısı.
- `aria-live="polite"` yenileme ve düzen kaydı duyuruları.
- Sürüklemenin yanında klavye ile sıralama; odak halkası korunur.
- Dokunma hedefleri ≥ 40 px.

## 5. Test

**Backend**
- V174 migration (kolonlar, unique, index) + tercih upsert/tenant/JSON doğrulama.
- `/ozet`: KPI hesapları, bugün şeridi, görev sayaçları, rol.
- `/gorevler`: türler, öncelik sırası, rol filtresi, boş liste.
- `/grafikler`: mevcut hesaplarla tutarlılık, boş veri.
- `/akis`: birleştirme, sıralama, limit, tenant.
- `/icgoruler`: eşikler ve seviyeler.
- Legacy `/api/dashboard` değişmedi.
- Cache anahtarları ve yetki.

**Frontend**
- Hero, KPI, görev merkezi, içgörü, hedefler, grafik kartı, akış, hızlı işlemler.
- Progresif yükleme: ozet hemen, diğer bölümler skeleton ile.
- Rol bazlı varsayılan düzen.
- Sürükle-bırak + klavye sıralama + boyut + gizle + sıfırla + kaydetme/fallback.
- Drill-down rotaları.
- Komut paleti tetikleme.
- i18n tr/en simetrisi, a11y, mobil, `prefers-reduced-motion`.

**Kapılar:** ESLint 0, i18n, Vitest + kapsam, `mvn -B clean verify`, Docker, V174 doğrulama.

## 6. Riskler

| Risk | Karar |
|---|---|
| Parçalı uçlarda veri tutarsızlığı | Tüm uçlar aynı servis katmanını kullanır; özet ile grafik arasında kısa süreli fark kabul edilir |
| Cache bayatlığı | Kısa TTL + manuel yenile; kritik sayfalar cache anahtarını temizler |
| Sürükle-bırak karmaşıklığı | Native HTML5; klavye alternatifi zorunlu |
| Rol yetkisi sızıntısı | Görevler ve tercih backend'de role/tenant ile filtrelenir |
| Eski `/api/dashboard` tüketicileri | Uç korunur, şekli değişmez |
| Grafik yükü | Ana 4 grafik hemen, diğerleri “Tüm analizler” açılınca |
| JSON tercih bozulması | Boyut + JSON doğrulama; geçersizde 400, frontend varsayılana döner |
