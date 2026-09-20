# RasPel ERP - PITR Base Backup
# WAL arsivleme (PITR) icin periyodik base backup alir ve eski yedekleri temizler.
# Kullanim: powershell -File scripts/pitr-basebackup.ps1 [-KeepDays 14]
#
# Onkosul: docker-compose.yml'de postgres `archive_mode=on` ve /wal-archive mount'u aktif olmalidir.

param(
    [string]$ContainerName = "raspel-postgres",
    [string]$DbUser = "postgres",
    [int]$KeepDays = 14
)

$ErrorActionPreference = "Stop"

$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"
$password = $env:PGPASSWORD
if (-not $password -and (Test-Path $envFile)) {
    $satir = Get-Content $envFile | Where-Object { $_ -match "^POSTGRES_PASSWORD=" } | Select-Object -First 1
    if ($satir) { $password = ($satir -split "=", 2)[1].Trim() }
}
if (-not $password) { throw "POSTGRES_PASSWORD bulunamadi (.env veya PGPASSWORD ortam degiskeni)." }

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$hedef = "/wal-archive/base/$stamp"

Write-Host "RasPel ERP PITR base backup" -ForegroundColor Cyan
Write-Host "==========================="

docker exec -e PGPASSWORD=$password $ContainerName sh -c "mkdir -p /wal-archive/base"
docker exec -e PGPASSWORD=$password $ContainerName `
    pg_basebackup -U $DbUser -D $hedef -Ft -z -X fetch -c fast -P
if ($LASTEXITCODE -ne 0) { throw "pg_basebackup basarisiz (exit $LASTEXITCODE)" }

# Base backup + arsivlenmis WAL dosyalarindan KeepDays'ten eski olanlari temizle.
docker exec $ContainerName sh -c "find /wal-archive/base -mindepth 1 -maxdepth 1 -type d -mtime +$KeepDays -exec rm -rf {} +"
docker exec $ContainerName sh -c "find /wal-archive -maxdepth 1 -type f -name '0000*' -mtime +$KeepDays -delete"

$adet = (docker exec $ContainerName sh -c "ls -1 /wal-archive/base | wc -l").Trim()
Write-Host "Base backup olusturuldu: $hedef (mevcut base backup sayisi: $adet)" -ForegroundColor Green
Write-Host "Saklama: son $KeepDays gun. WAL dosyalari /wal-archive altinda arsivlenir." -ForegroundColor Cyan
