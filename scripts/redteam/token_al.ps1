<#
    Red Team test ortami icin JWT alir.
    DIKKAT: Bu script YALNIZCA izole test tenant'ina (sirket 99) ve yerel demo
    verisine karsi kullanilir. Parola degerini kaynak koda YAZMAYIN:
      - varsayilan: RT_TEST_PASSWORD ortam degiskeni
      - yoksa parametre ile verilir
    Gercek ortam parolalari bu dosyada tutulmaz.
#>
param(
    [string]$Username = "zzadmin_b",
    [string]$SirketId  = "99",
    [string]$Pass     = $env:RT_TEST_PASSWORD
)

if ([string]::IsNullOrWhiteSpace($Pass)) {
    Write-Error @"
Parola belirtilmedi. Ortam degiskeni ile verin:
    `$env:RT_TEST_PASSWORD = '<test parolasi>'; .\token_al.ps1
veya parametre ile: .\token_al.ps1 -Pass '<test parolasi>'
"@
    exit 1
}

$ErrorActionPreference = "Stop"
$base = "http://localhost:8081/api"

function Giris($u, $s, $p) {
    $body = @{ username = $u; password = $p } | ConvertTo-Json
    $g = Invoke-RestMethod -Uri "$base/kullanicilar/giris" -Method Post -Body $body -ContentType "application/json"
    $b = @{ girisToken = $g.girisToken; sirketId = [int64]$s } | ConvertTo-Json
    try {
        $r = Invoke-RestMethod -Uri "$base/kullanicilar/giris-sirket" -Method Post -Body $b -ContentType "application/json"
        return @{ token = $r.token; kullanici = $u; sirket = $s }
    } catch {
        return @{ token = $g.token; kullanici = $u; sirket = $s }
    }
}

function Code($method, $uri, $token, $bodyObj) {
    $h = @{ Authorization = "Bearer $token" }
    try {
        if ($null -eq $bodyObj) {
            $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $h -UseBasicParsing -ErrorAction Stop
        } else {
            $j = $bodyObj | ConvertTo-Json -Depth 8
            $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $h -Body $j -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
        }
        return @{ code = [int]$r.StatusCode; body = $r.Content }
    } catch {
        $c = $null
        try { $c = [int]$_.Exception.Response.StatusCode } catch {}
        $txt = ""
        try {
            $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
            $txt = $sr.ReadToEnd()
        } catch {}
        return @{ code = $c; body = $txt }
    }
}

# Test kullanicilari (01_test_ortami_kur.sql ile olusturulur).
# Yalnizca test tenant'ina ait hesaplar kullanilir.
$tokens = @{}
$tokens["admin_a"]  = Giris "admin"      "4"  $Pass
$tokens["zzadmin_b"] = Giris "zzadmin_b" "99" $Pass
$tokens["driver_a"]  = Giris "zzdriver_a" "4" $Pass
$tokens["zzuser_b"]  = Giris "zzuser_b"  "99" $Pass

$tokens | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath "$env:TEMP\rt_tokens2.json" -Encoding UTF8
$tokens.GetEnumerator() | ForEach-Object {
    $ok = if ($_.Value.token) { "OK" } else { "TOKEN YOK" }
    "{0,-12} sirket={1,-4} {2}" -f $_.Key, $_.Value.sirket, $ok
}