param(
    [string]$ContainerName = "raspel-postgres",
    [string]$DbName = "raspelerp",
    [string]$DbUser = "postgres",
    [Parameter(Mandatory = $true)]
    [string]$BackupFile
)

if ($BackupFile -eq "" -or -not (Test-Path -LiteralPath $BackupFile)) {
    # List available backups
    $defaultDir = "$env:USERPROFILE\Documents\RaspelERP Backups"
    if (Test-Path -LiteralPath $defaultDir) {
        $files = Get-ChildItem -Path $defaultDir -Filter "raspelerp_*" | Sort-Object Name -Descending
        if ($files.Count -eq 0) {
            Write-Host "No backups found in $defaultDir" -ForegroundColor Red
            exit 1
        }
        Write-Host "Available backups:" -ForegroundColor Cyan
        for ($i = 0; $i -lt $files.Count; $i++) {
            Write-Host "  [$i] $($files[$i].Name) ($(Get-Date $files[$i].LastWriteTime -Format 'yyyy-MM-dd HH:mm'))"
        }
        $selection = Read-Host "Select backup number (0-$($files.Count-1))"
        $BackupFile = $files[[int]$selection].FullName
    } else {
        Write-Host "Backup file not found: $BackupFile" -ForegroundColor Red
        exit 1
    }
}

if (-not (Test-Path -LiteralPath $BackupFile)) {
    Write-Host "Backup file not found: $BackupFile" -ForegroundColor Red
    exit 1
}

Write-Host "WARNING: This will DROP and recreate database '$DbName'!" -ForegroundColor Red -BackgroundColor Black
$confirm = Read-Host "Type 'RESTORE' to confirm"
if ($confirm -ne "RESTORE") {
    Write-Host "Restore cancelled." -ForegroundColor Yellow
    exit 0
}

Write-Host "Restoring '$DbName' from '$BackupFile'..." -ForegroundColor Cyan

# Terminate existing connections and drop/recreate
docker exec $ContainerName psql -U $DbUser -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname='$DbName' AND pid <> pg_backend_pid();" 2>&1 | Out-Null
docker exec $ContainerName psql -U $DbUser -c "DROP DATABASE IF EXISTS $DbName;" 2>&1 | Out-Null
docker exec $ContainerName psql -U $DbUser -c "CREATE DATABASE $DbName;" 2>&1 | Out-Null

# Handle compressed files
$inputFile = $BackupFile
if ($BackupFile -match '\.gz$') {
    if (Get-Command gzip -ErrorAction SilentlyContinue) {
        $inputFile = [System.IO.Path]::GetTempFileName()
        gzip -d -c $BackupFile > $inputFile
    } else {
        Write-Host "gzip not found, cannot decompress." -ForegroundColor Red
        exit 1
    }
} elseif ($BackupFile -match '\.zip$') {
    $tempDir = [System.IO.Path]::GetTempPath()
    Expand-Archive -Path $BackupFile -DestinationPath $tempDir -Force
    $inputFile = Get-ChildItem -Path $tempDir -Filter "*.sql" | Select-Object -First 1 -ExpandProperty FullName
}

# Restore
Get-Content $inputFile | docker exec -i $ContainerName psql -U $DbUser $DbName 2>&1

if ($LASTEXITCODE -eq 0) {
    Write-Host "Restore completed successfully." -ForegroundColor Green
} else {
    Write-Host "Restore FAILED with exit code $LASTEXITCODE" -ForegroundColor Red
}

# Clean temp files
if ($inputFile -ne $BackupFile -and (Test-Path -LiteralPath $inputFile)) {
    Remove-Item -Path $inputFile -Force -ErrorAction SilentlyContinue
}