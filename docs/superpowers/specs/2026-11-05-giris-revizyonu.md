# Giriş Ekranı Kapsamlı Revizyonu

**Tarih:** 2026-11-05
**Durum:** Onaylandı (kullanıcı kararları)
**Kapsam:** Giriş UX'i, görsel dil, şirket kodu, passkey, QR oturum onayı, dil değiştirici, destek/durum.

## 1. Kararlar

| Konu | Karar |
|---|---|
| Hedef | Kapsamlı yeniden tasarım |
| Açılış | Her ziyarette marka ekranı, atlanabilir (~1,2 sn) |
| Yerleşim | İki adım: marka açılışı → sade giriş formu |
| Görsel dil | Koyu teal, sakin ve premium |
| Hedef kitle | Karma (iç personel + müşteri + saha) |
| Şirket kodu | İsteğe bağlı; yeni benzersiz `sirket.kod` alanı; otomatik slug + düzenlenebilir |
| Passkey | Birincil/parolasız; kayıtta parola onayı; kullanıcı + ADMIN yönetimi |
| QR 2FA | Telefonla oturum onayı; yalnız 2FA açık kullanıcılar için TOTP alternatifi |
| Dil | Giriş ekranında TR/EN |
| Destek/durum | `destek@raspel.com.tr`, `/kullanim-sartlari`, yeni public `/durum` |
| Kaldırılanlar | Hızlı rol çipleri, paralaks, dönen sloganlar, 1,5 sn karşılama overlay'i |

## 2. UX akışları

### 2.1 Açılış ekranı
- Tam ekran koyu zemin; logo + RasPel ERP wordmark + tek satır vaat + ince ilerleme çizgisi.
- Sağ üstte **Atla**; `prefers-reduced-motion` → 250 ms, aksi halde 1200 ms.
- Açılış sırasında form arkada hazır; odak çalınmaz. Bitince (veya Atla) kullanıcı adı boşsa ona odaklanır.
- `role="status"`, `aria-live="polite"`; atlama düğmesi klavye erişimli.

### 2.2 Giriş formu
- Masaüstü: solda statik marka paneli (başlık, 3 değer maddesi, statik ürün önizlemesi, güven satırı), sağda form kartı.
- Mobil (<960px): marka paneli gizlenir; kompakt logo + tek satır vaat + form.
- Form alanları:
  1. Şirket kodu (isteğe bağlı, katlanabilir; `autocomplete="organization"`)
  2. Kullanıcı adı (`autocomplete="username"`)
  3. Parola (`autocomplete="current-password"`, göster/gizle, Caps Lock uyarısı)
  4. Beni hatırla
  5. **Giriş yap** (birincil)
  6. **Passkey ile giriş** (destekleniyorsa; ikincil)
  7. Şifremi unuttum
- Alt bilgi: dil değiştirici (TR/EN), sistem durumu noktası + `/durum`, destek e-postası, KVKK, sürüm.
- Hata: form üstünde `role="alert"`, alan bazlı `aria-invalid`; sunucu mesajı korunur.

### 2.3 Şirket kodu
- Girilirse backend kodu çözer, kullanıcının üyeliğini doğrular; yalnız o şirket döner.
- Tek şirket → seçim ekranı atlanır, doğrudan giriş tamamlanır.
- Kod yok + çok şirket → mevcut şirket seçimi ekranı.
- Hatalı kod/kullanıcı → genel hata mesajı (tenant enumerasyonu yok).

### 2.4 Passkey
- Destek: `window.PublicKeyCredential` + `navigator.credentials`.
- Giriş: kullanıcı adı gerekmeden discoverable credential; başarıda JWT + kullanıcı bilgisi.
- Çok şirket + kod yok → şirket seçimi.
- Kayıt: Hesap Ayarları → Güvenlik; parola onayı + cihaz etiketi; platform authenticator.
- Liste/silme; ADMIN kullanıcı detayından tüm passkey'leri sıfırlama.
- Passkey girişi güçlü kimlik sayılır; TOTP istenmez.

