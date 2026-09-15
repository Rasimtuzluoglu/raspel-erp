param(
    [string]$OutputDir = "$env:USERPROFILE\Documents\RaspelERP Backups",
    [string]$ContainerName = "raspel-postgres",
    [string]$DbName = "raspelerp",
    [string]$DbUser = "postgres",
    [int]$KeepDays = 30
)

# Kanonik format (BackupService ile aynı): raspelerp_{TYPE}_{yyyyMMdd}_{HHmmss}.sql.gz
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$type = "DAILY"
$backupFile = Join-Path $OutputDir "raspelerp_${type}_$stamp.sql.gz"

if (!(Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null }

Write-Host "RasPel ERP Veritabani Yedekleme" -ForegroundColor Cyan
Write-Host "================================"

$envFile = Join-Path (Split-Path $PSScriptRoot -Parent) ".env"
$password = $env:PGPASSWORD
if (!$password -and (Test-Path $envFile)) {
    $envContent = Get-Content $envFile | Where-Object { $_ -match "POSTGRES_PASSWORD=" }
    if ($envContent) { $password = ($envContent -split "=", 2)[1].Trim() }
}
if (!$password) { $password = Read-Host "PostgreSQL sifresi" -AsSecureString; $password = [System.Runtime.InteropServices.Marshal]::PtrToStringAuto([System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($password)) }

Write-Host "Yedekleniyor: $DbName"
$env:PGPASSWORD = $password

try {
    # pg_dump | gzip tek seferde (BackupService'in yaptigi gibi).
    docker exec $ContainerName sh -c "PGPASSWORD='$password' pg_dump -U $DbUser -d $DbName --no-owner --no-acl --clean --if-exists | gzip > /tmp/$stamp.sql.gz"
    if ($LASTEXITCODE -ne 0) { throw "pg_dump hatasi (exit $LASTEXITCODE)" }
    docker cp "$ContainerName`:/tmp/$stamp.sql.gz" $backupFile
    docker exec $ContainerName rm -f "/tmp/$stamp.sql.gz"

    $size = (Get-Item $backupFile).Length / 1KB
    Write-Host "BASARILI: $backupFile ($([math]::Round($size, 1)) KB)" -ForegroundColor Green

    # KeepDays temizligi: bu gun sayisindan eski DAILY yedeklerini sil
    if ($KeepDays -gt 0) {
        $cutoff = (Get-Date).AddDays(-$KeepDays)
        $eski = Get-ChildItem $OutputDir -Filter "raspelerp_${type}_*.sql.gz" -ErrorAction SilentlyContinue |
            Where-Object { $_.LastWriteTime -lt $cutoff }
        foreach ($f in $eski) { Remove-Item $f.FullName -Force; Write-Host "Silindi (eski): $($f.Name)" -ForegroundColor DarkYellow }
        Write-Host "Saklama: son $KeepDays gunluk yedek tutuldu." -ForegroundColor Cyan
    }
} catch {
    Write-Host "HATA: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    docker exec $ContainerName rm -f "/tmp/$stamp.sql.gz" 2>&1 | Out-Null
}