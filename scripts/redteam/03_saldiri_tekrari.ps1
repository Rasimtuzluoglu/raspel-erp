# REDTEAM - ADIM 3.4: SALDIRI TEKRARI (RE-ATTACK) v2
$ErrorActionPreference = "Continue"
$base = "http://localhost:8081/api"
$T = (Get-Content -LiteralPath "$env:TEMP\rt_tokens2.json" -Encoding UTF8 | ConvertFrom-Json)
$tok = @{}
$T.PSObject.Properties | ForEach-Object { $tok[$_.Name] = $_.Value.token }
$script:suken = 0; $script:basari = 0

function Atk($id, $desc, $beklenen, $method, $uri, $tokenKey, $body) {
    $t = $tok[$tokenKey]
    $h = @{ Authorization = "Bearer $t" }
    $code = 0; $txt = ""
    try {
        if ($null -eq $body) {
            $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $h -UseBasicParsing -ErrorAction Stop
            $code = [int]$r.StatusCode; $txt = $r.Content
        } else {
            $j = $body | ConvertTo-Json -Depth 10
            $r = Invoke-WebRequest -Uri $uri -Method $method -Headers $h -Body $j -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
            $code = [int]$r.StatusCode; $txt = $r.Content
        }
    } catch {
        try { $code = [int]$_.Exception.Response.StatusCode } catch {}
        try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $txt = $sr.ReadToEnd() } catch {}
    }
    $ok = $false
    if ($beklenen -eq "4xx") { $ok = ($code -ge 400 -and $code -lt 500) }
    elseif ($beklenen -eq "2xx") { $ok = ($code -ge 200 -and $code -lt 300) }
    elseif ($beklenen -eq "not2xx") { $ok = ($code -lt 200 -or $code -ge 300) }
    if ($ok) { $script:basari++ ; $v = "KORUNDU " } else { $script:suken++ ; $v = "ACIK !!" }
    $msg = ""
    try {
        $o = $txt | ConvertFrom-Json
        if ($o.message) { $msg = $o.message }
        elseif ($o.error) { $msg = $o.error }
        elseif ($o.errors) { $msg = ($o.errors.PSObject.Properties | ForEach-Object { $_.Name + "=" + $_.Value }) -join "; " }
    } catch { $msg = $txt }
    $msg = $msg -replace "`r?`n", " "
    if ($msg.Length -gt 95) { $msg = $msg.Substring(0, 95) + "..." }
    Write-Host ("{0}  {1,-5} HTTP {2,-4} bek={3,-6} {4}" -f $v, $id, $code, $beklenen, $msg)
    return @{ code = $code; body = $txt; ok = $ok }
}

Write-Output "=============== REDTEAM SALDIRI TEKRARI v2 (fix sonrasi) ==============="
Write-Output ""

Write-Output "-- C1: negatif iskonto ile kasa nakit yaratma"
Atk "C1" "iskonto=-500" "4xx" "POST" "$base/faturalar" "admin_a" @{
    cariHesapId = 5015; tarih = "2026-10-04"; vadeTarihi = "2026-11-04"
    kalemler = @(@{ stokId = 2017; aciklama = "ZZTEST C1"; adet = 1; birimFiyat = 100; kdvOrani = 0; iskontoOrani = -500 })
    odemeDurumu = "ODENDI"; odenenTutar = 600 } | Out-Null
Atk "C1b" "iskonto=-50/-100/-500" "4xx" "POST" "$base/faturalar" "admin_a" @{
    cariHesapId = 5015; tarih = "2026-10-04"; vadeTarihi = "2026-11-04"
    kalemler = @(@{ stokId = 2017; aciklama = "ZZTEST C1"; adet = 10; birimFiyat = 100; kdvOrani = 20; iskontoOrani = -100 })
    odemeDurumu = "ODENDI" } | Out-Null

Write-Output "-- C3: negatif stok hareketi ile stok cogaltma"
Atk "C3" "CIKIS -5000" "4xx" "POST" "$base/stoklar/2017/hareketler" "admin_a" @{
    tur = "CIKIS"; miktar = -5000; hareketTarihi = "2026-10-04"; aciklama = "ZZTEST C3" } | Out-Null
Atk "C3b" "GIRIS -5000" "4xx" "POST" "$base/stoklar/2017/hareketler" "admin_a" @{
    tur = "GIRIS"; miktar = -5000; hareketTarihi = "2026-10-04"; aciklama = "ZZTEST C3b" } | Out-Null
Atk "C3c" "CIKIS 0" "4xx" "POST" "$base/stoklar/2017/hareketler" "admin_a" @{
    tur = "CIKIS"; miktar = 0; hareketTarihi = "2026-10-04"; aciklama = "ZZTEST C3c" } | Out-Null

