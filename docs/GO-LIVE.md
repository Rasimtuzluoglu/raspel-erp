# RasPel ERP - Production Go-Live Checklist

> Bu liste, sistemi canliya almadan once kontrol edilmesi gereken maddeleri icerir.
> Her madde gecildikten sonra isaretleyin.

## Guvenlik

- [ ] `JWT_SECRET` ortam degiskeni guclu bir degerle ayarlandi (en az 256-bit)
- [ ] `POSTGRES_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `REDIS_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `RABBITMQ_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `GRAFANA_PASSWORD` ortam degiskeni guclu bir degerle ayarlandi
- [ ] `DASHBOARD_BASIC_AUTH` ortam degiskeni htpasswd hash'i ile ayarlandi (Traefik dashboard/adminer; yoksa fail-closed)
- [ ] `app.cors.allowed-origins` gercek domain'i iceriyor
- [ ] Veritabani portu (5432) dis dunyaya acik degil (firewall'dan kapali)
- [ ] Backend 8081 ve Traefik 8080 portlari yalnizca 127.0.0.1'e bagli (docker-compose varsayilani)

## Yapilandirma

- [ ] `docker-compose.yml`'de `SPRING_PROFILES_ACTIVE=prod`
- [ ] `SPRING_MAIL_*` ortam degiskenleri gercek SMTP sunucusu ile ayarlandi
- [ ] `ACME_EMAIL` ortam degiskeni gecerli bir e-posta (Let's Encrypt icin)
- [ ] `app.backup.dir` erisilebilir bir dizine isaret ediyor
- [ ] `app.admin.*` degiskenleri kontrol edildi, demo admin kullanicisi yok

## Veritabani

- [ ] Flyway migration'lari hatasiz calisti
- [ ] `sistem.kullanici` tablosunda demo hesaplar (admin/admin123 vb.) yok
- [ ] Yedekleme calisiyor (`/actuator/health` uzerinden kontrol edin)
- [ ] Otomatik yedekleme cron'u dogru zamana ayarlandi

## Monitoring

- [ ] Prometheus backend'i scrape edebiliyor
- [ ] Grafana dashboard'lari veri gosteriyor
- [ ] Alertmanager bildirim kanali (Slack/E-posta) yapilandirildi
- [ ] `/actuator/health` uzerinden tum servisler UP durumda

## Test

- [ ] Backend testleri gecti (`mvn -B test` -> 820 test)
- [ ] Frontend build alindi (`npm run build`) ve lint/test temiz (158 test)
- [ ] Farkli sirket kullanicilariyla tenant izolasyonu test edildi (negatif senaryolar dahil)
- [ ] Login/logout/2FA akisi test edildi
- [ ] Flyway migration'lari bos bir PostgreSQL'de sifirdan calisti (V61 dahil)

## Son Kontrol

- [ ] `docker-compose up -d` ile tum servisler ayaga kalkiyor
- [ ] Tarayicidan giris yapilabiliyor
- [ ] `.env` dosyasi sunucuda mevcut ve dogru degerlerle dolu
- [ ] `.env` dosyasi `.gitignore`'da ve commit'lenmemis
- [ ] Repo public ise tum secret'lar rotate edildi
