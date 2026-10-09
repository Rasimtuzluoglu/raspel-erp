# ROADMAP — RasPel ERP

> Sürüm geçmişi için `CHANGELOG.md` dosyasına bakın. Bu belge planlanan çalışmaları takip eder.

## Güncel Durum (v1.36.2)

- Testler: **~1600 backend** (JUnit 5 + H2/Postgres entegrasyon) + **814+ frontend** (Vitest) — tümü yeşil; JaCoCo/Vitest coverage gate aktif.
- Migration: **V1..V160** uygulanıyor.
- E2E: **12 Cypress spec** CI'da (dev-server üzerinde).
- Hedef kurulum: 8GB RAM VPS / 10 kullanıcı / ~5000 işlem-gün; backend bellek tavanı `BACKEND_MEM_LIMIT` (öneri 2g, min 1.5g).
- Aşağıdaki epikler tarihsel kayıttır; tamamlanan kalemler işaretlidir.

## Pilot Sertleştirme Çalışması (2026-10)

Uygulama işleyişine etki eden eksikler beş fazda giderildi (dağıtım/Docker kapsam dışı):

- **Faz 0 — Güvenlik/doğrulama:** `TenantChecker` fail-closed, HTTP'te JWT `sirketId` üyelik doğrulaması, XFF sertleştirme, DRIVER sipariş kısıtı, export yetkileri, stok yazma yetkileri, `PersonelIzin` onaylayan sunucudan, `StokSayim`/`Teklif` durum muhafazası, atomik 2FA + şifre sıfırlama, `@Digits`/`@Valid`.
- **Faz 1 — Cari teknik:** mükerrer VKN (V154), tenant-filtreli sorgular, stok tenant doğrulaması, sınırlı uçlar, hareket CSV filtresi, bakiye semantiği.
- **Faz 2 — Cari özellikleri:** risk/kredi limiti + proaktif uyarı, tarih aralıklı ekstre, toplu güncelleme, kalıcı etiket (V155), çoklu adres (V156), hareket soft iptal (V157), hareket import, açılış fişi/devir (V158), cari para birimi (V159).
- **Faz 3 — Sohbet/Ajanda/İletişim:** WS abonelik güvenliği (oda üyeliği + sil topic), oda rolleri (V160), oda mesajları cursor pagination, sidebar rozetleri, Mail & WhatsApp modülü.
- **Faz 4 — AI/tasarım:** `AiService` soyutlaması, dashboard AI özeti, kurumsal tasarım token'ları (Tailwind + PrimeVue), Dashboard view ayrıştırması. (SSE yapısal akış ve kalan büyük view parçalama açık.)
- **Faz 5 — Doküman/altyapı:** ROADMAP güncel duruma çekildi; `/api/v1/*` için geriye uyumlu alias filtresi (`ApiVersionAliasFilter`) eklendi; `backend/sonar-project.properties` (opsiyonel SonarQube analizi) eklendi. Kalan uzun vadeli başlıklar: HizliSatis/Stoklar/Teklifler view'larının tam parçalanması, SpotBugs, S3/GDrive/Dropbox bulut yedek sağlayıcıları.

## Sistem Geneli İyileştirme (2026-10)

Kullanıcının saydığı 15 madde üç fazda ele alınıyor (her faz sonunda yeşil kapı: `lint` · `i18n:check` · `test` · `build`; gerekiyorsa Docker rebuild).

### Faz 1 — Bağımsız düşük riskli düzeltmeler (tamamlandı)

| # | Madde | Yapılan |
|---|---|---|
| 12a | İletişim WhatsApp boş mesaj | `Iletisim.vue` konu+mesajı `?text=` olarak ekler; buton mesaj/telefon yoksa pasif. Sayfa iki kolon + hazır şablon + cari iletişim kartı + karakter sayacı ile "premium" hale getirildi. |
| 13 | AI Yönetici Özeti taşıma | `AiOzetKarti` `Dashboard.vue`'dan `YoneticiKokpiti.vue`'ne taşındı ve mevcut deterministik AI insight kutusuyla **tek kartta** birleştirildi. |
| 5 | Gizlilik + Kullanım birleştirme | `YasalMetinler.vue` (sekmeli) + paylaşılan `YasalIcerik.vue`; eski rotalar `?sekme=`'ye yönlendirir; iki eski view silindi. |
| 8 | Toplu stok sekmesi | `/toplu-stok` rotası + menü + dosya kaldırıldı; `rafNo` desteği `POST /api/import/stok`'a taşındı (kabiliyet kaybı yok). |
| 2 | Hareketler işlem ikonları | 3 satır içi ikon → tek `SatirEylemleri` "..." menüsü (aria-label, uygulama geneli desen). |
| 1 | Denetim geliştirme | Tarih ön ayarı artık listeyi yeniler; kayıtlı filtreler ISO-güvenli + sil/yeniden-kullan + mükerrer engel + cap(20); kullanıcı (`kullaniciId`) filtresi; `islem` çevirileri; tek boş durum; **backend `findDistinct*` tenant-scoped** (çapraz-tenant sızıntı kapatıldı). |

Doğrulama: `lint` temiz · `i18n:check` temiz (4961=4961) · `test` **1483/1483** (95 dosya) · backend `AuditLogControllerTest`+`VeriImportControllerTest` 9/9 · `build` başarılı · `docs-sync` (views 75, components 77) · Docker frontend+backend yeniden derlendi ve sağlıklı.

### Faz 2 — Orta kapsam (tamamlandı)