### 2.5 QR oturum onayı
- Tetik: parola sonrası 2FA adımında **QR ile onayla**.
- Masaüstü: `girisToken`'a bağlı tek kullanımlık QR; 120 sn geri sayım; "Kodu elle gir" fallback.
- Mobil: QR `/qr-onay/:token` adresini açar; oturum açmış olmalı; aynı kullanıcı olmalı.
- Mobil onay ekranı: tarayıcı/IP özeti, şirket, **Onayla / Reddet**.
- Onay sonrası masaüstü poll ile tamamlar; token tek kullanım, süre aşımında 410.
- 2FA kapalı kullanıcıda QR seçeneği gösterilmez (backend de reddeder).

## 3. Görsel tasarım

### 3.1 Tokenlar
- Zemin: `#070b10` → `#0b1219` dikey gradyan; ince mesh + grid (CSS, blur/canvas yok).
- Yüzey: `rgba(16, 24, 33, .78)`, kenar `rgba(148, 163, 184, .14)`, radius 18px.
- Vurgu: mevcut teal (`--accent`), hover `--accent-hover`, soft `--accent-soft`.
- Metin: birincil `#e8eef5`, ikincil `#9fb0c0`, sessiz `#6f8294`.
- Tipografi: başlık 30/36, `-0.02em`; gövde 13,5/1,55; etiket 12,5/600.
- Boşluk: 8px ölçek; kart içi 28–32px; alan arası 14px.
- Gölge: `0 24px 60px rgba(0,0,0,.45)`; iç ışık ince.
- Hareket: 160–220 ms `ease-out`; yalnız opaklık/transform; reduced-motion'da kapalı.

### 3.2 Bileşenler
- `GirisAcilis.vue` (yeni): marka açılış overlay'i.
- `GirisMarkaPaneli.vue` (yeni): statik sol panel; `GirisHeroPaneli` kaldırılır.
- `GirisFormKarti.vue` (yeni): form + adımlar; `Giris.vue` akış/orkestrasyon.
- `GirisAltBilgi.vue` (yeni): dil, durum, destek, KVKK, sürüm.
- `LoginPreview.vue`: statik vitrin olarak sadeleştirilir (otomatik döngü yok).
- `PasskeyGirisButonu.vue`, `QrOnayPaneli.vue`, `QrOnaySayfasi.vue` (faz 2/3).
- `HesapAyarlari.vue` güvenlik sekmesine passkey yönetimi eklenir.
- `Sirketler.vue` formuna **Şirket Kodu** alanı eklenir.

### 3.3 Erişilebilirlik
- Tüm alanlarda görünür etiket; `aria-describedby` hata/ipucu.
- Kontrast AA; odak halkası 2px teal; dokunma hedefi ≥44px.
- Klavye: splash atla → kullanıcı adı; 2FA'da kod alanı; QR panelinde iptal.
- Hata ve başarı duyuruları `role="alert"` / `aria-live`.

## 4. Backend sözleşmeleri

### 4.1 Migration V175 — şirket kodu
```sql
ALTER TABLE sistem.sirket ADD COLUMN IF NOT EXISTS kod VARCHAR(60);
-- ad → slug (TR karakter dönüşümü), boş/çakışma düzeltmesi, sonra:
ALTER TABLE sistem.sirket ALTER COLUMN kod SET NOT NULL;
CREATE UNIQUE INDEX ux_sirket_kod ON sistem.sirket (LOWER(kod));
```
- `Sirket` / `SirketDTO` + `kod`.
- `SirketService`: boşsa üret, `^[a-z0-9][a-z0-9-]{1,58}[a-z0-9]$`, rezerve kelimeler (`api`, `giris`, `admin`, `app`, `www`, `durum`, `qr-onay`), benzersizlik.
- `SirketRepository.findByKodIgnoreCase`.

### 4.2 Giriş
`POST /api/kullanicilar/giris`
```json
{ "username": "...", "password": "...", "sirketKodu": "abc-holding", "rememberMe": false }
```
- `sirketKodu` varsa çöz + üyelik doğrula; `sirketler` yalnız o şirkete filtrelenir.
- İstenen şirket `giris:sirket:{girisToken}` (TTL 5 dk) olarak saklanır; 2FA sonrası yeni token'a taşınır.
- `giris-sirket` istenen şirket varsa yalnız onu kabul eder.