Write-Output "-- C4: negatif recete kalemi ile hammadde stogu artirma"
Atk "C4" "miktar=-10" "4xx" "POST" "$base/uretim/receteler" "admin_a" @{
    urunStokId = 2017; ad = "ZZTEST C4 recete"; birim = "ADET"
    kalemler = @(@{ hammaddeId = 2017; miktar = -10 }) } | Out-Null

Write-Output "-- C5: irsaliye durum mass-assignment -> bedava stok"
$c5 = Atk "C5a" "irsaliye TASLAK" "2xx" "POST" "$base/irsaliyeler" "admin_a" @{
    irsaliyeNo = "ZZ-C5-$(Get-Random)"; tarih = "2026-10-04"; tip = "SATIS"; cariHesapId = 5015
    kalemler = @(@{ stokId = 2017; aciklama = "ZZTEST C5"; miktar = 50; birimFiyat = 10 }) }
if ($c5.code -eq 201) {
    $yeniId = ($c5.body | ConvertFrom-Json).id
    Atk "C5b" "PUT durum=KESILDI" "4xx" "PUT" "$base/irsaliyeler/$yeniId" "admin_a" @{ durum = "KESILDI" } | Out-Null
    Atk "C5c" "PUT durum=IPTAL" "4xx" "PUT" "$base/irsaliyeler/$yeniId" "admin_a" @{ durum = "IPTAL" } | Out-Null
} else { "  ACIK !!  C5a irsaliye olusturulamadi (HTTP $($c5.code))"; $script:suken++ }

Write-Output "-- C6: iade durum gecisi -> cift stok girisi"
$h = @{ Authorization = "Bearer " + $tok["admin_a"] }
# C9 icin saklanan token: bu noktada henuz sifresi degistirilmemis olmali.
$hEski = @{ Authorization = "Bearer " + $tok["zzuser_b"] }
# C6a: stok 2017 icin satis faturasi olustur (iade bu faturaya baglanmali)
$c6f = Atk "C6-pre" "satis faturasi (stok 2017)" "2xx" "POST" "$base/faturalar" "admin_a" @{
    cariHesapId = 5015; tur = "SATIS"; tarih = "2026-10-04"; vadeTarihi = "2026-11-04"
    kalemler = @(@{ stokId = 2017; aciklama = "ZZTEST C6"; adet = 10; birimFiyat = 10; kdvOrani = 20 }) }
if ($c6f.code -eq 201) {
    $fid = ($c6f.body | ConvertFrom-Json).id
    $c6 = Atk "C6a" "iade TASLAK" "2xx" "POST" "$base/iadeler" "admin_a" @{
        faturaId = $fid; tur = "SATIS"; tarih = "2026-10-04"; tutar = 60
        kalemler = @(@{ stokId = 2017; miktar = 5; birimFiyat = 10; kdvOrani = 20 }) }
    if ($c6.code -eq 201) {
        $iid = ($c6.body | ConvertFrom-Json).id
        Atk "C6b" "TAMAMLANDI" "2xx" "PUT" "$base/iadeler/$iid/durum" "admin_a" @{ durum = "TAMAMLANDI" } | Out-Null
        Atk "C6c" "TAMAMLANDI->TASLAK" "4xx" "PUT" "$base/iadeler/$iid/durum" "admin_a" @{ durum = "TASLAK" } | Out-Null
        Atk "C6d" "TAMAMLANDI (tekrar)" "4xx" "PUT" "$base/iadeler/$iid/durum" "admin_a" @{ durum = "TAMAMLANDI" } | Out-Null
        Atk "C6e" "TAMAMLANDI->IPTAL" "2xx" "PUT" "$base/iadeler/$iid/durum" "admin_a" @{ durum = "IPTAL" } | Out-Null
        Atk "C6f" "IPTAL->TAMAMLANDI" "4xx" "PUT" "$base/iadeler/$iid/durum" "admin_a" @{ durum = "TAMAMLANDI" } | Out-Null
    } else { "  ACLIK !! C6a iade olusturulamadi (HTTP $($c6.code))"; $script:suken++ }
} else { "  ACLIK !! C6-pre fatura olusturulamadi (HTTP $($c6f.code))"; $script:suken++ }

Write-Output "-- H-3: capraz tenant veri aktarimi"
Atk "H-3" "Tenant B USER -> Tenant A" "4xx" "GET" "$base/veri-aktarim/onizleme?kaynakSirketId=4&hedefSirketId=99" "zzuser_b" $null | Out-Null
Atk "H-3b" "Tenant A ADMIN -> B (platform muaf)" "2xx" "GET" "$base/veri-aktarim/onizleme?kaynakSirketId=4&hedefSirketId=99" "admin_a" $null | Out-Null

