param(
    [string]$OutputDir = ".\backups",
    [string]$ContainerName = "raspel-postgres",
    [string]$DbName = "raspelerp",
    [string]$DbUser = "postgres"
)

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupFile = Join-Path $OutputDir "raspelerp-$timestamp.backup"
$tempFile = Join-Path $env:TEMP "raspelerp-$timestamp.sql"

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
    docker exec $ContainerName pg_dump -U $DbUser -d $DbName -f "/tmp/$timestamp.sql"
    docker cp "$ContainerName`:/tmp/$timestamp.sql" $tempFile
    docker exec $ContainerName rm -f "/tmp/$timestamp.sql"
    
    Compress-Archive -Path $tempFile -DestinationPath $backupFile -Force
    Remove-Item $tempFile -Force
    
    $size = (Get-Item $backupFile).Length / 1KB
    Write-Host "BASARILI: $backupFile ($([math]::Round($size, 1)) KB)" -ForegroundColor Green
} catch {
    Write-Host "HATA: $($_.Exception.Message)" -ForegroundColor Red
} finally {
    Remove-Item $tempFile -ErrorAction SilentlyContinue
    if (Test-Path $tempFile) { Remove-Item $tempFile -Force -ErrorAction SilentlyContinue }
}