| # | Madde | Yapılan |
|---|---|---|
| 9 | Stok grubu dropdown | Filtre `AutoComplete` → yalnızca mevcut değerli **Dropdown** (her tuşta değil, seçimde filtreler); toplu fiyat diyaloğuna filtresiz `stokGruplari`; gereksiz `stokGrubuOnerileri` kaldırıldı. |
| 10 | Sipariş takip revizyonu | Backend'e **sunucu filtre + sayfalama** (`q`/`durum`/`sofor`) ve `/soforler` ucu; 200 kayıt tavanı kalktı; frontend'de filtre çubuğu + `Paginator`; tüm durum kodları **i18n sözlüğü** ile çevrildi; "tamam" artık yalnız gerçek tamamlanan durumlar için; üretim emri **onay** ister; çoklu emir/sevk/teslimat sayaçları gösterilir. |
| 14 | PDF / yazıcı | Termal fişe **`@page`** (80/58mm) eklendi (A4'e basılma/kırpılma giderildi); fiş penceresi **görselleri bekleterek** yazdırır (boş logo/düzen kayması giderildi); fatura A4'te `@page` + `break-inside` + `thead` tekrarı; Teklifler `@page` artık yalnız `@media print` içinde; tasarımcıdaki **çift `@page`** kaldırıldı; PDFBox tablo raporları **ağırlıklı kolon** + `bilgiBlogu`/etiket sayfa taşması koruması; rapor tutar formatı **tr-TR**'ye sabitlendi. |
| — | Global yoğunluk | `app.css` kompaktlaştırıldı (main-content padding, tablo/toolbar/input/dialog/card yoğunluğu, form gap'leri); `dashboard.css` başlığı 28→24px (global `page-title` ile hizalı); 44px/16px **dokunma bloğu** artık yalnız `@media (hover:none) and (pointer:coarse)` (dar masaüstünde arayüz şişmiyor). |

Doğrulama: `lint` temiz · `i18n:check` temiz (4980=4980) · `test` **1498/1498** (96 dosya) · backend `SiparisTakip*`, `PdfRapor*`, `RaporController` testleri 24/24 · `build` başarılı · Docker frontend+backend yeniden derlendi ve sağlıklı.

### Faz 3A — Üretim (tamamlandı)

Üretim isteği: "1 litre kolanın malzemelerini girdim; 10 litre için de hesap edebilmeli, ince ayar yapılabilmeli, sayfa görsel olarak düzenlenmeli."

- **Baz miktar (V161):** `stok.recete`'ye `baz_miktar` (varsayılan 1) + `baz_birim` eklendi; `Recete`/`ReceteDTO` alanları. Kalem miktarları artık **baz miktar** içindir; ölçek = istenen / bazMiktar. Varsayılan 1 olduğu için eski reçeteler birebir korunur.
- **Tek kaynak ölçekleme:** `ihtiyacAnalizi` ve `emirTamamla` aynı baz/fire matematiğini kullanır; `emirTamamla`'da `(üretilen + fire) / bazMiktar`, `ihtiyacAnalizi`'de `istenen / bazMiktar`.
- **Aktif/revizyon seçimi:** `findFirstBySirketIdAndUrunIdAndAktifTrueOrderByRevizyonDesc` — pasif/eski revizyon tüketilmesi engellendi.
- **Frontend (`Uretim.vue`):** reçete dialoguna **Baz Miktar + birim** alanları; **"Üretim Miktarı"** girişi ve kalem tablosunda **canlı "Hesaplanan"** kolonu (fire + kalem fire dahil). 5 etiketsiz kontrol yerine etiketli grid (Hammadde | Miktar | Birim | Fire % | Hesaplanan | Sil); mobilde 2 kolona iner.
- **i18n:** `uretim.bazMiktar/uretimMiktari/hesaplanan` (tr+en).

Doğrulama: `lint` temiz · `i18n:check` temiz (4983=4983) · `test` **1500/1500** (96 dosya) · backend **1616/1616** (baz miktar ölçekleme testleri dahil) · `build` başarılı · Docker yeniden derlendi, **V161 canlı Postgres'te uygulandı** doğrulandı.

### Faz 3B — Bordro (tamamlandı)

- **Kümülatif gelir vergisi (V162):** `ik.maas_bordro`'ya `gelir_vergisi_matrahi` eklendi. Önce toplu üretim her ayı "ilk ay" gibi hesaplıyordu (kümülatif matrah hep 0) → yüksek ücretlilerde yıl boyunca eksik vergi. Artık `kumulatifMatrah` önceki ayların toplamından gelir; `hesapla` verilmezse sunucu otomatik toplar; `topluUret` her personel için toplayıp kaydeder.
- **Canlı hesaplama (frontend):** bordro oluşturma dialogunda personel/brüt/dönem değişince `/hesapla` çağrılır; **SGK+İşsizlik, Gelir Vergisi, Damga, Matrah, İşveren Maliyeti** dökümü gösterilir; kesinti otomatik doldurulur (elle girişte korunur); net anında görünür.
- **Filtre + sayfalama + KPI:** sunucu taraflı filtre (yıl/ay/durum/ödeme durumu/personel arama) + `Paginator`; `/ozet` ucu ile **Toplam Brüt/Kesinti/Net/Ödenen Net/Kayıt** KPI şeridi.
- **Vergi dilimleri editörü:** ayarlardaki ham JSON textarea yerine **üst limit/oran** satır tablosu (sınırsız = boş limit); kaydederken JSON'a serileşir.
- **Bordro fişi:** satır başına **yazdır** (A4, `@page`) — personel/dönem/matrah/brüt/kesinti/net.
- **i18n:** tüm yeni etiketler tr+en.

Doğrulama: `lint` temiz · `i18n:check` temiz (5007=5007) · frontend **1503/1503** · backend **1618/1618** (kümülatif matrah testleri dahil) · `build` başarılı · Docker yeniden derlendi, **V162 canlı Postgres'te uygulandı**.

**Ertelenen (3B kapsamı dışı):** toplu üretim öncesi seçim/önizleme ekranı; bordro modülüne MUHASEBE rolü erişimi (hassas ACL; ayrı değerlendirilecek).

### Faz 3C — Hesap Ayarları (tamamlandı)

- **Gruplama:** düz 8 sekmeli şerit yerine **Kişisel** (Profil, Güvenlik, Görünüm, Bildirimler) / **Sistem** (Entegrasyonlar, Yazdırma, Sistem, Marka) `SelectButton` grubu; `v-if="kisiselMi(n)"` ile sekmeler gruplanır, grup değişince ilk sekmeye dönülür.
- **Şifre tek kaynak:** satır içi şifre formu kaldırıldı → paylaşılan `PasswordChangeModal` açılır (kenar çubuğuyla aynı bileşen). Duplike `sifreForm`/`sifreKaydet` silindi.
- **Avatar yükleme:** ham URL alanına ek olarak **önizlemeli dosya yükleme** (`uploadAPI.foto` → URL).
- **Bildirim tipleri** `BildirimZili` ile hizalandı (`AJANDA` eklendi).
- **i18n:** yeni etiketler tr+en.

Doğrulama: `lint` temiz · `i18n:check` temiz (5015=5015) · frontend **1507/1507** (97 dosya) · `build` (Docker içi) başarılı · Docker frontend yeniden derlendi, **healthy**. (Backend değişmedi.)

### Faz 3D — Raporlar (tamamlandı)

- **Favoriler key tabanlı:** `raporFavoriDegistir` artık sayısal `index` **kaydetmez**; `favoriAc` `key → güncel sekme` çözer (eski index'li kayıtlar için fallback). Sekme eklenince/sırası değişince favoriler kırılmaz.
- **Üst tarih aralığı tek yetkili kaynak:** `tarihAraliginiUygula(idx)` ile tarih aralığı **0 cari ekstre, 1 gelir/gider, 2 KDV, 3 yaşlandırma (bitiş), 4 cari kârlılık, 9 temsilci** sekmelerine uygulanır; hem tarih değişiminde hem sekmeye girerken.
- **Sayfalama:** tedarikçi ürünleri ve ürün kârlılığı tablolarına `Paginator` (10/15/25/50) eklendi (binlerce satır artık tek seferde render edilmiyor).
- **Export para birimi:** ekrandaki dönüşüm ile indirilen PDF/Excel (sunucu TRY) farkı, para birimi TRY değilken görünür **not** ile bildirilir.
- **i18n:** `raporlar.exportParaBirimiNotu` (tr+en).

Doğrulama: `lint` temiz · `i18n:check` temiz (5016=5016) · frontend **1511/1511** (98 dosya) · `build` (Docker içi) başarılı · Docker frontend yeniden derlendi, **healthy**. (Backend değişmedi.)

**Ertelenen (3D kapsamı dışı):** Raporlar view'ının sekme-başına alt bileşenlere tam parçalanması; PDF/Excel çıktılarının backend'de seçili para birimine **çevrilmesi** (TCMB kurlarının PDF üretimine bağlanması ayrı, odaklı bir iş).

### Faz 3E — Saha Portalı (tamamlandı)

- **Mobil uyum:** `SahaPortali.vue` artık `@media (max-width: 640px)` taşır — header dikey yığın, form satırları tek kolon, 7 sekme **yatay kaydırmayla** erişilebilir (mobil `layout-audit` hedefi).
- **Çevrimdışı kuyruk görünürlüğü:** bekleyen kayıt sayısı header'da rozet olarak ve **elle senkron** butonu (yükleniyor durumu).
- **i18n boşlukları:** `SahaSiparislerPanel` içindeki ham `Tutar`/`Durum` metinleri ve ham durum kodu `siparisTakip.durum.*` çevirisine bağlandı.
- **Not:** Sipariş görünürlüğü DRIVER rolü için backend'de zaten kullanıcıya atananlarla sınırlı (`SiparisController.soforKullaniciId`); ek client filtresi gereksiz.

Doğrulama: `lint` temiz · `i18n:check` temiz (5017=5017) · frontend **1514/1514** (99 dosya) · `build` (Docker içi) başarılı · Docker frontend yeniden derlendi, **healthy**. (Backend değişmedi.)

### Faz 3F — Rapor dışa aktarımlarında para birimi dönüşümü (tamamlandı)

Ekrandaki değerler seçilen para birimine çevrilirken PDF/Excel ham TRY idi. Artık **dışa aktarımlar da seçili para biriminde** üretilir.

- **`DovizCevirici`** (yeni): istek-yerel (thread-local) hedef para birimi; TRY→hedef dönüşümü `TcmbKurService.cevir` ile; kur yoksa ham TRY'ye güvenli düşüş.
- **`RaporController`**: `yaslandirmaPdf`, `cariEkstrePdf`, `gelirGiderPdf`, `cariKarlilikPdf`, `butceGerceklesenPdf` uçları opsiyonel **`doviz`** parametresi alır; `basla/temizle` ile sarılır, tutarlar `cevirStr`/`formatTutar` üzerinden çevrilir ve PDF başlığına `(USD)` soneki eklenir.
- **Frontend**: `dovizParam()` ile seçili birim (TRY dışıysa) PDF isteklerine eklenir; cari ekstre **CSV**'si de `dovizStore.convert` ile çevrilir. Böylece ekran ↔ PDF ↔ Excel ↔ CSV tutarlı.

Doğrulama: `lint` temiz · `i18n:check` temiz (5017=5017) · frontend **1514/1514** · backend **1624/1624** (`DovizCeviriciTest` + `doviz` parametre testi dahil) · `build` başarılı · Docker frontend+backend yeniden derlendi, **healthy**.

### Faz 3G — Ertelenen maddeler (tamamlandı)

Önceki fazlarda bilinçli ertelenen işler kapatıldı.

- **5 — Toplu bordro üretim öncesi önizleme/seçim:** yeni `GET /api/maas-bordro/toplu-onizleme` ucu (her personel için `UYGUN`/`MAAS_YOK`/`ZATEN_VAR` + tahmini net) ve `POST /toplu-uret` artık opsiyonel `personelIds` gövdesi alır. Frontend diyaloğunda önizleme tablosu + **işaretli personeli** üretme.
- **3 — Bordroya MUHASEBE erişimi:** backend `MaasBordroController` sınıf-seviyesi `hasAnyRole('ADMIN','USER','MUHASEBE')`; frontend genel **`roller` meta/menü desteği** eklendi (`router` guard + `AppSidebar` filtresi); bordro rotası/menüsü `roller: ['ADMIN','MUHASEBE']`.
- **4 — PDFBox metinleri çok dilli:** doküman PDF'leri (fatura/sipariş/irsaliye/teslimat) zaten `Accept-Language` + `PdfMetin` kullanıyordu. **Raf etiketi** ve **rapor tablo başlıkları/kolonları** da `PdfMetin`'e bağlandı (tr/en); raf etiketi uçları ve teslimat fişi artık `Accept-Language` alır. Sözlüğe rapor/etiket anahtarları eklendi.
- **2 — Raporlar parçalama (kısmi):** bağımsız 4 analitik sekme alt bileşenlere çıkarıldı — `RaporTedarikciUrunler.vue`, `RaporUrunKarlilik.vue`, `RaporNakitAkisi.vue`, `RaporTemsilci.vue` (kendi verisini yükler; `v-if` ile tembel mount; temsilci `tarihAraligi` prop'u). Parent (`Raporlar.vue`) bu sekmelerin state/getter'larından arındı.

Doğrulama: `lint` temiz · `i18n:check` temiz (5021=5021) · frontend **1515/1515** (99 dosya) · backend **1620/1620** (`PdfMetinTest`, `topluOnizleme`, `doviz`/`roller` dahil) · `build` başarılı · `docs-sync` (components 81) · Docker frontend+backend yeniden derlendi, **healthy**.

**Kalan (istenirse):** Raporlar'ın geri kalan sekmeleri (cari ekstre, gelir/gider, KDV, yaşlandırma, cari kârlılık) da benzer şekilde alt bileşenlere çıkarılabilir; bunlar cari seçimi/tarih/PDF/eposta akışını paylaştığı için ek görsel doğrulama gerektirir.

## "Yeni Satış" Penceresi Revizyonu (2026-10)

`Satis.vue` (`/satislar`) ekranındaki **Yeni Satış** penceresi ve onun paylaşımlı kalem bileşeni `FaturaKalemleri.vue` (hem `Satis.vue` hem `Faturalar.vue` kullanır) elden geçirildi.

| Aşama | Kapsam | Durum |
|---|---|---|
| **0** | Test güvenlik ağı: `FaturaKalemleri.vue` (23 mount testi) + `AppDialog.vue` (8 test); `redteam-faz4-satis-akisi` sınıflandırıldı | ✅ |
| **1** | Pencere boyutu: `AppDialog`'a **içerik kaydırması** (`overflow-y: auto` — asıl "sığmıyor" nedeni), 1280px × 84vh, Esc kontrolü | ✅ |
| **2** | Stok seçimi: tek yol (hızlı ekleme + **barkod**), satır içi arama kaldırıldı, öneri satırında **stok adedi**, işlevsiz dropdown oku kaldırıldı | ✅ |
| **3** | Cari seçimi: zengin öneri satırı (vergi no + telefon) + **seçili cari kartı** (bakiye, kredi limiti, risk, vade, adres) | ✅ |
| **4** | Silme: **onay + 8 sn geri-al**, satır seçimi + `Del` ile silme | ✅ |
| **6** | Düzeltme turu: `?` bozulmaları (kodlama), öneri rozeti global CSS, barkod tam satır + ara breakpoint | ✅ |
| **7** | İyileştirme turu (8 sorun): **tek vurgu rengi (teal)**, **etiketli tek sıra** hızlı ekle, barkod ikon payı, **kompakt boş durum**, **özet sağda**, **footer'dan her zaman görünür + pasif neden**, barkod odak | ✅ |

### Aşama 6 — Düzeltme turu (tamamlandı)

Kullanıcı geri bildirimi: *"barkod okutun ... stok arama kısmı küçük kalmış ve bazı harfler ? ile gösteriliyor"*.

1. **`?` ile görünen harfler — kodlama kaybı.** i18n ekleme script'leri PowerShell ile `-Encoding ASCII` yazıldığı için script **dosyasına** yazılırken Türkçe harfler `?`'ye dönüşmüş ve `tr.json`'a öyle geçmişti. **11 metin** düzeltildi (`hizliSatis.adetStokAsimi/adetGecersiz/adetAdimUyusmadi/katalogYukleniyor/katalogYuklenemedi/cogaltFiyatTipiYok`, `satis.cariGun/cariKrediLimitiAsildi/cariBorcUyari`, `faturaKalemleri.hizliEkleIpucu/barkodBulunamadi`). Aynı hata Satis.vue'daki `·` ayracını da bozmuştu; düzeltildi.
   > **Süreç kuralı:** Türkçe metin içeren script'ler artık PowerShell'den **UTF-8** (veya doğrudan `write` aracı) ile yazılır; `-Encoding ASCII` kullanılmaz.
2. **Öneri rozeti stilsiz kalmıştı.** `.stok-opsiyon-stok` kuralı bileşenin `scoped` stilindeydi; AutoComplete öneri listesi **body'ye teleport** edildiği için scoped kural oraya ulaşmaz → rozet çıplak kalıp ürün adı/fiyatla çakışıyordu. Kural `assets/app.css`'e (global) taşındı.
3. **Quick-add satırı sıkışıktı.** Barkod alanı 150px'lik bir kolondaydı ve yalnız `720px` breakpoint vardı; 720–1150px arasında 6 kolon taşıyordu. Yeni düzen: **barkod kendi satırında tam genişlik** (`grid-column: 1 / -1`), kalan kontroller ikinci satırda; **`@media (max-width: 1150px)`** ara kırılımı eklendi.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1440/1440** · `build` başarılı · Docker frontend imajı yeniden derlendi (Traefik).

### Aşama 7 — İyileştirme turu: 8 sorun (tamamlandı)

Kullanıcı tek tek saydığı 8 düzeltmeyi onayladı. Amaç yerleşim netliği ve **tek tutarlı vurgu rengi**.

1. **Tek vurgu rengi (teal).** Ekranda üç ayrı accent vardı (PrimeVue indigo, `p-button-success` yeşili, `--accent` teal). PrimeVue preset `primary` indigo→teal (`main.js`) ve Tailwind `brand`→teal (`tailwind.config.cjs`) yapıldı; `p-button-success` kullanımları (`Satis.vue` Yeni Satış + Tahsilat Al, `FaturaKalemleri.vue` Ekle) varsayılan primary'e çekildi. `Satis.vue` KPI renkleri teal ailesine (`#14b8a6`/`#0d9488`/`#2dd4bf`) alındı; açık kalan/alacak `#ef4444` semantik kırmızı olarak korundu.
2. **Hızlı ekle satırı: tek sıra + etiketli.** Her alan `.hizli-alan` sarmalayıcısında, üstünde etiket (`faturaKalemleri.etiketBarkod/etiketStok/etiketAdet/etiketFiyat/etiketKdv`). `align-items: end` ile kolonlar hizalı; **Aşama 6'daki barkod tam-satır kuralı kaldırıldı** (artık normal kolon).
3. **Barkod ikon payı.** `.hizli-barkod :deep(.p-inputtext) { padding-left: 2.25rem }` — `IconField` ikonu placeholder üzerine biniyordu.
4. **Kompakt boş durum.** Tablo `#empty` içindeki global `EmptyState` (88px daire + 48px dolgu) yerine `.kalem-yok-tablo` (satır içi, kompakt). Global bileşen başka sayfalarda bozulmadı.
5. **Özet sağda ve kompakt.** `.summary-box` `margin-left: auto; width: fit-content; min-width: 300px`.
6. **Footer her zaman görünür + pasif neden.** `Satis.vue` diyalogu `content-max-height="none"` (AppDialog/app.css flex düzeni içeriği viewport'a sığdırır, footer `flex-shrink:0` ile sabit). Birincil buton pasifken `.footer-neden` metni nedeni yazar (`tamamlaNeden`/`tamamlaAktif` computed); tooltip direktifi projede kayıtlı olmadığı için görünür metin + native `title` kullanıldı.
7. **İ18n.** Yeni etiket anahtarları hem `tr.json` hem `en.json`'a eklendi; parity 4937=4937.
8. **Barkod odak.** Hızlı ekle barkod alanına `autofocus` + bileşen mount'unda `nextTick(() => barkodInput.value?.focus())` (PrimeVue `v-focustrap` ilk odağı kapabileceği için).

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1460/1460** (92 dosya, `redteam-faz10-yeni-satis-modal.spec.js` +19) · `build` başarılı · Docker frontend imajı yeniden derlendi, `footer-neden` içeren yeni bundle Traefik'te sağlıklı. (E2E ertelendi.)

### Aşama 0 — Test güvenlik ağı (tamamlandı)
- `src/components/__tests__/FaturaKalemleri.spec.js`: **23 mount testi** (kalem ekleme, barkod, çoğalt, sil, stok uyarıları, toplamlar). PrimeVue `DataTable`/`Column` gerçek davranışı taklit eden stub'la satır hücreleri render edilir.
- `src/components/__tests__/AppDialog.spec.js`: 8 test (prop/slot sözleşmesi + kaydırma kuralı).
- `redteam-faz4-satis-akisi.spec.js`: davranış testiyle kapsanan `FaturaKalemleri` kuralları işaretlendi; `Satis.vue`'ya özgü kurallar korundu.

### Aşama 1 — Pencere (tamamlandı)
**Kök neden:** `AppDialog` içerik alanına `max-height` veriyor ama **`overflow` vermiyordu**; PrimeVue varsayılanında da yok. Bu yüzden yükseklik aşılınca içerik **kaydırılamıyor, kırpılıyordu**.
- `AppDialog`: `.p-dialog-content`'e `overflow-y: auto` + `overscroll-behavior: contain`; `closeOnEscape` prop'u.
- `Satis.vue` ve `Faturalar.vue` (ham `Dialog` → `AppDialog`'a taşındı): `1280px` genişlik, `min(84vh, 900px)` içerik yüksekliği, Esc ile kapanma kapalı (form verisi kaybını önler).
- **Not:** 1280px, kalem tablosunun ~986px'lik minimum genişliğini karşılar; yatay kaydırma ihtiyacı ortadan kalkar.

### Aşama 2 — Stok seçimi (tamamlandı)
- **İki arama yüzeyi tek yola indirildi**: hızlı ekleme satırı + **barkod**. Satır içi açıklama araması kaldırıldı; açıklama artık serbest metin (her iki ekranda).
- **Barkod alanı**: okutup Enter → kalem anında eklenir; bulunamazsa panel içinde uyarı (sessiz yutma yok).
- Öneri satırına **mevcut stok adedi** eklendi (stoksuzsa kırmızı).
- İşlevsiz `dropdown` oku kaldırıldı (boş sorguda listeyi boşaltıyordu — HızlıSatış'taki hatanın aynısı).
- `stok-sec` olayı ve iki ekrandaki `stokSatirSecildi` kaldırıldı.

### Aşama 3 — Cari seçimi (tamamlandı)
- Öneri satırı: ad + **vergi no** (DTO alanı `vergiNumarasi`; kod yanlışlıkla `vergiNo` okuyordu → hiç görünmüyordu) + telefon.
- **Seçili cari kartı**: bakiye (borç kırmızı / alacak yeşil), kredi limiti, vade, **adres** ve risk uyarısı (borç ≥ limit → kırmızı). Düzenleme modunda cari `cariHesapCoz` ile tam çözülür.
- Backend değişikliği gerekmedi (DTO'da tüm alanlar mevcut).

### Aşama 4 — Düzenleme/silme (tamamlandı)
- Silme artık **onay** ister (yanlış tıklama veri kaybı yaratmaz) ve ardından **8 sn geri-al bandı** çıkar; "geri al" kalemi **eski konumuna** koyar.
- Satır seçimi + **`Del`** ile silme (odak metin alanında değilken).
- Ebeveynler (`Satis.vue`, `Faturalar.vue`) `@geri-al` olayını işler.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1437/1437** · `build` başarılı. (E2E ertelendi.)

## POS / Hızlı Satış Revizyonu (2026-10)

Kasa ekranında iki ayrı şikâyet işlendi: **malzeme ekleme/satış akışı karışık** ve **kod düzenlenmesi zor**. `HizliSatis.vue` 4057 satırdı (template 884 / script ~1980 / CSS ~1168, 3 ayrı `<style>` bloğu, 93 `ref`, 14 `localStorage` anahtarı, `handlePosKeys` 146 satır).

**Asıl teknik bulgu:** POS testleri kodu değil **metni** doğruluyordu — `handlePosKeys`'in ~25 satırı kelime kelime pin'liydi. Bu yüzden refactor önünde test vardı; yani "düzenlenmesi zor" şikâyetinin ikinci yarısının kökü test altyapısıydı.

Onaylanan kurallar: katalog **sunucu taraflı** aranacak (200 ürünlük istemci tavanı kalkacak); stokta fazlası **satılamayacak**; kg/m² gibi birimlerde adım **0.5**; **F9/F10 yalnızca ödeme durumunu** değiştirecek; kırılgan metin-tarama assertion'ları **davranış testine çevrilecek / uygulama detayıysa silinecek**.

| Aşama | Kapsam | Durum |
|---|---|---|
| **0** | Test güvenlik ağı (çalışma zamanı kodu değişmeden) | ✅ bitti |
| **1** | Kod düzeni: mantık `usePosSepet`/`usePosOdeme`/`usePosKatalog`/`usePosTercih` composable'larına, `fisiYazdir` → `utils/posFis.js`, CSS → `assets/pos-hizli-satis.css`, klavye → gruplanmış handler tablosu; ölü state ve `surklenenUzerinde` yazım hatası | ✅ bitti (1.5–1.7 Aşama 6'da tamamlandı) |
| **2** | Adet/miktar: `PosAdetGirisi.vue`, tek `miktarDegistir`, `[1, stok]` tavanı, ondalık adım, `change`/`blur`/`Enter` commit | ✅ bitti |
| **3** | Malzeme arama: `GET /api/stoklar/filtreli` sayfalı katalog, `gruplama-dagilimi` çip sayaçları, tek arama yüzeyi, ayrışmış yükleme/boş/hata durumları | ✅ bitti |
| **4** | Satır düzenleme/silme: aynı `stokId`+`fiyatTipi` çakışmasında birleştirme, fare/klavye seçim tek kaynak, adet penceresi tıklanan kartın yanına | ✅ bitti |
| **5** | Ödeme/tamamlama: satır içi doğrulama + panel otomatik aç/kaydır/odaklan (gizli zorunlu teslimat alanı), F9/F10 güvenli, ödeme paneli sadeleştirme | ✅ bitti |

Her aşamada yeşil kapı: `npm run lint` · `npm run i18n:check` · `npm run test` · `npm run build` · POS Cypress spec'leri.

### Aşama 0 — Test güvenlik ağı (tamamlandı)

- **Kırık test düzeltildi:** `redteam-faz4-pos-klavye.spec.js` içindeki "useKisayollar ipucu eylemini destekliyor" denetimi `useKisayollar.js`'in `?` bloğunun **ilk 400 karakterini** okuyordu; yorum satırı eklendiği için kırılıyordu (mantık doğruydu, mantık değişmedi). Süslü parantez dengesiyle blok çıkaran `blokBul()` yardımcısı yazıldı; artık yorum eklendiğinde test kırılmıyor.
- **6 yeni mount test dosyası, 120 yeni test:** `PosSepetPaneli` (25), `PosOdemePaneli` (24), `PosFisOnizleme` (22), `PosUrunKarti` (18), `PosDialogs` (16), `PosMusteriPaneli` (15). Bu 7 POS bileşeninin **hiçbirinde** mount testi yoktu; props/emits/erişilebilirlik sözleşmeleri artık sabit.
- **Metin-tarama assertion'ları sınıflandırıldı:** yeni mount testlerinin sahiplendiği kurallar silindi; `HizliSatis.vue` içi kurallar blok-kapsamlı denetime çevrildi (yorum/sıra değişimine dayanıklı) ve her birine taşınacağı composable notlandı; çift `defineEmits` kuralı tek dosyadan **tüm `.vue` dosyalarını tarayan** mimari denetime genelleştirildi.
- **Bilinçli silinenler:** `stokStore.getAll({ size: 200 })` değerine kilitlenen denetim (kuralı değil *tavanı* koruyordu; Aşama 3'te tavan kalkıyor) ve `:min-length="2"` + `:delay="0"` tek satırına kilitlenen denetim (yerine çapraz dosya kuralı geldi: `:delay` tanımlıysa 0 olmalı).
- **Karakterizasyon notu:** `PosMusteriPaneli` testleri teslimat adresinin yalnızca şoför seçiliyken render edildiğini (Aşama 5'in "gizli zorunlu alan" hedefi) ve `PosFisOnizleme` fiyatsız fiş modunun tüm tutar satırlarını gizlediğini bilinçli olarak sabitliyor.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` 1068/1068 (77 dosya) · `build` başarılı · POS Cypress 38/38 (`hizli-satis` 25, `redteam-faz2` 6, `redteam-faz5-sepet-ust-uste` 7). Bu aşamada **çalışma zamanı kodu değiştirilmedi** — yalnızca test dosyaları.

### Aşama 1 — Kod düzeni (1.1–1.4 tamamlandı)

`HizliSatis.vue` **4043 → 2654 satıra** indi (script 1974 → ~1750). Kullanıcıya **görünür hiçbir değişiklik** yok; amaç dosyayı düzenlenebilir kılmaktı.

| Adım | Ne yapıldı | Sonuç |
|---|---|---|
| **1.1 CSS** | `<style scoped>` bloğu (1159 satır) → `assets/pos-hizli-satis.css`, `<style scoped>` UZERINDEN `@import` ile geri bağlandı | Scoped semantik **kanıtlandı**: build çıktısında kurallar `[data-v-…]` ile scoped kaldı, genel adlar (`.field`, `.required`) sızmadı |
| **1.2 Fiş** | 98 satırlık `fisiYazdir` (içinde gömülü `<style>` + 25 satır HTML şablonu) → saf `posFisHtml()` fonksiyonu (`utils/posFis.js`) | **+30 birim test**; dosyanın "üçüncü `<style>` bloğu" görüntüsü gitti |
| **1.3 Tercihler** | 14 `localStorage` anahtarı ve **DÖRT farklı kalıcılık yöntemi** (`watch` / `@change` / doğrudan `setItem` / hiç yazılmamış) → `usePosTercih` | **+22 test**; ipucu anahtarı artık 3 yere değil 1 yere yazılıyor |
| **1.4 Klavye** | 146 satırlık `handlePosKeys` (25+ kısayol, iç içe `if`) → `usePosKisayollar` içinde **7 adlandırılmış grup** + tek sıralama listesi | **+55 davranış testi** |

**Öğrenilen kurallar (kalıcı yorumlarla belgelendi):**
- **Fonksiyon tuşları her zaman önce**: ızgara modu "oda" gibi davranıp o an basılan *her* tuşu yutar; F tuşları yine de çalışmalı.
- **Yazarken koruması sıradadır**: harf kısayolları (`n/k/h/t/p/g/d`) `girdideMi` kontrolünden **sonra** gelir. Koruma kaldırılırsa ürün adı yazarken `n` ödeme yöntemini değiştirirdi.
- **`?` yalnızca `useKisayollar`'a aittir**: iki handler da capture fazında `window`'da olduğu için `?` burada da ele alınsa ipucu iki kez açılıp kapanırdı (net sonuç: hiçbir şey olmazdı). Bu, `usePosKisayollar` context'inde `ipucuToggle` **olmadığı** anlamına gelir.
- **`girdidenIzgaraya` en başta**: metin alanındaki `↓`; F tuşlarıyla çelişemediği için sıralama değişikliği davranışı etkilemez.
- `girdideMi` artık gerçek `boolean` döndürüyor (`Boolean(...)` ile sarıldı; `el.isContentEditable` tanımsızken `undefined` dönüyordu).

**Aşama 1'de kazanılan gerçek davranış düzeltmeleri:**
- `girdidenIzgaraya`, `fonksiyonTuslari`, `altOklar`, `izgaraModu`, `sepetGezinme`, `yazarkenKoru`, `satirSil`, `harfKisayollari` artık **tek birim test dosyasında saf fonksiyonlar**; `window` dinleyicisi kurmadan, mount yapmadan doğrulanıyor.
- Aşama 0'ın kırılgan testleri artık doğru dosyayı tarıyor: `redteam-faz4-pos-klavye.spec.js` yalnızca view tarafındaki sözleşmeyi denetliyor, tuş→eylem eşlemesi `usePosKisayollar.spec.js`'te.

**Bilinen yan etki / düzeltilen regresyon:** HEAD sürümüne geri dönüşte commit edilmemiş "başarılı ekleme toast'unu kaldır" değişikliği kayboldu ve `redteam-faz5-sepet-ust-uste` 2 testi kırıldı (her barkod okutmada "Ürün Eklendi" bildirimi çıkıyordu, bildirimler sağ paneli örtüyordu). `barkodTarandi` içindeki iki başarı toast'u kaldırılarak düzeltildi; hata/uyarı bildirimleri korunuyor.

**Ertelenenler (1.5–1.7) ve gerekçesi:** `usePosSepet` / `usePosOdeme` / `usePosKatalog` çıkarımı yapılmadı. Bu üç blok sepet satırlarının **toplam hesaplarına, ödeme paneline, ürün ızgarasının template'ine ve kısayol context'ine** doğrudan bağlı; taşıma sırasında template bağlantılarının da değişmesi gerekiyor. Aşama 2 (adet girişi) ve Aşama 3 (sunucu taraflı katalog) bu blokları zaten yeniden yazacağı için, önce onların yapılması hem daha az iş hem daha az regresyon demek.

### Aşama 2 — Adet (miktar) kontrolü (tamamlandı)

**Sorun:** Adet alanı `v-model.number` ile doğrudan state'e yazılıyordu; `min="1"` yalnızca tarayıcı spinner'ını kısıtlıyordu. Sonuç: negatif/0 adet yazılabiliyor, `Enter` hiçbir şey yapmıyordu, depoda 3 olan ürün sepete 500 olarak eklenebiliyordu. Ayrıca `+` butonu geri-al kaydı yazmıyor ve stok kontrolü yapmıyordu; `−` butonu miktârı 1'de düşünce **satırı sessizce siliyordu**; kg/m² ürünlerde adım hep 1'di.

**Tavan kaynağı doğrulandı:** Backend `FaturaService` satışta `Stok.miktar` ile karşılaştırıp yetersizse `"Yetersiz stok!"` ile reddediyor. `Stok.miktar` tek global değerdir (depo bazlı değil), yani POS'un zaten elinde olan `StokDTO.miktar` **birebir doğru tavan** değeridir.

| Parça | Ne |
|---|---|
| `utils/posAdet.js` | Saf kurallar: `adimBirimIcin`, `miktarDogrula`, `adimla`, `adimaYuvarla`, `ADET_SORUN`. **+38 test** |
| `components/PosAdetGirisi.vue` | `−` / girdi / `+` tek bileşende. **+21 test** |
| `HizliSatis.vue` → `miktarDegistir(idx, istenen, mod)` | **TEK giriş noktası**: doğrulama, stok tavanı, geri-al kaydı, satır vurgusu ve kullanıcı bildirimi |

**Kazanılan davranış:**
- Buton, satır içi girdi (`change`/`blur`/`Enter`) ve klavye (`Alt+↑↓`) **tek yoldan** geçer → stok tavanı ve geri alma hepsinde aynı çalışır.
- **Negatif / 0 / harfli adet** engellenir ve sessizce düzeltilmez: kullanıcı bilgilendirilir.
- **Stokta fazlası satılamaz** (onaylanan kural). Aşımda adet tavana kırpılır ve uyarılır; `−` butonu 1'de satırı silmez.
- **Ondalık adım 0.5**: kg/m²/m³/lt gibi ölçülen birimlerde yarım adım, tam sayı birimlerde 1. Adımlar Türkçe/küçük harf varyasyonlarına duyarlı.
- **Geri al penceresi artık adet değişikliğini de kapsar** (`+` butonu da geri alınabilir).
- Aynı ürünün **üstüne** eklemede tavan toplam üzerinden kontrol edilir (sepette 2 + istenen 5, stok 3 → 3).
- **Barkodla stoksuz ürün** artık sepete giremiyor; kart tıklamasındaki gibi uyarılıyor.

**Tasarım kararı — bilesen adedi kendisi değiştirmez:** `PosAdetGirisi` yalnızca **olay yayar**, değeri `miktar` prop'undan okur. Doğrulama üst tarafta yapılır. Böylece üst taraf bir değişikliği reddederse (stok aşımı) girdi **kendiliğinden eski değerine döner ve kırmızıya döner**; "yazdığım değer kaydedilmedi" durumu görünür olur. Bileşen kendi adedini yazsaydı ekrandaki sayı ile gerçek miktar ayrılırdı.

**İki gerçek hata yakalandı ve düzeltildi:**
- Bileşende ilk değer `String(...)` ile kurulduğu için "değiştirmeden Enter" commit'i **string**, yazıp Enter **number** gönderiyordu → değer tutarlı sayıya çekildi.
- `miktarAzalt` artık adet 1'de satırı silmiyor (veri kaybı).

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1235/1235** · `build` başarılı · E2E **41/41** (`hizli-satis` 25, `redteam-faz5-sepet-ust-uste` 7, `redteam-faz2` 6, `dialog-layout` 3).

### Aşama 3 — Malzeme arama / katalog (tamamlandı)

**Sorun (senin "stoktan malzeme ekleyip satış yaparken zorlanıyorum" şikâyetinin asıl kökü):** Katalog POS'a ilk açılışta **tek seferde 200 ürün** ile çekiliyordu; arama, kategori/marka/stok grubu filtreleme ve "sadece stokta" filtresi **bu 200 ürün üzerinde yerel** olarak çalışıyordu. Sonuç:

- **200. üründen sonraki hiçbir malzeme POS'ta bulunamıyordu.** Kasiyer ürünün stokta olduğunu biliyor, POS "bulunamadı" diyordu.
- Kategori/marka/stok grubu seçenekleri yalnızca o 200 üründen türetiliyordu → **sayaçlar yanlış**, 200. üründen sonraki gruplara ulaşmak mümkün değildi.
- İstek sürerken ızgara boş görünüyor ve **"ürün bulunamadı"** yazıyordu; hata halinde de aynı mesaj çıkıyordu. Kasiyer malzemenin olmadığını sanıp aramaya devam ediyordu.
- "Daha fazla" butonu yerel listeyi 60'ar büyütüyordu, yani 200'de bitiyordu.

**Çözüm — katalog artık sunucu taraflı ve sayfalı:**

| Parça | Ne |
|---|---|
| `composables/usePosKatalog.js` | Sayfalı sunucu araması, filtre parametreleri, durum ayrımı, yarış koruması · **+26 test** |
| Backend `GET /api/stoklar/filtreli` | **`sadeceStokta`** parametresi eklendi (POS filtresi sunucuda çözülüyor) |
| Backend `GET /api/stoklar/gruplama-dagilimi` | **`markalar`** dağılımı eklendi (kategori ve stok grubu ile simetrik) |

**Kazanılan davranış:**

- **200 ürünlük tavan kalktı**: katalog `GET /api/stoklar/filtreli` ile sayfalı gelir, "daha fazla" sonraki sayfayı ister.
- Arama metni, kategori, marka, stok grubu ve "sadece stokta" **sunucuya gider**; istemcide filtreleme tamamen kaldırıldı (`filtrelenmisUrunler` silindi). Aynı filtrenin hem sunucuda hem istemcide uygulanması "iki kaynak, iki gerçek" durumu yaratıyordu.
- **Tek arama yüzeyi**: yazarken arama kutusu kart ızgarasını da sunucu tarafı aramayla yeniler; öneri listesi tam barkod/stok kodu eşleşmesi için ayrıca sunucudan gelir.
- **Kategori çipleri gerçek sayaçlarla** gelir (`count(*)` ile tüm katalogdan); her çipte ürün adedi görünür.
- **Yükleme / boş / hata ayrı durumlar**: spinner, "ürün bulunamadı" ve hata + "Yenile" butonu artık birbirinden ayrışır. Izgara `aria-busy` ile duyurur.
- **Sonuç sayacı** artık yerel liste uzunluğu değil, sunucunun gerçek `totalElements` değeridir.
- **Hata durumunda mevcut ürünler boşaltılmaz** — kasiyer elindeki malzemeleri kaybetmez.
- **Yarış koruması**: gecikmeli esenek istek yeni sonucu ezmez.
- Katalog hatası ile filtre dağılımı hatası **ayrıdır**; dağılım gelmezse katalog çalışmaya devam eder.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1261/1261** · backend `mvn -B test` temiz · E2E **41/41** POS spec'i.

> **NOT (E2E ertelemesi):** Proje hâlâ yapım aşamasında olduğu için Cypress E2E koşuları kullanıcı talebiyle **Aşama 4'ten itibaren ertelendi**; tüm E2E suite'i iş bitiminde toplu çalıştırılacak. Aşama 3'te tespit edilen ve düzeltilen Cypress altyapı sorunu şudur: glob'da `*` **yol ayırıcısını geçmez**, bu yüzden eski `'/api/stoklar*'` deseni `/api/stoklar/filtreli?...` ile eşleşmiyordu (katalog artık alt yola taşındı). Desenler `'**/api/stoklar*'` / `'**/api/stoklar/filtreli*'` olarak düzeltildi.

### Aşama 8 — Dashboard/Stoklar CSS ayrıştırması (tamamlandı)

`Dashboard` ve `Stoklar` `<style scoped>` blokları Aşama 1'de kanıtlanan `@import` yöntemiyle dış dosyalara taşındı.

| View | Önce | Sonra | CSS dosyası |
|---|---|---|---|
| Dashboard | 2598 | **1556** | `assets/dashboard.css` (1053 satır) |
| Stoklar | 2233 | **1791** | `assets/stoklar.css` (453 satır) |

**Scoped semantik korundu (build çıktısından doğrulandı):** `Dashboard-*.css` → 247, `Stoklar-*.css` → 165 `[data-v-…]` kuralı; her iki dosyada **sıfır** öneksiz genel ad. Yani başka ekranlara sızma yok.

Bir kaynak-tarama testi (`redteam-faz3-yetki-ikon.spec.js`) taşınan kuralı `Stoklar.vue`'da arıyordu; `assets/stoklar.css`'e yönlendirildi.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1401/1401** · `build` başarılı.

### Aşama 7 — Ödeme ve satış sonucu ayrıştırması (tamamlandı)

`HizliSatis.vue` script'inin kalan yarısı iki uyumlu birime ayrıldı.

| Composable | Kapsam |
|---|---|
| `usePosOdeme.js` | İndirim (tutar/yüzde), ödeme durumu (tam/yarım/yok) + odenen tutar senkronu, ödeme yöntemi, taksit, kasa/banka/POS-terminali seçimleri + yükleyicileri, para üstü, POS komisyonu, türetilen `genelToplam`/`kalanTutar`/`odemeDurumText\|Enum\|Severity` |
| `usePosSatisGecmisi.js` | Satış sonucu özeti (dialog), günlük satış geçmişi (F7), son satış iptali |

**Bilinçli tasarım kararı:** Satışı *oluşturan* orkestrasyon (`satisiTamamlaOnaysiz`, `satisBasarili`) **view'da bırakıldı**. Çünkü o akış sepet + ödeme + teslimat + yazdırma + cari olmak üzere ~25 birimi sırayla bağlar; composable'a taşınsaydı 25 bağımlılık enjekte eden bir "god object" olurdu — yani çözmeye çalıştığımız sorunun daha kötüsü. View'in işi zaten modülleri birleştirmektir.

**Yan düzeltme:** Son satış iptalinde, iptal **başarılı olduğu halde** ardından gelen tazeleme (stok/kasa) hata verirse kullanıcıya "iptal başarısız" deniyordu. Artık iptal sonucu ile tazeleme hatası ayrı ele alınır.

**Sonuç:** `HizliSatis.vue` **4043 → 2472 satır** (%39 azalma; script ~1750 → ~1200). Yeni testler: `usePosOdeme` (33), `usePosSatisGecmisi` (15).

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1401/1401** · `build` başarılı.

### Aşama 6 — Sepet çekirdeği ayrıştırması (Aşama 1.5 tamamlandı)

**Amaç:** Aşama 1'de ertelenen "sepet çekirdeği" çıkarımını bitirmek. `HizliSatis.vue` hâlâ 2900 satırdı ve sepet mantığı (satırlar, işlemler, geri alma, toplamlar) 400+ satır olarak view'ın geri kalanıyla iç içeydi.

**Yapılan:** `composables/usePosSepet.js` — sepetin sahip olduğu TEK sorumluluk:

| Grup | İçerik |
|---|---|
| Durum | `sepet`, `aktifSatir`, `vurguluId`, `geriAlSatir`, `geriAlSepet`, `suruklenenIdx`, `suruklenenUzerinde` |
| Toplamlar | `toplam`, `toplamFt3`, `toplamAgirlik`, `agirlikVarMi` |
| İşlemler | `sepeteEkle`, `miktarDegistir`, `miktarAzalt`, `adediSifirla`, `sepetSil`, `satiriCogalt`, `fiyatTipiDegisti`, `sepeteCariFiyatUygula` |
| Geri alma | `geriAlYap` (SATIR penceresi → sepet), `sepetiGeriAlinabilirTemizle`, `sepetGeriAl`, `sepetiSifirla` |
| Sıralama | `suruklemeBasla/Uzerine/Birak/Bitir` |

Bağımlılıklar enjekte edilir: `t`, `bildir`, `seciliMusteri`, `urunFiyatlariniYukleTek`, `cariUrunFiyatGecmisi`. Böylece composable **view'sız, `window`'sız** test edilebilir.

**Sonuç:**
- `HizliSatis.vue`: **4043 → 2395 satır** (%41 azalma). Script bölümü ~1750 satırdan ~1200'e indi.
- Sepet mantığı artık **48 davranış testiyle** doğrudan doğrulanıyor (`usePosSepet.spec.js`) — önceden yalnız view render testleriyle dolaylı görülebiliyordu.
- `utils/posAdet.js` + `utils/posSepet.js` saf kuralları korunuyor; composable bu kuralları sepet ref'ine bağlar.
- `surklenenUzerinde` yazım hatası `suruklenenUzerinde` olarak düzeltildi.

**Süreç notu:** Bu çıkarım sırasında toplu regex tabanlı silme denemesi dosyada kısmi bozulmaya yol açtı; `lint` + parantez dengesi denetimiyle tespit edilip içerik-çapalı, doğrulamalı düzenlemelerle düzeltildi. Ders: bu büyüklükteki SFC'lerde **satır numarasına değil içerik çapasına** dayalı ve her adımda doğrulanan düzenleme yapılmalı.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1353/1353** · `build` başarılı. (E2E ertelendi.)

### Aşama 5 — Satış tamamlama / ödeme (tamamlandı)

**Sorun (üç ayrı kusur):**

1. **F9/F10 kazara kayıt**: Ödeme durumu tuşları (F9 = peşin, F10 = kısmi) durumu ayarlayıp **ardından satışı anında tamamlıyordu**. Kasiyer ödeme tipini seçerken yanlışlıkla F9'a basması satışı kaydediyor, fiş penceresi açılıyor ve geri dönüşü olmuyordu.
2. **Gizli zorunlu alan**: Teslimat paneli varsayılan **kapalı**; şoför seçilince adres **zorunlu** hale geliyordu ama panel kapalıyken alan *render edilmediği* için hiç görünmüyordu. Kasa duruyor, toast çıkıyor, kullanıcı paneli elle aramak zorunda kalıyordu.
3. **Doğrulama yalnızca toast**: Hangi alanın eksik olduğu ekranda görünmüyordu.

**Çözüm:**

| Parça | Ne |
|---|---|
| `usePosKisayollar` | F9/F10 artık **yalnızca** ödeme durumunu değiştirir; `satisiTamamla` bağlamdan çıkarıldı · **+3 test** |
| `hataGoster` / `hataTemizle` / `hatalar` | Alan bazlı hata durumu; ilgili panel otomatik açılır, alan kırmızıya döner, altında açıklama çıkar |
| `watch(seciliSofor)` | Şoför seçilince teslimat paneli **otomatik açılır** |
| Panel `hatalar` prop'u | `PosMusteriPaneli` (teslimat adresi) + `PosOdemePaneli` (taksit) satır içi hata gösterir |
| `odemeAktif` computed | Ödeme panelindeki 6 tekrarlayan koşul adlandırıldı (davranış aynı, okunurluk arttı) |

**Kazanılan davranış:**
- **F9/F10 artık satışı tamamlamaz** — satışı bitirmek için açık bir eylem gerekir (`Ctrl/Cmd+S` veya "Satışı Tamamla" butonu). Kazara kayıt riski kalktı.
- **Şoför seçilince adres alanı anında görünür**; gizli zorunlu alan kalmadı.
- Doğrulama hatası artık **satır içi**: alan kırmızı, altında açıklama; alan doldurulunca hata kendiliğinden temizlenir.
- Hata halinde panel otomatik açılır ve ilgili alana **odaklanır** (kasa akışı kesilmez).
- Müşteri eksikliği de ilgili paneli açıp müşteri moduna geçer.
- Korunanlar: sepet-boş ve onay adımı doğrulamaları, taksit zorunluluğu, kredi limiti onayla-tekrar-dene, çevrimdışı kuyruk.

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1305/1305** · `build` başarılı. (E2E ertelendi.)

### Aşama 4 — Satır düzenleme / silme (tamamlandı)

**Sorun:** Satır düzenleme akışında üç somut kusur vardı:

1. **Duplicate satır**: `Çoğalt`, aynı ürünün *aynı* fiyat tipiyle ikinci satırını açıyordu. Karttan yeniden ekleme `find` ile yalnızca **ilk eşleşmeyi** bulduğu için kopyadaki miktar artmıyordu → kasiyer "ekledim ama artmadı" diyordu.
2. **Aktif satır senkronu**: Satır içindeki düğmeler `@click.stop` ile kullanıldığı için klavye odağı **başka satırda** kalıyordu; bir satırı tıklayıp `Del`'e basmak **yanlış satırı siliyordu**.
3. **Adet penceresi**: `position: fixed` ama `top/left` yoktu → tıklanan karttan bağımsız, ekranın ortasında açılıyordu.

**Çözüm:**

| Parça | Ne |
|---|---|
| `utils/posSepet.js` | `sepetSatiriBul`, `kullanilanFiyatTipleri`, `sonrakiFiyatTipi`, `sepetteToplamAdet` — saf, birim testli · **+13 test** |
| Aktif satır sarmalayıcısı (`satirAktifYap`) | Tüm satır eylemleri önce aktif satırı hizalar |
| Adet penceresi konumu | Sağ tık noktasından (yoksa odaklı kart kutusundan) hesaplanır, ekran dışına taşmaz |

**Kazanılan davranış:**
- **Satır kimliği `(stokId + fiyatTipi)`** olarak netleşti: aynı ürün + aynı fiyat tipi tek satırda birikir; farklı fiyat tipi ayrı satırdır (zaten "çoğalt"ın varlık sebebi).
- **`Çoğalt` aynı fiyat tipini tekrarlamaz**: kullanılmayan ilk fiyat tipiyle yeni satır açar, fiyatı da o tipe göre ayarlar. Alternatif yoksa **çoğaltmaz** ve kullanıcıyı bilgilendirir.
- **Zenginleştirme hatası düzeltildi**: fiyat listesi / cari son-alış bilgisi artık `(id+fiyatTipi)` ile bulunan satıra yazılır (önce iki fiyat tipi varken **yanlış satıra** yazılıyordu).
- **Kart rozeti** aynı ürünün **tüm** satırlarını toplar (önce yalnız ilk satırı gösteriyordu).
- **Tıkla → `Del`** artık doğru satırı siler; fare ve klavye seçimi senkron.
- Adet girdisine **odaklanınca metin seçilir**; `1` → `20` yazmak için önce silmek gerekmez.
- Adet penceresi **tıklanan kartın yanında** açılır (konum sınırlandırılmış).

Doğrulama: `lint` temiz · `i18n:check` temiz · `test` **1290/1290** · `build` başarılı. (E2E ertelendi.)

## Dalga 0 — Güvenilirlik ve Veri Bütünlüğü (tamamlandı)

Her faz küçük tutuldu; faz sonunda tam kapı (lint · i18n · test · build · Docker sağlık) çalıştırıldı.

| Faz | Konu | Sonuç |
|---|---|---|
| 0.1 | `client.js` sessiz hataları (`hataYayinla`, bağlantı/zaman aşımı/sunucu ayrımı, çevrimdışı bayrağı) | spec 11→18 |
| 0.2 | `MaasBordroService` kasa ödemesi dashboard cache'ini temizlemiyordu | test 12→14 |
| 0.3 | Tahsilat idempotency (`X-Idempotency-Key` + çift gönderim guard'ı) | controller 2→6, dialog 1→4 |
| 0.4 | Tahsilat dönem kilidi: güvenlik `hareketOlustur` içindeydi, mesaj yanlıştı | test 8→10 |
| 0.5 | Saha portalı çevrimdışı imzalı teslimat kuyruğu | spec 1→4 |
| 0.6 | Export/rapor uçlarına 180 sn timeout (tek yerden, URL deseni) | spec 18→21 |

Ayrıca: `docker-compose.yml` yedekleme şifresi hatası (tüm yedekler başarısızdı) ve `BackupService` yarım dosya/boş gzip kabulü düzeltildi.

## Dalga 1 — Performans ve PWA (tamamlandı)

| Faz | Konu | Ölçüm |
|---|---|---|
| 1.1 | `RaporService.stokDegerleme` N+1 → tek sorgu | 50 stok, `times(1)` + `never()` |
| 1.2 | V163 indeks `(sirket_id, iptal, hareket_tarihi DESC)` | 16.807 ms → **0.149 ms** (~113x) |
| 1.3 | `KpiKart` SparkLine'ı async (chart-vendor 181.8 KB) | Satis/Uretim/Gorunumler360 |
| 1.4 | `sw.js` activate: blanket cache silme yerine yalnız `raspel-eski-*` | 2 test |
| 1.5 | `IadeService.tumunuGetir` N+1 → `findByIadeIdIn` | 27→29 |

## Dalga 2 — Güvenlik ve Veri Bütünlüğü (tamamlandı)

**Yöntem notu:** Her denetim maddesi kodla doğrulandı. 5 maddenin 3'ü **abartı/yanlıştı** ve raporda düzeltildi; gerçek açıklar denetimin işaret etmediği yerlerdeydi.

| Faz | Konu | Sonuç |
|---|---|---|
| 2.1 | PDF rapor uçları — aktif açık **yoktu**; MUHASEBE sınıf kuralında eksikti (muhasebeci PDF basamıyordu) | SecurityConfig 9→15 |
| 2.2 | `AuditAspect` kapsamı 16→51 desen (depo, bordro, poz, dönem kilidi, mutasyonlar) | 7 test |
| 2.3 | Bordro ayarı: oran sınırsızdı; bozuk dilim JSON'u herkese sessizce %15 uyguluyordu | test 7→14 |
| 2.4 | `/api/import` alan bazlı yetki (STOK/CARI/FINANS/FATURA `_WRITE`) + MUHASEBE erişimi | 15→24 |

Kritik ders: method-level `@PreAuthorize` class-level'ın **yerine geçer** (birleşmez). Faz 2.4'te rol filtresi metot ifadelerinin içine de yazıldı; SAHA senaryosu bu hatanın testte **kırmızı** olduğunu gösterdi.

Bilinçli ertelendi: `SirketService.konsolideOzet` 6×N (N küçük) · Dashboard 11 grafiğinin ayrıştırılması (görsel doğrulama yok) · Raporlar'ın kalan sekmeleri.

Doğrulama: backend **1664/1664** (1 skip) · frontend **1534/1534** · lint/i18n temiz · build başarılı · Docker healthy.

## Dalga 3 — Premium UX ve Veri Bütünlüğü (tamamlandı)

**Önceliklendirme yöntemi:** Denetim maddeleri sayı odaklıydı ve çoğu abartıydı. Her fazda gerçek sayım yapıldı; 5 maddeden 3'ü kapandı.

| Faz | Konu | Gerçek bulgu |
|---|---|---|
| 3.1 | Geri alınamaz işlem onayı | 45 silme çağrısının **42'sinde onay zaten vardı**; onaysız 3 gerçek riskti (şirket, API token, AI config) |
| 3.2 | Çift gönderim engeli | "21 view" değil: 35 view'da bayrak var, 24 korumalı, **11 korumasız**. Asıl risk `Faturalar` (Enter ile iki kez kayıt) |
| 3.3 | `ListeDurumu` bileşeni | 54 view'de bayrak var, yalnız 5'i iskelet basıyor → veri gelmeden "kayıt yok" |
| 3.3a | Bileşene geçiş | `Kategoriler` store loading'i hiç kullanmıyordu; `IskontoKurallari` hata halinde "kayıt yok" gösteriyordu |
| 3.4 | Ham tablo → ortak tablo | **Sayı tersiymiş**: ham `<table>` 7, `DataTable` 62 (zaten ortak). Kalan 7'nin hepsi gerekli (yazdırma/termal fış/AI/RBAC) |
| 3.5 | Erişilebilirlik | Odak tuzağı PrimeVue 4'te var. Gerçek açık: **yalnız `v-tooltip` olan 3 ikon buton** ekran okuyucuya ad taşımıyordu |

### Kapanan kurallar (regresyon kalıcı)
- `ciftGonderimKurali.spec.js` — kritik view listesi kural gibi test ediliyor
- `ikonErisilebilirlik.spec.js` — **hiçbir** ikon-only buton etiketsiz kalamaz
- `listeDurumuKullanimi.spec.js` — durum önceliği (iskelet > hata > boş) kaynakta sabit

### Bilinçli ertelendi
`TeklifListesi` kendi durum yönetimiyle `ListeDurumu`'na geçecek · kalan ~20 view'in boş durum metni ayrı çalışma gerektiriyor · vitest config'inde `PrimeVueResolver` yok, bu yüzden PrimeVue bileşenlerinin DOM'u mount testinde doğrulanamıyor.

Doğrulama: frontend **1578/1578** (108 dosya) · backend **1664/1664** · lint/i18n temiz · build başarılı.

## Planlı Epikler (v2.0)

### Epik 1 — Ürün Maliyet/ Kârlılık Analiz Sistemi (YARININ ODAĞI)

Gerçek alış/satış hareketlerinden ağırlıklı ortalama maliyet, satış ortalaması, tedarikçi/müşteri bazlı analiz ve kârlılık hesaplayan sistem.

- [x] **Faz 0-1:** Mevcut Product/Stock/Purchase/PurchaseItem/Sale/SaleItem/Cari/StokHareket yapısını analiz et ve raporla (entity, migration, API, servis, DTO, frontend, raporlama)
- [x] **Faz 2:** Veritabanı/entity tasarımı — yeni tablo gerekmedi; `V82` ile yalnızca analiz indexleri (`fatura_kalem(stok_id)`, `iade_kalem(stok_id)`, `iade(tur,durum,tarih)`); mevcut tablolar bozulmadı
- [x] **Faz 3:** Hareket kaynağı kararı — PURCHASE/SALE/RETURN rolünü mevcut `fatura_kalem` (KESILDI) + `iade_kalem` (TAMAMLANDI) üstlenir; `StokAnalizService` bu kaynakları birleştirir
- [x] **Faz 4:** Moving weighted average — alış/satış ortalamaları `Σ(miktar×netBirimFiyat)/Σ(miktar)` ile gerçek hareketlerden; kârlılık maliyeti `stok.fiyat` (mevcut moving avg) + `tedarikciFiyat` fallback
- [x] **Faz 5:** Alış/satış analiz servisleri (ort, ağırlıklı ort, ilk/son/min/max, toplam, tedarikçi/müşteri bazında, tarih aralığı)
- [x] **Faz 6:** Kârlılık hesabı (birim/toplam brüt kâr, brüt marj; iade/iskonto kuralları dahil)
- [x] **Faz 7:** REST API'leri — `StokController` altında `/api/stoklar/{id}/analiz|alis-ozet|satis-ozet|karlilik|tedarikci-analiz|musteri-analiz|islem-gecmisi|aylik-fiyat` (tarih aralıklı, tenant `sirketId`)
- [x] **Faz 8:** Unit (16) + H2 entegrasyon (1) + Postgres entegrasyon testleri (CI'da); 893 backend testi + JaCoCo gate yeşil
- [x] **Faz 9:** Vue ürün analiz ekranı
- [x] **Faz 10:** Filtreleme ve raporlama (grafik verisi: ay-bazlı ortalama alış/satış)
- [x] **Faz 11-12:** Performans/N+1 + security/auth kontrol (islem-gecmisi `limit` tavanı, ters tarih doğrulaması, tüm analiz endpoint'lerinde `@PreAuthorize` + tenant guard teyit)
- [x] **Faz 13:** Production gözden geçirme (899 backend test + JaCoCo gate, 170 frontend test + coverage, lint + i18n temiz, canlıya deploy + smoke test; işlem geçmişi sunucu tarafı sayfalama + cari/aylık liste üst sınırları)
- [ ] **Not:** İki epik de "önce analiz, sonra kademeli uygulama, mevcut sistemi bozma" metodolojisiyle yürütülecek. Her faz sonunda rapor.

### Epik 2 — Grup Sohbet Odaları · İkonlar · Ajanda · Kurumsal Dashboard · AI Altyapı

- [ ] **Bölüm A:** Grup sohbet odaları (ChatRoom/Member/Message + rollere OWNER/ADMIN/MEMBER, backend yetki, WebSocket gerçek zamanlı, last-50 + cursor pagination); birebir sohbet korunacak
- [ ] **Bölüm B:** WhatsApp/Mail/sohbet/ajanda/dashboard menü ikon tamamlama (mevcut ikon kütüphanesi)
- [ ] **Bölüm C:** Ajanda görev + hatırlatıcı (AgendaTask/AgendaReminder, takvim badge'i, bildirim, backend yetki)
- [ ] **Bölüm D:** Kurumsal tasarım dili (Tailwind config + PrimeVue tema token'ları) ve gerçek verili yeni dashboard grafikleri
- [ ] **Bölüm E:** AI altyapı iskeleti (AiService arayüzü, env tabanlı anahtar, asenkron çağrı; dashboard özeti / ajanda NL / sohbet özeti)

### Epik 3 — KOBİ Özellik Paketi (QR Sayım · Taksit Takvimi · Müşteri 360 · PWA Push)

Kullanıcının seçtiği 4 özellik; her faz keşif → backend → frontend → test/lint/i18n → canlı deploy + smoke.

- [x] **Faz 1 — QR/Barkod Hızlı Stok Sayımı + Raf Etiketleri:** Backend `GET /api/stoklar/barkod/{kod}` (mevcut `barkodIleBul`'u endpoint'e taşı), `GET /api/stoklar/{id}/etiket` (PDF + ZXing QR, rafNo/stokKodu/barkod/fiyat), `POST /api/stok-sayim/tarama` (barkod ile TASLAK upsert/sayım artırma); `durumGuncelle`'e cache evict + kritik stok bildirimi bağlantısı. Frontend `StokSayim.vue`'a `BarcodeScannerModal` tarama modu, `BarkodEtiketDialog`'da harici qrserver yerine backend QR PNG. **Ek:** `PdfRaporService` gömülü Unicode font (DejaVuSans) ile Türkçe karakter/PDF 500 hatası giderildi; `stokAdi` alan uyumsuzluğu düzeltildi. Canlı smoke: barkod arama, tarama TASLAK artırma, PNG/PDF etiket ✓
- [x] **Faz 2 — Taksit Planı & Tahsilat Takvimi:** V68/V69 altyapısı üzerine `finans.taksit` plan satırları (V83; vade/tutar/durum/planNo), `GET /api/taksitler` + `/yaklasan` + `/takvim` + `/ozet`, `POST /api/taksitler/plan` (otomatik vade bölme), `POST /api/taksitler/{id}/ode`, silme; TAKSIT tahsilatında `taksitId` ile kalem atama; frontend `TaksitTakvimi.vue` (aylık takvim + yaklaşan liste + yeni plan diyaloğu). Canlı smoke: plan/takvim/yaklaşan/öde/sil ✓
- [x] **Faz 3 — Müşteri 360 Kartı:** `CariHesap`'a `temsilciId`/`temsilciAd` (V84), `CariKartDTO` + `GET /api/cari-hesaplar/{id}/kart` (cari + kredi durumu/kullanılabilir limit/risk + sipariş/fatura/iade özeti + son kayıtlar + fırsatlar + notlar + özel fiyatlar); frontend `CariKart360Dialog` (tablı) + Cari listesinde 360 butonu. Canlı smoke ✓
- [x] **Faz 4 — PWA Web Push Bildirimi:** `sistem.push_abonelik` tablosu (V85), `nl.martijndwars:web-push` + BouncyCastle + VAPID env (gitignored `.env`), `PushSubscriptionController` (`/api/push/vapid-public-key`, `/abone`, `/test`), `BildirimKuyrukConsumer` → push köprüsü (WebPushService); frontend `injectManifest` SW (`src/sw.js`: precache + push + notificationclick), `usePushBildirim` composable + Bildirim Zili'nde izin/kapat UI, giriş sonrası otomatik abone. Canlı smoke: VAPID aktif, abone/abone-sil/test ✓ (gerçek push tarayıcı izni/HTTPS gerektirir)

- [x] **Ek iyileştirmeler (Epik 3 sonrası):** Müşteri 360 kartına **Taksitler** sekmesi eklendi (`CariKartDTO.taksitler` + `TaksitRepository` cari sorgusu); push bildirimleri artık **tür bazlı** hedefleniyor — `WebPushService` kullanıcı bildirim tercihlerini (`Kullanici.bildirimTercihleri`) whitelist olarak uygular, boş liste = tüm tipler açık; `BildirimZili` ve `HesapAyarları` tercihleri tek kanonik tip listesinde (STOK/SIPARIS/TEKLIF/TESLIMAT/FATURA/VADE/TAKSILAT/ODEME/MASRAF_TALEBI) birleştirildi ve Bildirim Zili tercihleri backend'e kalıcı yazıyor. Canlı smoke: 360→taksit listesi, tercih whitelist round-trip ✓

## Kısa Vadeli (v1.9.0)

- [x] Kalan servis/controller test kapsamını tamamlama (Ajanda, Bildirim, Crm, Tahsilat, TekrarlayanFatura, Yetki vb.) — 59 controller + 146 test sinifi mevcut
- [ ] Büyük view dosyalarının (Dashboard, HizliSatis, Stoklar, Teklifler) alt bileşenlere ayrıştırılması
- [x] CI'da JaCoCo/Vitest coverage eşik (gate) tanımlanması (JaCoCo INSTR %50 / BRANCH %30 / LINE %55; Vitest global eşikler)
- [x] i18n eksik anahtar otomatik kontrol script'i (`scripts/check-i18n.mjs` + CI'de `npm run i18n:check`)
- [x] Cypress E2E suite'in CI'ye bağlanması (7 spec, dev-server üzerinde, auto-retry)

## Orta Vadeli

- [ ] API sürümleme (`/api/v1/`) tutarlı şekilde devreye alınması (şu an tümü `/api/`)
- [ ] SonarQube / SpotBugs statik analiz entegrasyonu
- [ ] Bulut yedekleme (S3/GDrive/Dropbox) uç nokta doğrulaması ve otomatik restore testi

## Uzun Vadeli

- [ ] Çoklu bölge (multi-region) yedeklilik ve failover
- [ ] Detaylı metrik/alert kural genişletme (SLO, hata oranı, p95 gecikme)
- [ ] Performans/load test otomasyonunun CI'ye bağlanması (`scripts/load-test.mjs`)

## Prod Hazırlık Kontrol Listesi

Canlıya geçiş öncesi zorunlu adımlar `docs/GO-LIVE.md` dosyasında tutulur.
