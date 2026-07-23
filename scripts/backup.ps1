param(
    [string]$ContainerName = "raspel-postgres",
    [string]$DbName = "raspelerp",
    [string]$DbUser = "postgres",
    [string]$BackupDir = "$env:USERPROFILE\Documents\RaspelERP Backups"
)

if (-not (Test-Path -LiteralPath $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir -Force | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$filename = "raspelerp_$timestamp.sql"
$filepath = Join-Path $BackupDir $filename

Write-Host "Backing up '$DbName' from container '$ContainerName'..." -ForegroundColor Cyan

$result = docker exec $ContainerName pg_dump -U $DbUser $DbName 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "Backup FAILED: $result" -ForegroundColor Red
    exit 1
}

$result | Out-File -FilePath $filepath -Encoding utf8

# Compress with gzip
$gzipped = "$filepath.gz"
if (Get-Command gzip -ErrorAction SilentlyContinue) {
    gzip -f $filepath
    Write-Host "Backup saved: $gzipped" -ForegroundColor Green
} else {
    # PowerShell-native compression
    $compressed = "$filepath.zip"
    Compress-Archive -Path $filepath -DestinationPath $compressed -Force
    Remove-Item -Path $filepath -Force
    Write-Host "Backup saved: $compressed" -ForegroundColor Green
}

# Keep last 7 backups, remove older ones
$backups = Get-ChildItem -Path $BackupDir -Filter "raspelerp_*" | Sort-Object Name -Descending
if ($backups.Count -gt 7) {
    $backups | Select-Object -Skip 7 | Remove-Item -Force
    Write-Host "Old backups cleaned. Keeping last 7." -ForegroundColor Yellow
}

Write-Host "Backup completed successfully." -ForegroundColor Green