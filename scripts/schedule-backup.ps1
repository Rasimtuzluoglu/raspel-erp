param(
    [string]$TaskName = "RaspelERP_DailyBackup",
    [string]$ScriptPath = (Join-Path $PSScriptRoot "backup.ps1"),
    [string]$Time = "03:00",
    [int]$KeepDays = 7
)

$action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument "-NoProfile -ExecutionPolicy Bypass -File `"$ScriptPath`""
$trigger = New-ScheduledTaskTrigger -Daily -At $Time
$settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -StartWhenAvailable

Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger -Settings $settings -Force

Write-Host "Scheduled task '$TaskName' created." -ForegroundColor Green
Write-Host "Runs daily at $Time: powershell.exe -File `"$ScriptPath`"" -ForegroundColor Cyan
Write-Host "To view: Get-ScheduledTask -TaskName '$TaskName'" -ForegroundColor Gray
Write-Host "To remove: Unregister-ScheduledTask -TaskName '$TaskName' -Confirm:`$false" -ForegroundColor Gray