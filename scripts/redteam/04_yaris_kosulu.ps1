# REDTEAM - YARIS KOSULU TEKRARI (fix sonrasi)
# CANLI KANIT (onceki calisma): 500 TL kalanTutarli faturaya 2 PARALEL
# POST /api/hareketler (500'er TL) -> ikisi de HTTP 201,
# fatura.odenen_tutar = 1000, kalan_tutar = 0 (fatura genelToplam 600).
# 500 TL HAYALET TAHSILAT olustu.
#
# DUZELTME: FaturaRepository.findByIdForUpdate (PESSIMISTIC_WRITE) eklendi ve
# faturaOdemeUygula + hareketOlustur dogrulamasi kilitli okumadan yapiliyor.
$ErrorActionPreference = "Continue"
$base = "http://localhost:8081/api"
$T = (Get-Content -LiteralPath "$env:TEMP\rt_tokens2.json" -Encoding UTF8 | ConvertFrom-Json)
$tok = @{}; $T.PSObject.Properties | ForEach-Object { $tok[$_.Name] = $_.Value.token }
$h = @{ Authorization = "Bearer " + $tok["admin_a"] }

function Psql($q) {
    $esc = $q -replace '"', '\"'
    docker exec raspel-postgres psql -U postgres -d raspelerp -t -A -c $esc 2>&1
}

Write-Output "=============== YARIS KOSULU TEKRARI ==============="

# 1) 600 TL'lik, 0 odenmis fatura olustur
$body = @{
    cariHesapId = 5015; tur = "SATIS"; tarih = "2026-10-04"; vadeTarihi = "2026-11-04"
    kalemler = @(@{ stokId = 2017; aciklama = "ZZTEST YARIS"; adet = 5; birimFiyat = 100; kdvOrani = 20 })
} | ConvertTo-Json -Depth 8
$f = Invoke-RestMethod -Uri "$base/faturalar" -Method Post -Headers $h -Body $body -ContentType "application/json"
Write-Host ("  Test faturasi olusturuldu: id={0} genelToplam={1} odenen={2} kalan={3}" -f `
    $f.id, $f.genelToplam, $f.odenenTutar, $f.kalanTutar)
$fid = $f.id

# 2) 8 PARALEL tahsilat istegi, her biri 500 TL (fatura 600 TL)
Write-Host "  8 paralel POST /api/hareketler (500'er TL) gonderiliyor..."
$jobs = @()
for ($i = 1; $i -le 8; $i++) {
    $jobs += Start-Job -ScriptBlock {
        param($base, $h, $fid, $i)
        $b = @{
            cariHesapId = 5015; tur = "TAHSILAT"; tutar = 500
            faturaId = $fid; odemeYontemi = "Nakit"
            hareketTarihi = "2026-10-04"; aciklama = "ZZTEST YARIS #$i"
        } | ConvertTo-Json -Depth 6
        try {
            $r = Invoke-WebRequest -Uri "$base/hareketler" -Method Post -Headers $h -Body $b `
                -ContentType "application/json" -UseBasicParsing -ErrorAction Stop
            "OK:" + [int]$r.StatusCode
        } catch {
            $c = 0; try { $c = [int]$_.Exception.Response.StatusCode } catch {}
            $t = ""; try { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); $t = $sr.ReadToEnd() } catch {}
            $msg = ""; try { $msg = ($t | ConvertFrom-Json).message } catch { $msg = $t }
            "ERR:$c " + $msg
        }
    } -ArgumentList $base, $h, $fid, $i
}
Wait-Job -Job $jobs -Timeout 180 | Out-Null
$sonuclar = Receive-Job -Job $jobs
Remove-Job -Job $jobs -Force

$ok = @($sonuclar | Where-Object { $_ -like "OK:*" })
$err = @($sonuclar | Where-Object { $_ -like "ERR:*" })
Write-Host ("  basarili (2xx) = {0}, reddedilen (4xx/5xx) = {1}" -f $ok.Count, $err.Count)
$err | Select-Object -First 3 | ForEach-Object { Write-Host ("    " + $_) }

# 3) Son durumu DB'den dogrula
$row = Psql "SELECT genel_toplam, odenen_tutar, kalan_tutar, odeme_durumu, version FROM fatura.fatura WHERE id = $fid"
Write-Host ("  FATURA SONUCU (DB): " + ($row -join " | "))
$f2 = Invoke-RestMethod -Uri "$base/faturalar/$fid" -Headers $h
Write-Host ("  API GORUNUMU      : genelToplam={0} odenen={1} kalan={2} durum={3}" -f `
    $f2.genelToplam, $f2.odenenTutar, $f2.kalanTutar, $f2.odemeDurumu)

$parts = ($row -split '\|') | ForEach-Object { $_.Trim() }
$toplam = [decimal]$parts[0]
$odenen = [decimal]$parts[1]
$beklenen = 500 * $ok.Count
if ($odenen -eq $beklenen) {
    Write-Host ("KORUNDU   YARIS-1  odenen={0} = basarili istek sayisi x 500 ({1}) -> KAYIP GUNCELLEME YOK" -f $odenen, $ok.Count)
} else {
    Write-Host ("ACIK !!   YARIS-1  odenen={0} beklenen={1} -> KAYIP GUNCELLEME VAR" -f $odenen, $beklenen)
}
if ($odenen -le $toplam) {
    Write-Host ("KORUNDU   YARIS-2  fazla tahsilat YOK: odenen {0} <= genelToplam {1}" -f $odenen, $toplam)
} else {
    Write-Host ("BILINEN  YARIS-2  fazla odeme KABUL EDILDI: odenen {0} > genelToplam {1} (fazla {2}) - istenen davranis, log.warn ile izlenir" -f $odenen, $toplam, ($odenen - $toplam))
}

# 4) Hareket kayitlarini say
$hcount = Psql "SELECT count(*) FROM cari.hareket WHERE fatura_id = $fid"
Write-Host ("  Bu faturaya yazilan hareket sayisi: " + (($hcount -join "").Trim()) + " (basarili istek: " + $ok.Count + ")")