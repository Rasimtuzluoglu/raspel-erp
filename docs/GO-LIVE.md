# RasPel ERP - Production Go-Live Checklist

> Bu liste, sistemi canliya almadan once kontrol edilmesi gereken maddeleri icerir.
> Her madde gecildikten sonra isaretleyin.

## Guvenlik

- [ ] `JWT_SECRET` ortam degiskeni guclu bir degerle ayarlandi (en az 256-bit)
- [ ] `POSTGRES_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `REDIS_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `RABBITMQ_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `GRAFANA_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `DASHBOARD_BASIC_AUTH` ortam degiskeni htpasswd hash'i ile ayarlandi
- [ ] `DASHBOARD_MIDDLEWARES=dashboard-auth@file` ayarlandi (yoksa dashboard basicAuth KORUMASIZ kalir; port 8080 loopback'te oldugu icin disaridan acilmaz ama iceriden herkes erisebilir)
- [ ] `app.cors.allowed-origins` gercek domain'i iceriyor
- [ ] Veritabani portu (5432) dis dunyaya acik degil (firewall'dan kapali)
- [ ] Backend 8081 ve Traefik 8080 portlari yalnizca 127.0.0.1'e bagli (docker-compose varsayilani)

## Yapilandirma

- [ ] `docker-compose.yml`'de `SPRING_PROFILES_ACTIVE=prod`
- [ ] `SPRING_MAIL_*` ortam degiskenleri gercek SMTP sunucusu ile ayarlandi
- [ ] `ACME_EMAIL` ortam degiskeni gecerli bir e-posta (Let's Encrypt icin)
- [ ] `APP_DOMAIN` gercek alan adini iceriyor — `localhost` OLMAMALI. `localhost` iken Let's Encrypt sertifika veremedigi icin Traefik'in uretim router'lari tanimlanmaz (`config/traefik/dynamic.yml`), HTTPS kendi self-signed sertifikasiyla kalir ve HSTS nedeniyle kullanici guvenlik uyarisi gorur
- [ ] `app.backup.dir` erisilebilir bir dizine isaret ediyor
- [ ] `app.admin.*` degiskenleri kontrol edildi, demo admin kullanicisi yok
- [ ] Bos sistemde onboarding yalnizca firma + yonetici olusturuyor; demo veri yukleme ozelligi yok

## Veritabani

- [ ] Flyway migration'lari hatasiz calisti
- [ ] `sistem.kullanici` tablosunda demo hesaplar (admin/admin123 vb.) yok
- [ ] Teslim oncesi DB sifirdan kuruldu (`docker-compose down -v`); `sistem.sirket`'te yalnizca musteri firmasi var, `RasPel Test`/`YUK TESTI` gibi kayitlar yok
- [ ] Yedekleme calisiyor (`/actuator/health` uzerinden kontrol edin)
- [ ] Otomatik yedekleme cron'u dogru zamana ayarlandi

## Monitoring

- [ ] Prometheus backend'i scrape edebiliyor
- [ ] Grafana dashboard'lari veri gosteriyor
- [ ] Alertmanager bildirim kanali (Slack/E-posta) yapilandirildi
- [ ] `/actuator/health` uzerinden tum servisler UP durumda

## Test

- [ ] Backend testleri gecti (`mvn -B clean verify` -> 1346 test, JaCoCo gate dahil)
- [ ] Frontend build alindi (`npm run build`) ve lint/test temiz (814 test, coverage gate dahil)
- [ ] Cypress E2E suite CI'da gecti (9 spec)
- [ ] `npm run i18n:check` temiz (eksik/cop anahtar yok)
- [ ] Farkli sirket kullanicilariyla tenant izolasyonu test edildi (negatif senaryolar dahil)
- [ ] Login/logout/2FA akisi test edildi
- [ ] Flyway migration'lari bos bir PostgreSQL'de sifirdan calisti (V144 dahil)

## Son Kontrol

- [ ] `docker-compose up -d` ile tum servisler ayaga kalkiyor (`docker compose ps` -> hepsi `Up`, saglik kontrollu olanlar `healthy`)
- [ ] Uygulamaya **https://\<APP_DOMAIN\>** adresinden giris yapilabiliyor ve sertifika Let's Encrypt'ten geliyor (tarayici kilidinde uyari YOK)
- [ ] `curl -sI http://\<APP_DOMAIN\>/` -> `301` (HTTPS'e yonlendirme)
- [ ] `curl -sI https://\<APP_DOMAIN\>/api/actuator/health` -> `401` (kimliksiz API erisimi reddediliyor)
- [ ] `docker logs raspel-traefik` icinde `rejectedIdentifier` / ACME hatasi YOK
- [ ] Traefik dashboard `http://127.0.0.1:8080/dashboard` adresinden aciliyor ve basicAuth SORUYOR
- [ ] `.env` dosyasi sunucuda mevcut ve dogru degerlerle dolu
- [ ] `.env` dosyasi `.gitignore`'da ve commit'lenmemis
- [ ] Repo public ise tum secret'lar rotate edildi
