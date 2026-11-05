# Anasayfa — Mobil & PWA İnce Ayar (Faz 5)

**Tarih:** 2026-11-05
**Kapsam:** 390px mobil yerleşim, üst bar sadeleştirme, kurulum/çevrimdışı deneyimi, manifest.

## 1. Mobil üst bar (≤760px)
- Üst bar 10 kontrolü tek satıra sığmıyor. Mobilde:
  - **Birincil:** dönem seçici (tam satır), Ara (ikon), Bildirim zili, **Daha fazla** (⋯).
  - **Daha fazla** alt sayfası: Notlar, Vade Takvimi, Excel indir, Yazdır, Canlı mod anahtarı, Yenile, Düzenle.
- Canlı/çevrimdışı durumu alt satırda kalır.
- Masaüstü görünümü değişmez; gizlenenler yalnız CSS + mobil menü.
- 390px’te yatay taşma yok; dokunma hedefleri ≥44px.

## 2. Mobil KPI ve kartlar
- KPI: 360–699px arası **2 kolon**, <360 tek kolon; ≤430’da değer 18-20px, taşma ellipsis.
- Görev satırı: adet/tutar ve aksiyon mobilde alt satıra sarar; 44px hedef.
- Grafik yükseklikleri mobilde 140px; kart padding 12px.
- Alt menü (72px) + safe-area kadar içerik alt boşluğu; son kart gizlenmez.
- Drawer’lar `100dvh`, alt safe-area; gövde kaydırma kilidi.

## 3. PWA manifest
- `shortcuts`: Onaylar `/onaylar`, Faturalar `/faturalar`, Kasa `/kasa`, Stoklar `/stoklar` (ikon `icon-192.png`).
- `preferred_related_applications: false`, `display_override` korunur.
- Mevcut ikonlar; maskable için güvenli alan notu.

## 4. Kurulum ve çevrimdışı
- `PwaKurulum.vue`: `beforeinstallprompt` → alt menünün üstünde “RasPel’i ana ekrana ekle” bandı; Ekle/Kapat; 30 gün hatırla; `appinstalled` ile gizle.
- iOS Safari (standalone değil): “Paylaş → Ana Ekrana Ekle” yönlendirmesi (tek sefer).
- `useCevrimici`: `online/offline` olayları; çevrimdışıyken üstte ince amber bant “Çevrimdışısınız – son veriler gösteriliyor”.
- Kurulum bandı ve çevrimdışı bant `aria-live`, klavye, mobil güvenli alan.

## 5. Test
- 390px: üst bar taşma yok, KPI 2 kolon, Daha fazla menüsü satırları.
- Kurulum bandı: beforeinstallprompt, ekle, kapat/kalıcılık, standalone’da gizli.
- Çevrimdışı bant: offline/online geçişi.
- Manifest shortcuts (build çıktısı).
- Mevcut testler kırılmaz; Cypress 390 smoke.
