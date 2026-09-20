# RasPel ERP - Yuk Testi Veri Ureticisi (runner)
# scripts/test-data-generator.sql dosyasini PostgreSQL container'inda calistirir.
# Izole "YUK TESTI" sirketi olusturur (demo veriye dokunmaz).
#
# Kullanim: powershell -File scripts/generate-test-data.ps1

param(
    [string]$ContainerName = "raspel-postgres",
    [string]$DbUser = "postgres",
    [string]$DbName = "raspelerp"
)

$ErrorActionPreference = "Stop"

$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"
$password = $env:PGPASSWORD
if (-not $password -and (Test-Path $envFile)) {
    $satir = Get-Content $envFile | Where-Object { $_ -match "^POSTGRES_PASSWORD=" } | Select-Object -First 1
    if ($satir) { $password = ($satir -split "=", 2)[1].Trim() }
}
if (-not $password) { throw "POSTGRES_PASSWORD bulunamadi." }

$sql = Join-Path $PSScriptRoot "test-data-generator.sql"
Write-Host "Yuk testi verisi uretiliyor (birkac dakika surebilir)..." -ForegroundColor Cyan

$bas = Get-Date
Get-Content $sql -Raw | docker exec -i -e PGPASSWORD=$password $ContainerName psql -U $DbUser -d $DbName -v ON_ERROR_STOP=1
if ($LASTEXITCODE -ne 0) { throw "Veri uretimi basarisiz (exit $LASTEXITCODE)" }

$sure = [math]::Round(((Get-Date) - $bas).TotalSeconds, 1)
Write-Host "Veri uretimi tamamlandi ($sure sn)." -ForegroundColor Green