### 4.3 Passkey (Faz 2)
- `POST /api/kullanicilar/passkey/kayit/secenekler` (auth, parola) → `{ challengeId, publicKey }`
- `POST /api/kullanicilar/passkey/kayit` (auth) → kayıt
- `GET /api/kullanicilar/passkeyler` / `DELETE /api/kullanicilar/passkeyler/{id}`
- `POST /api/kullanicilar/passkey/giris/secenekler` (public) → `{ challengeId, publicKey }`
- `POST /api/kullanicilar/passkey/giris` (public) → `LoginResponse` veya `girisToken` + `sirketler`
- `DELETE /api/kullanicilar/{id}/passkeyler` (ADMIN)
- Migration V176 `sistem.passkey`.
- WebAuthn4J; challenge Redis (`passkey:reg:*`, `passkey:auth:*`); RP ID/origin config.

### 4.4 QR onayı (Faz 3)
- `POST /api/kullanicilar/qr-oturum` (public, `{girisToken}`) → `{ token, qrUrl, expiresIn }`
- `GET /api/kullanicilar/qr-oturum/{token}/gorsel` (public) → PNG (ZXing)
- `GET /api/kullanicilar/qr-oturum/{token}` (public) → `{ durum, kalanSn }`
- `POST /api/kullanicilar/qr-oturum/{token}/onayla` (auth, `{sirketId?}`)
- `POST /api/kullanicilar/qr-oturum/{token}/reddet` (auth)
- `POST /api/kullanicilar/giris-qr-tamamla` (public, `{girisToken, rememberMe}`) → `LoginResponse`
- Redis `qr:oturum:{token}` TTL 120 sn; onay yalnız aynı kullanıcı; tek kullanım; audit.

### 4.5 Durum
- `GET /api/durum` public → `{ durum, surum, zaman }`; detay sızdırmaz.
- `SecurityConfig` permitAll; login alt bilgisi ve `/durum` sayfası kullanır.

## 5. Test ve kabul

### Backend
- Şirket kodu: üretim, doğrulama, benzersizlik, giriş filtreleme, 2FA sonrası taşıma.
- Passkey: seçenek üretimi, başarısız doğrulama, kayıt/liste/silme, parola onayı, ADMIN sıfırlama.
- QR: token TTL, yanlış kullanıcı reddi, onay/red, tek kullanım, süre aşımı.
- Durum endpoint'i public ve detay sızdırmıyor.

### Frontend
- Splash: 1200 ms, atla, reduced-motion, odak.
- Form: şirket kodu, hatalar, caps lock, beni hatırla, dil değişimi, alt bilgi.
- Passkey: desteklenmiyorsa gizli; başarı/iptal; Hesap Ayarları listeleme/silme.
- QR: panel aç/kapa, poll, onay/red, süre aşımı; mobil onay sayfası.
- i18n tr/en simetrik; lint 0 uyarı; build.
- Cypress: splash atla → giriş; şirket kodu; 390px taşma yok; QR panel (API intercept).

### Kapılar
`mvn -B test`, `npm run lint`, `npm run i18n:check`, `npm run test`, `npm run build`, Docker build/deploy, canlı E2E.

## 6. Fazlar

1. **Faz 1 — Tasarım + şirket kodu + dil/durum:** V175, giriş sözleşmesi, yeni giriş UI, splash, alt bilgi, `/durum`, testler.
2. **Faz 2 — Passkey:** V176, WebAuthn4J, uçlar, giriş butonu, Hesap Ayarları yönetimi, testler.
3. **Faz 3 — QR onay:** Redis challenge, QR uçları, mobil onay sayfası, masaüstü paneli, testler.
4. **Faz 4 — Sertleştirme:** oran sınırlama, audit, dokümantasyon, Cypress, Docker, canlı doğrulama.

## 7. Kapsam dışı
- SSO (Microsoft/Google) — bu fazda yok.
- Müşteri portalı ayrı giriş alanı — aynı ekran, rol bazlı yönlendirme.
- Biyometrik donanım testleri — tarayıcı/WebAuthn mock ile.
