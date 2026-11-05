# Anasayfa — Gerçek Zamanlı Güncelleme (Faz 4)

**Tarih:** 2026-11-05
**Kapsam:** Veri değiştiğinde anasayfanın STOMP üzerinden otomatik tazelenmesi.

## Backend
- Yeni `DashboardYayinService.yayinla(sirketId, tur, mesaj)`:
  - `SimpMessagingTemplate.convertAndSend("/topic/dashboard/{sirketId}", {tur, mesaj, zaman})`.
  - Aktif transaction varsa **afterCommit** yayınla (`TransactionSynchronizationManager`), yoksa hemen.
- Yayın noktaları (başarılı mutasyon sonrası):
  - `FaturaService` → `FATURA`
  - `HareketService` → `HAREKET`
  - `SiparisService` → `SIPARIS`
  - `StokHareketService`/`StokService` → `STOK`
  - `OnayService` → `ONAY`
  - `MasrafService` → `MASRAF`
  - `KasaService`/`BankaService` → `KASA`/`BANKA`
- **Güvenlik:** `JwtChannelInterceptor` içindeki `SIRKET_TOPIC_PATTERN` → `^/topic/(bildirimler|dashboard)/(\\d+)$`; abone şirket = oturum şirketi değilse reddet. Test ekle.
- Migration yok.

## Frontend
- `useWebSocket` **tek** SockJS/STOMP bağlantısına dönüştürülür (modül seviyesinde singleton + abone kayıtları). Bildirim zili ve dashboard aynı bağlantıyı paylaşır; son abone ayrılınca kapatılır.
- Yeni `useDashboardCanli`:
  - `/topic/dashboard/{sirketId}` aboneliği; `bagli` ve `sonOlay`.
  - Yeniden bağlanınca tam yenileme tetikle (kaçırılan olaylar için).
- `Dashboard.vue`:
  - Olayda 800 ms debounce ile `getOzet()` + `getAkis()`; `FATURA/SIPARIS/STOK/MASRAF/ONAY` ise `getGrafikler()` en fazla 5 sn’de bir.
  - Sekme gizliyse beklet, görünür olunca tazele.
  - Üst barda **Canlı** göstergesi: bağlıyken yeşil nabız, kopunca “yeniden bağlanıyor”; mevcut oto-yenileme anahtarı canlı modu açar/kapatır.
  - STOMP kopuksa ve canlı mod açıksa 60 sn yedek polling.
  - Her olayda toast yok; `aria-live` ile sessiz “güncellendi” duyurusu.
- Testler: yayın servisi, interceptor tenant reddi, frontend abonelik/debounce/sekme görünürlüğü/reconnect.

## Doğrulama
Backend `clean verify`; frontend lint/i18n/vitest/build; Docker; iki farklı şirketle abonelik reddi.
