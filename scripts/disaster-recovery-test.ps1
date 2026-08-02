# RasPel ERP - Felaket Kurtarma (Backup + Restore) Testi
# Bu test gerçek bir PostgreSQL üzerinde: veri yazar -> yedek alır -> veriyi siler (felaket) -> geri yükler -> doğrular.
# Kullanım: powershell -File scripts/disaster-recovery-test.ps1

$ErrorActionPreference = "Continue"
$CONTAINER = "raspel-dr-test"
$PG_IMAGE = "postgres:16-alpine"
$HOST_PORT = 5545
$BACKUP_FILE = "C:\Users\RASIMT~1\AppData\Local\Temp\opencode\raspel-dr-backup.sql.gz"

Write-Host "=== RasPel ERP Felaket Kurtarma Testi ===" -ForegroundColor Cyan

# 1. Test PostgreSQL'i başlat
Write-Host "`n[1/5] Test PostgreSQL baslatiliyor..." -ForegroundColor Yellow
try { docker rm -f $CONTAINER 2>&1 | Out-Null } catch {}
docker run -d --name $CONTAINER -e POSTGRES_PASSWORD=test -p "${HOST_PORT}:5432" $PG_IMAGE | Out-Null
Start-Sleep -Seconds 6

# 2. Örnek veri yaz
Write-Host "[2/5] Ornek veri yaziliyor (fatura + stok + schema)..." -ForegroundColor Yellow
docker exec $CONTAINER psql -U postgres -v ON_ERROR_STOP=1 -c @"
CREATE SCHEMA sistem; CREATE SCHEMA fatura;
CREATE TABLE fatura.fatura (id SERIAL PRIMARY KEY, fatura_no VARCHAR(20), tutar NUMERIC(12,2), olusturma_tarihi TIMESTAMP DEFAULT now());
INSERT INTO fatura.fatura (fatura_no, tutar) VALUES ('FTR-2026-000001', 1250.50), ('FTR-2026-000002', 3400.00), ('FTR-2026-000003', 99.99);
"@ | Out-Null
$oncekiAdet = docker exec $CONTAINER psql -U postgres -t -A -c "SELECT COUNT(*) FROM fatura.fatura;"
Write-Host "   Olusturulan kayit sayisi: $oncekiAdet"

# 3. Yedek al (BackupService ile aynı yöntem: pg_dump + gzip)
Write-Host "[3/5] Yedek aliniyor (pg_dump + gzip)..." -ForegroundColor Yellow
docker exec $CONTAINER sh -c "PGPASSWORD=test pg_dump -h localhost -U postgres -d postgres --no-owner --no-acl | gzip > /tmp/backup.sql.gz"
docker cp "${CONTAINER}:/tmp/backup.sql.gz" $BACKUP_FILE | Out-Null
$boyut = (Get-Item $BACKUP_FILE).Length
Write-Host "   Yedek boyutu: $([math]::Round($boyut/1024, 1)) KB"

# 4. FELAKET: veritabanını tamamen sil
Write-Host "[4/5] FELAKET SIMULASYONU: veritabani siliniyor..." -ForegroundColor Red
docker exec $CONTAINER psql -U postgres -c "DROP SCHEMA fatura CASCADE; DROP SCHEMA sistem CASCADE;" 2>&1 | Out-Null
$sonrakiAdet = docker exec $CONTAINER psql -U postgres -t -A -c "SELECT COUNT(*) FROM pg_tables WHERE schemaname IN ('fatura','sistem');"
Write-Host "   Felaket sonrasi tablo sayisi: $sonrakiAdet (0 beklenir)"
if ($sonrakiAdet -ne "0") { Write-Host "FATAL: Felaket simulasyonu basarisiz" -ForegroundColor Red; docker rm -f $CONTAINER | Out-Null; exit 1 }

# 5. Yedekten geri yükle
Write-Host "[5/5] Yedekten geri yukleniyor..." -ForegroundColor Yellow
docker cp $BACKUP_FILE "${CONTAINER}:/tmp/backup.sql.gz" | Out-Null
docker exec $CONTAINER sh -c "gunzip -c /tmp/backup.sql.gz | psql -U postgres -v ON_ERROR_STOP=1" 2>&1 | Select-Object -Last 3

# Doğrula
$geriAdet = docker exec $CONTAINER psql -U postgres -t -A -c "SELECT COUNT(*) FROM fatura.fatura;"
$ilkKayit = docker exec $CONTAINER psql -U postgres -t -A -c "SELECT fatura_no || '|' || tutar FROM fatura.fatura ORDER BY id LIMIT 1;"
Write-Host "`n=== SONUC ===" -ForegroundColor Cyan
Write-Host "Geri yuklenen kayit sayisi: $geriAdet (3 beklenir)"
Write-Host "Ilk kayit kontrolu: $ilkKayit (FTR-2026-000001|1250.50 beklenir)"

$temizlik = docker rm -f $CONTAINER | Out-Null

if ($geriAdet -eq "3" -and $ilkKayit -match "FTR-2026-000001") {
    Write-Host "`nTEST BASARILI ✅ - Yedekleme ve geri yukleme dogru calisiyor." -ForegroundColor Green
    exit 0
} else {
    Write-Host "`nTEST BASARISIZ ❌ - Veri kaybi var!" -ForegroundColor Red
    exit 1
}
