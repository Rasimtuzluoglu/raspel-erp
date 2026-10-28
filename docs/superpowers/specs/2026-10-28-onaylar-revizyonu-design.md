# Onaylar Revizyonu — Tasarım Spesifikasyonu

**Tarih:** 2026-10-28  
**Kapsam:** Birleşik onay API’si, karar geçmişi, toplu karar, detay drawer, onay ayarlarının ayrılması  
**Birincil kullanıcı:** Yönetici / onaycı  
**Durum:** Tasarım onaylandı

## 1. Amaç
İzin, masraf/avans, satınalma ve sipariş onaylarını tek ekranda, sunucu filtreli, toplu işlemli ve gerekçeli karar geçmişiyle yönetmek; onay ayarlarını ayrı yönetim ekranına taşımak.

## 2. Karar modeli (V172)
`sistem.onay_karar`: `id, sirket_id, tur (IZIN/MASRAF/SATINALMA/SIPARIS), kaynak_id, karar (ONAY/RED), gerekce, onceki_durum, yeni_durum, karar_veren_id, karar_veren_ad, olusturma_tarihi`.
- Yalnız yeni kararlar; geriye dönük backfill yok.
- Modül işlemi başarılı olduktan sonra yazılır; hata olursa karar yok.
- İndeksler: `(sirket_id, olusturma_tarihi)`, `(tur, kaynak_id)`.

## 3. API
- `GET /api/onaylar/ozet` → `{toplam, izin, masraf, satinalma, siparis}`.
- `GET /api/onaylar/bekleyen?tur&q&page&size` → `Page<OnayBekleyenDTO>`; `tur` zorunlu değil (Tümü’de tür başına ilk 100 birleşik).
- `POST /api/onaylar/karar` `{tur,kaynakId,karar,gerekce}` → tek karar; RED’de gerekçe min 3.
- `POST /api/onaylar/toplu` `{tur, kaynakIds[], karar, gerekce}` → aynı tür zorunlu; kayıt bazında bağımsız; `{basarili, atlanan[]}`.
- `GET /api/onaylar/gecmis?tur&karar&q&baslangic&bitis&page&size` → karar geçmişi.
- `GET /api/exports/onaylar?tur&q` → bekleyenler Excel.
- Mevcut modül uçları korunur.

## 4. Modül eşlemesi
| Tür | Onay | Red |
|---|---|---|
| IZIN | `durumGuncelle(id,"ONAYLANDI",jwtOnaylayan)` | `"REDDEDILDI"` |
| MASRAF | `talepService.onayla(id,onaylayan,not)` | `reddet(id,onaylayan,not)` |
| SATINALMA | `durumGuncelle(id,"ONAYLANDI")` | `"REDDEDILDI"` |
| SIPARIS | `durumGuncelle(id,"HAZIRLANIYOR")` | `"IPTAL"` |

Yetki ADMIN/MUHASEBE; onaylayan kimliği istemciden alınmaz (JWT/context).

## 5. Frontend
- `PageHeader` + 5 KPI: Toplam, İzin, Masraf/Avans, Satınalma, Sipariş.
- Sekmeler: Tümü / İzin / Masraf / Satınalma / Sipariş / Geçmiş; hafıza `raspel_onaylar_tab`.
- Sunucu sayfalama/arama; kart+liste; skeleton; mobil kart; a11y.
- Çoklu seçim (aynı tür) + toplu onay/red; red için ortak gerekçe modalı; sonuç özeti.
- Ret gerekçesi `prompt()` yerine modal.
- Detay drawer: ortak alanlar + türe özel detay + ilgili kayda git + aksiyonlar.
- Geçmiş: karar/tür/kayıt/kullanıcı/tarih/gerekçe + filtre.
- **Onay ayarları ayrı ekran**: `/onay-ayarlari` + menü; mevcut tablo/eşik/otomatik onay korunur.
- i18n tr/en; responsive `92vw`.

## 6. Test
Backend: karar tablosu, tür eşleme, tek/toplu karar (kısmi hata), RED gerekçe, geçmiş filtre, özet/bekleyen, Excel, yetki/tenant.
Frontend: KPI/sekme/hafıza, filtre, toplu seçim/karar, ret modalı, drawer, geçmiş, ayarlar rotası, mobil/a11y.
Kapılar: ESLint 0, i18n, Vitest, `mvn clean verify`, Docker, V172.

## 7. Riskler
- Tümü görünümünde birleşik sayfalama → tür başına 100 sınırı dokümante.
- Toplu kararda kısmi başarı → kayıt bazında transaction + özet.
- Modül işlemi başarısızken karar yazılması → önce modül, sonra log.
- Yetki tutarsızlığı → ADMIN/MUHASEBE guard.
