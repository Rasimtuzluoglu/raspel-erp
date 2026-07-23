# Database Maintenance Scripts

## Prerequisites
- Docker containers must be running (`docker compose up -d`)
- PostgreSQL container name: `raspel-postgres`
- PowerShell 5.1+

## Backup Location
Backups are stored **outside the project** in:
```
%USERPROFILE%\Documents\RaspelERP Backups\
```
This keeps the project folder clean.

## Usage

### Manual Backup
```powershell
.\scripts\backup.ps1
```
Saves to `Documents\RaspelERP Backups\raspelerp_YYYYMMDD_HHmmss.sql.zip`. Keeps last 7 backups.

### Manual Restore
By path:
```powershell
.\scripts\restore.ps1 -BackupFile "C:\Users\rasim\Documents\RaspelERP Backups\raspelerp_20260722_150000.sql.zip"
```
Or interactively (without arguments, lists available backups):
```powershell
.\scripts\restore.ps1
```

### Schedule Automatic Daily Backups
```powershell
.\scripts\schedule-backup.ps1
```
Creates a Windows Scheduled Task that runs backup.ps1 daily at 03:00.

## Adminer (Web DB Admin)
Access at http://localhost:8082
- System: PostgreSQL
- Server: postgres
- Username: postgres
- Password: postgres
- Database: raspelerp