Write-Output ""
Write-Output "-- C9: sifre degisimi token iptali (EN SONDA calisir: token'lari gecersiz kilar)"
# CANLI KANIT (onceki calisma): PUT /api/kullanicilar/{id} ile sifre degistirildi
# -> HTTP 200, ancak token_version 0'da KALDI ve sifre degisiminden ONCE alinan
# eski token hala HTTP 200 donuyordu.
# Burada: sifre degistirilir, sonra ayni eski token ile tekrar cagrilir -> 401.
Write-Output "   (once sifre degisimi YAPILMADAN eski tokenin gecerli oldugunu dogrula)"
$kode0 = 0
try {
    $r = Invoke-WebRequest -Uri "$base/kullanicilar/ben" -Headers $hEski -UseBasicParsing -ErrorAction Stop
    $kode0 = [int]$r.StatusCode
} catch { $kode0 = 0; try { $kode0 = [int]$_.Exception.Response.StatusCode } catch {} }
Write-Host ("   eski token, sifre degisimi ONCESI: HTTP {0} (200 beklenir)" -f $kode0)

# C9 icin kullanilan GECICI parola (test tenant'ina ait). Kaynak koda gercek
# parola YAZILMAZ; gecici deger uretilir.
$yeniSifre = "Rt" + (Get-Random -Minimum 100000 -Maximum 999999) + "aA1!"
$c9 = Atk "C9a" "kullanici 9902 sifre degistir" "2xx" "PUT" "$base/kullanicilar/9902" "zzadmin_b" @{
    username = "zzuser_b"; displayName = "ZZTEST User B"; password = $yeniSifre } | Out-Null

# AYNI eski token ile tekrar cagri: 401 beklenir
$kode2 = 0
try {
    $r = Invoke-WebRequest -Uri "$base/kullanicilar/ben" -Headers $hEski -UseBasicParsing -ErrorAction Stop
    $kode2 = [int]$r.StatusCode
} catch { $kode2 = 0; try { $kode2 = [int]$_.Exception.Response.StatusCode } catch {} }
if ($kode2 -eq 401) { $script:basari++; Write-Host "KORUNDU   C9b   eski token 401 ile reddedildi (oturumlar iptal)" }
else { $script:suken++; Write-Host ("ACIK !!   C9b   eski token hala HTTP {0}" -f $kode2) }

Write-Output "-- C10: DRIVER (sofor) finansal yazma yetkisi"
Atk "C10" "DRIVER tahsilat" "4xx" "POST" "$base/tahsilat" "driver_a" @{
    cariId = 5015; tutar = 50000; odemeYontemi = "Nakit"; aciklama = "ZZTEST C10" } | Out-Null

Write-Output "-- H-1/H-2: platform geneli yedek erisimi"
Atk "H-1" "Tenant B admin yedek listesi" "4xx" "GET" "$base/backups" "zzadmin_b" $null | Out-Null
Atk "H-2a" "Tenant B admin yedek indir" "4xx" "GET" "$base/backups/download/raspelerp_DAILY_20261004_025959.sql.gz" "zzadmin_b" $null | Out-Null
Atk "H-2b" "Tenant B admin manuel yedek" "4xx" "POST" "$base/backups/manual?type=DAILY" "zzadmin_b" $null | Out-Null
Atk "H-2c" "presigned backups URL" "4xx" "GET" "$base/dosya/imzali-url?klasor=backups&dosya=raspelerp_DAILY_20261004_025959.sql.gz" "zzadmin_b" $null | Out-Null
Write-Output "  (Tenant A admin icin 2xx BEKLENIR - platform yoneticisi)"
Atk "H-1-ok" "Tenant A admin yedek listesi" "2xx" "GET" "$base/backups" "admin_a" $null | Out-Null
Atk "H-2-ok" "Tenant A admin presigned" "2xx" "GET" "$base/dosya/imzali-url?klasor=backups&dosya=raspelerp_DAILY_20261004_025959.sql.gz" "admin_a" $null | Out-Null

Write-Output "-- H-3: capraz tenant veri aktarimi"
Atk "H-3" "Tenant B USER -> Tenant A" "4xx" "GET" "$base/veri-aktarim/onizleme?kaynakSirketId=4&hedefSirketId=99" "zzuser_b" $null | Out-Null
Atk "H-3b" "Tenant A ADMIN -> B (platform muaf)" "2xx" "GET" "$base/veri-aktarim/onizleme?kaynakSirketId=4&hedefSirketId=99" "admin_a" $null | Out-Null

Write-Output ""
Write-Output "SONUC: basarili=$($script:basari)  acik=$($script:suken)"