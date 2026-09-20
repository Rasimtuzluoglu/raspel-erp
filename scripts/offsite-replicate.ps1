# RasPel ERP - Offsite Yedek Replikasyonu
# Yerel yedekleri (logical dump + PITR base backup/WAL) offsite bir hedefe kopyalar.
# Desteklenen hedefler (oncelik sirasiyla, ortam degiskeniyle secilir):
#   1) OFFSITE_RCLONE_REMOTE  -> rclone remote (S3/B2/Drive vb.)  : rclone copy <src> <remote>
#   2) OFFSITE_SCP_TARGET     -> user@host:/path                   : scp -r
#   3) OFFSITE_DIR            -> yerel/NAS/external disk klasoru    : Copy-Item
#
# Kullanim:
#   $env:OFFSITE_DIR="E:\RasPelBackups"; powershell -File scripts/offsite-replicate.ps1
#   $env:OFFSITE_RCLONE_REMOTE="b2:raspel-backups"; powershell -File scripts/offsite-replicate.ps1

param(
    [string]$KaynakDizin = ""
)

$ErrorActionPreference = "Stop"

if (-not $KaynakDizin) {
    $KaynakDizin = Join-Path (Split-Path $PSScriptRoot -Parent) "backups"
}

if (-not (Test-Path $KaynakDizin)) {
    Write-Host "Kaynak dizin yok: $KaynakDizin (WAL arsivi henuz olusmamis olabilir)." -ForegroundColor Yellow
    exit 0
}

Write-Host "RasPel ERP Offsite Replikasyon" -ForegroundColor Cyan
Write-Host "Kaynak: $KaynakDizin"

if ($env:OFFSITE_RCLONE_REMOTE) {
    if (-not (Get-Command rclone -ErrorAction SilentlyContinue)) {
        throw "OFFSITE_RCLONE_REMOTE tanimli ama rclone kurulu degil."
    }
    Write-Host "Hedef (rclone): $env:OFFSITE_RCLONE_REMOTE" -ForegroundColor Yellow
    rclone copy "$KaynakDizin" "$env:OFFSITE_RCLONE_REMOTE" --transfers 4 --checkers 8 --stats-one-line -v
    if ($LASTEXITCODE -ne 0) { throw "rclone copy basarisiz (exit $LASTEXITCODE)" }
    Write-Host "Offsite kopya tamamlandi (rclone)." -ForegroundColor Green
    exit 0
}

if ($env:OFFSITE_SCP_TARGET) {
    Write-Host "Hedef (scp): $env:OFFSITE_SCP_TARGET" -ForegroundColor Yellow
    scp -r "$KaynakDizin"/* "$env:OFFSITE_SCP_TARGET"
    if ($LASTEXITCODE -ne 0) { throw "scp basarisiz (exit $LASTEXITCODE)" }
    Write-Host "Offsite kopya tamamlandi (scp)." -ForegroundColor Green
    exit 0
}

if ($env:OFFSITE_DIR) {
    if (-not (Test-Path $env:OFFSITE_DIR)) { New-Item -ItemType Directory -Path $env:OFFSITE_DIR -Force | Out-Null }
    Write-Host "Hedef (klasor): $env:OFFSITE_DIR" -ForegroundColor Yellow
    Copy-Item -Path (Join-Path $KaynakDizin '*') -Destination $env:OFFSITE_DIR -Recurse -Force
    Write-Host "Offsite kopya tamamlandi (klasor)." -ForegroundColor Green
    exit 0
}

Write-Host "Offsite hedef tanimli degil. OFFSITE_RCLONE_REMOTE / OFFSITE_SCP_TARGET / OFFSITE_DIR'den birini ayarlayin." -ForegroundColor Yellow
exit 0
