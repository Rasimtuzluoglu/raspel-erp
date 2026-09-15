# Repository Scripts

## Database Maintenance Scripts

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
Saves to `Documents\RaspelERP Backups\raspelerp_DAILY_YYYYMMDD_HHmmss.sql.gz`. Keeps the last N daily backups (`-KeepDays 30` default; pass `-KeepDays 7` for 7).

> **Format:** `raspelerp_{TYPE}_{yyyyMMdd}_{HHmmss}.sql.gz` — this is the canonical format used
> consistently by BackupService (backend), `backup.ps1` and `restore.ps1`. Bulut (MinIO)
> kopyaları şifreleme açıksa aynı adın `.enc` sonekiyle AES-256-GCM olarak saklanır.

### Manual Restore
By path:
```powershell
.\scripts\restore.ps1 -BackupFile "C:\Users\rasim\Documents\RaspelERP Backups\raspelerp_DAILY_20260722_150000.sql.gz"
```
Or interactively (without arguments, lists available backups):
```powershell
.\scripts\restore.ps1
```

### Schedule Automatic Daily Backups
```powershell
.\scripts\schedule-backup.ps1
```
Creates a Windows Scheduled Task that runs backup.ps1 daily at 03:00. Retention is passed through: `schedule-backup.ps1 -KeepDays 7` → backup.ps1 temizliği 7 gün olur.

## Adminer (Web DB Admin)
Access at http://localhost:8082
- System: PostgreSQL
- Server: postgres
- Username: postgres
- Password: postgres
- Database: raspelerp

## i18n Integrity Check
Validates that every translation key used in `frontend/src` exists in both `tr.json` and `en.json`, that `{parametre}` placeholders match call sites, and that no garbage values slipped in.

```bash
node scripts/check-i18n.mjs      # repo root
npm run i18n:check               # from frontend/
```

Also runs as `i18n integrity check` step in CI. Exit code 1 on any problem. New i18n keys are auto-detected; if a key is unused it is allowed, but missing/parametrized/garbage keys fail the gate.
